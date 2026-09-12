"""Öffentliches Statistik-Dashboard (stats.iceinfo.de).

Liest die von der App gesammelten Fahrten READ-ONLY aus derselben SQLite-Datei
wie die API und rendert eine selbst-enthaltene HTML-Übersicht (keine externen
Assets, kein JS-Framework). Läuft als eigener Container, getrennt von der
schreibenden API.
"""

import html
import json
import os
import re
import sqlite3
import urllib.request
from datetime import datetime
from io import BytesIO

from fastapi import FastAPI, HTTPException, Response
from fastapi.responses import FileResponse, HTMLResponse
from PIL import Image, ImageDraw, ImageFont

DB_PATH = os.environ.get("DB_PATH", "/data/journeys.db")
# Öffentliche Basis-URL (für Open-Graph og:url der Einzelfahrt-Links)
PUBLIC_BASE = os.environ.get("PUBLIC_BASE", "https://stats.iceinfo.de")
# Self-hosted App-Fonts (siehe server/fonts/)
FONT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts")
_FONTS = {"inter.ttf", "spacegrotesk.ttf"}

# Rechtliche Pflichtlinks (Impressum/Datenschutz liegen auf iceinfo.de)
LEGAL_LINKS = (
    "<a href='https://iceinfo.de/impressum'>Impressum</a> · "
    "<a href='https://iceinfo.de/datenschutz'>Datenschutz</a>"
)

# API-Statusanzeige: intern im Docker-Netz erreichbar (kein CORS/Cloudflare)
API_HEALTH_URL = os.environ.get("API_HEALTH_URL", "http://iceinfo-api:8000/v1/health")

API_STATUS_HTML = (
    "<div class='api-status online' id='apiStatus'>"
    "<span class='dot'></span><span class='api-label'>API online</span></div>"
)

# Fragt /api-status ab und färbt den Punkt grün (online) bzw. rot (offline)
API_STATUS_SCRIPT = (
    "<script>(function(){var e=document.getElementById('apiStatus');if(!e)return;"
    "var l=e.querySelector('.api-label');"
    "function c(){fetch('/api-status',{cache:'no-store'}).then(function(r){return r.json();})"
    ".then(function(d){var ok=!!d.ok;e.classList.toggle('online',ok);"
    "e.classList.toggle('offline',!ok);if(l)l.textContent=ok?'API online':'API offline';})"
    ".catch(function(){e.classList.remove('online');e.classList.add('offline');"
    "if(l)l.textContent='API offline';});}c();setInterval(c,30000);})();</script>"
)

# Baureihen-Codes → Bezeichnung, gespiegelt aus util/IceUtils.kt (SERIES_MAP),
# damit das Dashboard dieselben Namen zeigt wie die App.
SERIES_NAMES = {
    "401": "ICE 1", "402": "ICE 2", "403": "ICE 3", "406": "ICE 3M",
    "407": "ICE 3 Velaro D", "408": "ICE 3neo", "411": "ICE T",
    "412": "ICE 4", "415": "ICE T (5-teilig)", "605": "ICE TD",
}

app = FastAPI(title="ICE Info Stats", docs_url=None, redoc_url=None)


def _connect() -> sqlite3.Connection:
    # query_only=ON macht die Verbindung schreibgeschützt auf Engine-Ebene,
    # bleibt aber WAL-kompatibel (eine reine mode=ro-Verbindung kann eine von
    # der API im WAL-Modus geschriebene DB aus einem zweiten Prozess nicht lesen).
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA query_only = ON")
    conn.row_factory = sqlite3.Row
    return conn


def _de(n) -> str:
    """Ganzzahl mit deutschem Tausenderpunkt."""
    return f"{int(round(n)):,}".replace(",", ".")


MONTHS_DE = ["Januar", "Februar", "März", "April", "Mai", "Juni",
             "Juli", "August", "September", "Oktober", "November", "Dezember"]
MONTHS_SHORT_DE = ["Jan", "Feb", "Mär", "Apr", "Mai", "Jun",
                   "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]
WEEKDAYS_DE = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]  # Index = strftime('%w')


def _period_where(period: str):
    """SQL-WHERE, Label und normalisierter Wert aus dem ?period=-Filter.
    "" = Gesamt · "YYYY" = Jahr · "YYYY-MM" = Monat. Ungültiges fällt auf Gesamt zurück.
    Werte sind regex-validiert (nur Ziffern) und daher sicher zu interpolieren."""
    period = (period or "").strip()
    if re.fullmatch(r"\d{4}", period):
        return f" WHERE substr(date, 7, 4) = '{period}'", period, period
    m = re.fullmatch(r"(\d{4})-(\d{2})", period)
    if m and 1 <= int(m.group(2)) <= 12:
        y, mo = m.group(1), m.group(2)
        return (f" WHERE substr(date, 7, 4) = '{y}' AND substr(date, 4, 2) = '{mo}'",
                f"{MONTHS_DE[int(mo) - 1]} {y}", f"{y}-{mo}")
    return "", "", ""


def load_stats(period: str = "") -> dict:
    try:
        conn = _connect()
        with conn:
            return _load(conn, period)
    except sqlite3.OperationalError:
        # DB-Datei oder Tabelle existiert noch nicht (frischer Server)
        _, label, norm = _period_where(period)
        return {"total": 0, "period": norm, "period_label": label, "years": [], "months": []}


def _load(conn: sqlite3.Connection, period: str = "") -> dict:
    where, label, norm = _period_where(period)
    active_year = norm[:4] if norm else ""

    # Filterleisten-Daten: verfügbare Jahre (ganze Tabelle) + Monate des aktiven Jahres
    years = [r[0] for r in conn.execute(
        "SELECT DISTINCT substr(date, 7, 4) AS y FROM journeys "
        "WHERE length(date) = 10 ORDER BY y DESC")]
    months = []
    if active_year:
        months = [r[0] for r in conn.execute(
            "SELECT DISTINCT substr(date, 4, 2) AS m FROM journeys "
            "WHERE substr(date, 7, 4) = ? AND length(date) = 10 ORDER BY m",
            (active_year,))]
    meta = {"period": norm, "period_label": label, "years": years, "months": months}

    total = conn.execute(f"SELECT COUNT(*) FROM journeys{where}").fetchone()[0]
    if not total:
        return {"total": 0, **meta}

    # Alle Aggregate laufen über die gefilterte Teilmenge (Subquery).
    src = f"(SELECT * FROM journeys{where})"

    def row(q):
        return conn.execute(q).fetchone()

    agg = row(f"""
        SELECT
            COUNT(*)                       AS total,
            COUNT(DISTINCT install_id)     AS contributors,
            COUNT(DISTINCT train_type || train_number) AS distinct_trains,
            COUNT(DISTINCT origin || '→' || destination) AS distinct_routes,
            COALESCE(AVG(delay_minutes), 0) AS avg_delay,
            COALESCE(AVG(CASE WHEN delay_minutes < 6 THEN 1.0 ELSE 0.0 END) * 100, 0) AS punctuality,
            COALESCE(AVG(CASE WHEN delay_minutes >= 15 THEN 1.0 ELSE 0.0 END) * 100, 0) AS share_15,
            COALESCE(AVG(CASE WHEN delay_minutes >= 60 THEN 1.0 ELSE 0.0 END) * 100, 0) AS share_60
        FROM {src}
        """)

    # Median-Verspätung — robuster gegen Ausreißer als der Mittelwert
    median_delay = conn.execute(
        f"SELECT delay_minutes FROM {src} ORDER BY delay_minutes "
        f"LIMIT 1 OFFSET {(total - 1) // 2}"
    ).fetchone()[0]

    # Ø Verspätung am Endbahnhof, nur tatsächlich erreichte (keine Prognosen) —
    # die faire, zwischen Nutzern vergleichbare Kennzahl (vgl. Ausstieg vs. Ende).
    final_avg = row(
        f"SELECT AVG(final_delay_minutes) AS a, COUNT(*) AS n FROM {src} "
        "WHERE final_delay_is_prognosis = 0 AND final_station <> ''"
    )

    worst_delay = row(
        f"SELECT train_type, train_number, origin, destination, date, delay_minutes "
        f"FROM {src} ORDER BY delay_minutes DESC LIMIT 1"
    )

    worst_trains = conn.execute(
        f"SELECT train_type || ' ' || train_number AS train, "
        f"       COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} GROUP BY train HAVING n >= 3 "
        f"ORDER BY avg_delay DESC, n DESC LIMIT 5"
    ).fetchall()
    series_delay = conn.execute(
        f"SELECT series, COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} WHERE series <> '' GROUP BY series HAVING n >= 3 "
        f"ORDER BY avg_delay ASC, n DESC LIMIT 5"
    ).fetchall()
    worst_routes = conn.execute(
        f"SELECT origin || ' → ' || destination AS route, "
        f"       COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} GROUP BY route HAVING n >= 3 "
        f"ORDER BY avg_delay DESC, n DESC LIMIT 5"
    ).fetchall()

    # Ø Verspätung nach Abfahrts-Tageszeit (3-Stunden-Fenster aus departure_time)
    by_hour = conn.execute(
        f"SELECT (CAST(substr(departure_time, 1, 2) AS INTEGER) / 3) * 3 AS bucket, "
        f"       COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} WHERE departure_time <> '' "
        f"GROUP BY bucket ORDER BY bucket"
    ).fetchall()

    # Ø Verspätung nach Reisedauer-Klasse
    by_duration = conn.execute(
        f"""
        SELECT bucket, ord, COUNT(*) AS n, AVG(delay_minutes) AS avg_delay
        FROM (
            SELECT delay_minutes,
                CASE WHEN duration_minutes < 60  THEN '< 1 h'
                     WHEN duration_minutes < 120 THEN '1–2 h'
                     WHEN duration_minutes < 240 THEN '2–4 h'
                     WHEN duration_minutes < 360 THEN '4–6 h'
                     ELSE '> 6 h' END AS bucket,
                CASE WHEN duration_minutes < 60  THEN 0
                     WHEN duration_minutes < 120 THEN 1
                     WHEN duration_minutes < 240 THEN 2
                     WHEN duration_minutes < 360 THEN 3
                     ELSE 4 END AS ord
            FROM {src} WHERE duration_minutes > 0
        )
        GROUP BY bucket, ord ORDER BY ord
        """
    ).fetchall()

    # Verspätungsverteilung, klassiert (0–5 gilt als pünktlich)
    distribution = conn.execute(
        f"""
        SELECT bucket, ord, COUNT(*) AS n
        FROM (
            SELECT CASE WHEN delay_minutes < 6  THEN '0–5'
                        WHEN delay_minutes < 16 THEN '6–15'
                        WHEN delay_minutes < 31 THEN '16–30'
                        WHEN delay_minutes < 61 THEN '31–60'
                        ELSE 'über 60' END AS bucket,
                   CASE WHEN delay_minutes < 6  THEN 0
                        WHEN delay_minutes < 16 THEN 1
                        WHEN delay_minutes < 31 THEN 2
                        WHEN delay_minutes < 61 THEN 3
                        ELSE 4 END AS ord
            FROM {src}
        )
        GROUP BY bucket, ord ORDER BY ord
        """
    ).fetchall()

    # Monatstrend (YYYY-MM aus dem DD.MM.YYYY-Datum)
    monthly = conn.execute(
        f"SELECT substr(date, 7, 4) || '-' || substr(date, 4, 2) AS ym, "
        f"       COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} WHERE length(date) = 10 GROUP BY ym ORDER BY ym"
    ).fetchall()

    # Ø Verspätung nach Wochentag (strftime auf ISO-umgebautem Datum; %w: 0 = So)
    weekday_raw = conn.execute(
        f"SELECT CAST(strftime('%w', substr(date, 7, 4) || '-' || substr(date, 4, 2) "
        f"|| '-' || substr(date, 1, 2)) AS INTEGER) AS dow, "
        f"       COUNT(*) AS n, AVG(delay_minutes) AS avg_delay "
        f"FROM {src} WHERE length(date) = 10 GROUP BY dow"
    ).fetchall()
    by_weekday = sorted(
        [{"label": WEEKDAYS_DE[r["dow"]], "n": r["n"], "avg_delay": r["avg_delay"]}
         for r in weekday_raw if r["dow"] is not None],
        key=lambda w: (WEEKDAYS_DE.index(w["label"]) + 6) % 7,  # Mo … So
    )

    return {
        **meta,
        "total": agg["total"],
        "contributors": agg["contributors"],
        "distinct_trains": agg["distinct_trains"],
        "distinct_routes": agg["distinct_routes"],
        "avg_delay": agg["avg_delay"],
        "median_delay": median_delay,
        "punctuality": agg["punctuality"],
        "share_15": agg["share_15"],
        "share_60": agg["share_60"],
        "final_avg": final_avg,
        "worst_delay": worst_delay,
        "distribution": distribution,
        "monthly": monthly,
        "by_weekday": by_weekday,
        "worst_trains": worst_trains,
        "worst_routes": worst_routes,
        "series_delay": series_delay,
        "by_hour": by_hour,
        "by_duration": by_duration,
    }


STYLE = """
/* App-Fonts (dieselben TTFs wie die Android-App): Inter für Text,
   Space Grotesk Bold für Überschriften/Kennzahlen. */
@font-face { font-family: 'Inter'; src: url('/fonts/inter.ttf') format('truetype');
  font-weight: 400; font-style: normal; font-display: swap; }
@font-face { font-family: 'Space Grotesk'; src: url('/fonts/spacegrotesk.ttf') format('truetype');
  font-weight: 700; font-style: normal; font-display: swap; }
:root {
  --bg: #faf8f5; --card: #ffffff; --ink: #1c1b1f; --muted: #6b6763;
  --accent: #e2001a; --line: rgba(28,27,31,0.1); --good: #1a8f4c;
  --display: 'Space Grotesk', -apple-system, BlinkMacSystemFont, sans-serif;
  --body: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
/* Designsystem ICEinfo („Bordkarte & Anzeigetafel"): bewusst hell, kein Dark-Mode. */
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--ink);
  font-family: var(--body);
  line-height: 1.4; -webkit-font-smoothing: antialiased; }
.wrap { max-width: 1000px; margin: 0 auto; padding: 32px 20px 60px; }
header { margin: 0 0 20px; }
header h1 { font-family: var(--display); font-size: 1.7rem; margin: 0 0 4px; letter-spacing: -0.02em; }
header p { color: var(--muted); margin: 0 0 8px; font-size: .9rem; }
.accent { color: var(--accent); }
/* Wortmarke „ICEinfo": kursiv, zweifarbig */
.brand { font-family: var(--display); font-style: italic; font-weight: 700; letter-spacing: -0.03em; }
.brand-a { color: var(--accent); }
.brand-b { color: var(--ink); }
.brand-sub { font-style: normal; color: var(--muted); font-weight: 700; margin-left: 6px; }

/* Bordkarte: die einzige gerahmte Fläche der Seite. Trägt Datenbasis,
   die Leitfrage (Pünktlichkeit) und die Kennzahlen als Ticket-Felder. */
.ticket { border: 1px solid var(--line); border-radius: 24px; background: var(--card);
  overflow: hidden; margin: 0 0 46px; }
.ticket-head, .ticket-fields {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(132px, 1fr)); }
.ticket-fields { border-top: 1px dashed var(--line); }
.field { padding: 16px 22px; border-left: 1px dashed var(--line); }
.field:first-child { border-left: none; }
.field .k { font-size: .74rem; font-weight: 700; letter-spacing: .08em;
  text-transform: uppercase; color: var(--muted); }
.field .v { font-family: var(--display); font-weight: 700; font-size: 1.3rem;
  margin-top: 5px; letter-spacing: -0.015em; font-variant-numeric: tabular-nums; }
.field .u { font-size: .7em; font-weight: 600; color: var(--muted); margin-left: 3px; }
.answer { padding: 34px 22px 36px; border-top: 1px dashed var(--line); }
.answer .k { font-size: .74rem; font-weight: 700; letter-spacing: .08em;
  text-transform: uppercase; color: var(--muted); }
.answer .v { font-family: var(--display); font-weight: 700; line-height: .92;
  font-size: clamp(3.4rem, 13vw, 5.2rem); letter-spacing: -0.035em;
  font-variant-numeric: tabular-nums; margin: 10px 0 12px; }
.answer .v .u { font-size: .3em; font-weight: 700; color: var(--muted);
  letter-spacing: 0; margin-left: .12em; }
.answer .d { color: var(--muted); font-size: .9rem; max-width: 52ch; }

/* Flache Abschnitte: Struktur durch Abstand und gestrichelte Nähte, nicht durch Kästen */
.section { margin: 0 0 46px; }
h2 { font-family: var(--body); font-size: .72rem; font-weight: 700; letter-spacing: .1em;
  text-transform: uppercase; color: var(--muted); margin: 0 0 20px; }
.block h3 { font-size: .9rem; font-weight: 600; margin: 0 0 14px; letter-spacing: -0.01em; }
.cols { display: grid; gap: 38px 48px; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); }
.note { color: var(--muted); font-size: .78rem; margin-top: 12px; }
.callout { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 14px;
  border-top: 1px dashed var(--line); margin-top: 22px; padding-top: 16px; }
.callout .k { font-size: .74rem; font-weight: 700; letter-spacing: .08em;
  text-transform: uppercase; color: var(--muted); }
.callout .v { font-family: var(--display); font-weight: 700; font-size: 1.15rem;
  color: var(--accent); font-variant-numeric: tabular-nums; }
.callout .s { color: var(--muted); font-size: .85rem; }

.grid { display: grid; gap: 24px 34px; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); }
.tile { border-top: 1px dashed var(--line); padding-top: 12px; }
.tile .value { font-family: var(--display); font-size: 1.6rem; font-weight: 700;
  letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
.tile .label { color: var(--muted); font-size: .78rem; margin-top: 3px; }
.tile .unit { font-size: .95rem; font-weight: 600; color: var(--muted); margin-left: 3px; }
table { width: 100%; border-collapse: collapse; font-size: .9rem; }
td { padding: 9px 0; border-bottom: 1px dashed var(--line); }
td:last-child { text-align: right; font-variant-numeric: tabular-nums; color: var(--muted);
  white-space: nowrap; padding-left: 16px; }
tr:last-child td { border-bottom: none; }

/* Schmale Viewports: Ticket-Nähte laufen waagerecht statt senkrecht */
@media (max-width: 640px) {
  .ticket-head, .ticket-fields { grid-template-columns: 1fr 1fr; }
  .field { border-left: none; border-top: 1px dashed var(--line); }
  .field:nth-child(odd) { border-right: 1px dashed var(--line); }
  .ticket-head .field:nth-child(-n+2),
  .ticket-fields .field:nth-child(-n+2) { border-top: none; }
}
.filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 20px; }
.filters.months { margin-top: -10px; margin-bottom: 26px; }
.chip { display: inline-block; padding: 5px 12px; border-radius: 8px; border: 1px solid var(--line);
  color: var(--muted); text-decoration: none; font-size: .85rem; font-weight: 600;
  font-variant-numeric: tabular-nums; }
.chip:hover { border-color: rgba(28,27,31,0.22); color: var(--ink); }
.chip.active { background: var(--accent); border-color: var(--accent); color: #fff; }
footer { color: var(--muted); font-size: .78rem; margin-top: 40px; text-align: center; }
footer a { color: inherit; text-decoration: underline; }
.footer-legal { margin-top: 6px; }
.api-status { margin-top: 10px; display: inline-flex; align-items: center; gap: 7px; }
.api-status .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--muted); }
.api-status.online .dot { background: var(--good); animation: breathe 2.4s ease-in-out infinite; }
.api-status.offline .dot { background: var(--accent); animation: none; }
@keyframes breathe {
  0%, 100% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--good) 50%, transparent); opacity: 1; }
  50% { box-shadow: 0 0 0 7px color-mix(in srgb, var(--good) 0%, transparent); opacity: .55; }
}
@media (prefers-reduced-motion: reduce) { .api-status.online .dot { animation: none; } }
.empty { text-align: center; padding: 80px 20px; color: var(--muted); }
.bar-row { display: grid; grid-template-columns: 64px 1fr auto; align-items: center;
  gap: 10px; padding: 5px 0; font-size: .88rem; }
.bar-label { color: var(--muted); font-variant-numeric: tabular-nums; }
.bar-track { background: var(--line); border-radius: 6px; height: 12px; overflow: hidden; }
.bar-fill { background: var(--accent); height: 100%; border-radius: 6px; min-width: 3px; }
.bar-val { font-variant-numeric: tabular-nums; font-weight: 600; }
.timeline { list-style: none; margin: 0; padding: 0; }
.timeline li { position: relative; padding: 0 0 20px 24px; }
.timeline li::before { content: ''; position: absolute; left: 3px; top: 5px;
  width: 9px; height: 9px; border-radius: 50%; background: var(--accent); }
.timeline li::after { content: ''; position: absolute; left: 7px; top: 16px;
  bottom: -2px; width: 0; border-left: 1px dashed var(--line); }
.timeline li:last-child { padding-bottom: 0; }
.timeline li:last-child::after { display: none; }
.timeline .stop-name { font-weight: 600; }
.timeline .stop-meta { color: var(--muted); font-size: .85rem; font-variant-numeric: tabular-nums; }
.delay-ok { color: var(--good); font-weight: 600; }
.delay-late { color: var(--accent); font-weight: 600; }
.timeline li.cancelled .stop-name { text-decoration: line-through; color: var(--muted); }
.timeline li.cancelled::before { background: var(--muted); box-shadow: none; }
.timeline li.additional::before { background: var(--good); box-shadow: none; }
.tag { display: inline-block; font-size: .72rem; font-weight: 600; padding: 1px 6px;
  border-radius: 6px; background: color-mix(in srgb, var(--good) 18%, transparent); color: var(--good); }
.timeline li.prognosis .stop-name { color: var(--muted); }
.timeline li.prognosis::before { background: var(--muted); box-shadow: none; opacity: .6; }
.timeline li.prognosis::after { background: none;
  border-left: 2px dashed var(--line); left: 6px; width: 0; }
.tag-prognosis { display: inline-block; font-size: .72rem; font-weight: 600; padding: 1px 6px;
  border-radius: 6px; background: color-mix(in srgb, var(--muted) 18%, transparent); color: var(--muted); }
.timeline-note { color: var(--muted); font-size: .85rem; margin: -4px 0 12px; }
"""


def _field(label: str, value: str) -> str:
    """Ticket-Feld: Label über Wert, getrennt durch gestrichelte Nähte."""
    return f'<div class="field"><div class="k">{label}</div><div class="v">{value}</div></div>'


def _route(r) -> str:
    return f"{r['origin']} → {r['destination']}"


def _train(r) -> str:
    return f"{r['train_type']} {r['train_number']}"


def _dec(x, n: int = 1) -> str:
    """Dezimalzahl mit deutschem Komma."""
    return f"{x:.{n}f}".replace(".", ",")


def _signed_min(m) -> str:
    """Verspätung mit Vorzeichen: +14 / −3 / 0."""
    m = int(round(m or 0))
    if m > 0:
        return f"+{_de(m)}"
    if m < 0:
        return f"−{_de(-m)}"
    return "0"


def _ym_label(ym: str) -> str:
    """"2026-07" → "Jul 26" für kompakte Trend-Beschriftung."""
    y, m = (ym.split("-") + [""])[:2]
    if m.isdigit() and 1 <= int(m) <= 12:
        return f"{MONTHS_SHORT_DE[int(m) - 1]} {y[2:]}"
    return ym


def _tile(value: str, label: str, unit: str = "") -> str:
    u = f'<span class="unit">{unit}</span>' if unit else ""
    return (
        f'<div class="tile"><div class="value">{value}{u}</div>'
        f'<div class="label">{label}</div></div>'
    )


def _table(title: str, rows, fmt) -> str:
    body = "".join(f"<tr><td>{fmt(r)[0]}</td><td>{fmt(r)[1]}</td></tr>" for r in rows)
    return f'<div class="block"><h3>{title}</h3><table>{body}</table></div>'


def _bars(title: str, bars, note: str = "", level: str = "h3") -> str:
    """bars: Liste von (label, wert, anzeige). Balkenbreite skaliert auf den Maximalwert.
    level: h2, wenn der Block allein eine Sektion bildet (Heading-Hierarchie ohne Sprung)."""
    mx = max((v for _, v, _ in bars), default=0) or 1
    body = "".join(
        f'<div class="bar-row"><div class="bar-label">{label}</div>'
        f'<div class="bar-track"><div class="bar-fill" style="width:{max(0, v) / mx * 100:.0f}%"></div></div>'
        f'<div class="bar-val">{disp}</div></div>'
        for label, v, disp in bars
    )
    sub = f'<div class="note">{note}</div>' if note else ""
    return f'<div class="block"><{level}>{title}</{level}>{body}{sub}</div>'


def _filter_bar(s) -> str:
    """Chip-Filterleiste: Gesamt · Jahre · (bei aktivem Jahr) dessen Monate."""
    period = s.get("period", "")
    active_year = period[:4] if period else ""
    chips = [f'<a class="chip{"" if period else " active"}" href="/">Gesamt</a>']
    for y in s.get("years", []):
        chips.append(f'<a class="chip{" active" if active_year == y else ""}" href="/?period={y}">{y}</a>')
    bar = f'<div class="filters">{"".join(chips)}</div>'
    months = s.get("months", [])
    if active_year and months:
        mchips = []
        for m in months:
            pv = f"{active_year}-{m}"
            name = MONTHS_SHORT_DE[int(m) - 1] if m.isdigit() and 1 <= int(m) <= 12 else m
            mchips.append(f'<a class="chip{" active" if period == pv else ""}" href="/?period={pv}">{name}</a>')
        bar += f'<div class="filters months">{"".join(mchips)}</div>'
    return bar


def render(period: str = "") -> str:
    s = load_stats(period)
    now = datetime.now().strftime("%d.%m.%Y %H:%M")
    label = s.get("period_label", "")
    head = (
        "<!doctype html><html lang='de'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        "<meta http-equiv='refresh' content='300'>"
        "<title>ICE Info · Statistiken</title>"
        f"<style>{STYLE}</style></head><body><div class='wrap'>"
        "<header><h1 class='brand'><span class='brand-a'>ICE</span><span class='brand-b'>info</span>"
        "<span class='brand-sub'>Statistiken</span></h1></header>"
    )
    filters = _filter_bar(s)
    foot = (
        f"<footer>Stand: {now} · aktualisiert sich alle 5 Min · "
        "Daten anonym von der ICE-Info-App beigetragen"
        f"<div class='footer-legal'>{LEGAL_LINKS}</div>"
        f"{API_STATUS_HTML}</footer>"
        f"{API_STATUS_SCRIPT}</div></body></html>"
    )

    if not s["total"]:
        empty = (
            "<div class='empty'>Keine Fahrten in diesem Zeitraum.<br>"
            "<a href='/'>Gesamtstatistik anzeigen</a></div>"
            if label else
            "<div class='empty'>Noch keine Fahrten geteilt.<br>"
            "Sobald die ersten Fahrten eintreffen, erscheinen hier die Statistiken.</div>"
        )
        return head + filters + empty + foot

    # ── Bordkarte: Datenbasis, Leitfrage und Kennzahlen in einem Objekt ──────
    head_fields = [
        _field("Zeitraum", label or "Gesamt"),
        _field("Fahrten", _de(s["total"])),
        _field("Beitragende", _de(s["contributors"])),
        _field("Züge", _de(s["distinct_trains"])),
        _field("Strecken", _de(s["distinct_routes"])),
    ]
    kpi_fields = [
        _field("Ø Verspätung", f'{_dec(s["avg_delay"])}<span class="u">min</span>'),
        _field("Median", f'{_de(s["median_delay"])}<span class="u">min</span>'),
        _field("≥ 15 min", f'{s["share_15"]:.0f}<span class="u">%</span>'),
        _field("≥ 60 min", f'{s["share_60"]:.0f}<span class="u">%</span>'),
    ]
    fa = s["final_avg"]
    if fa and fa["n"]:
        kpi_fields.append(_field("Ø Endbahnhof", f'{_dec(fa["a"])}<span class="u">min</span>'))

    ticket = (
        '<div class="ticket">'
        f'<div class="ticket-head">{"".join(head_fields)}</div>'
        '<div class="answer"><div class="k">Pünktlichkeit</div>'
        f'<div class="v">{s["punctuality"]:.0f}<span class="u">%</span></div>'
        '<div class="d">Anteil der Fahrten, die mit weniger als sechs Minuten '
        'Verspätung am Ausstiegsbahnhof ankamen.</div></div>'
        f'<div class="ticket-fields">{"".join(kpi_fields)}</div>'
        '</div>'
    )

    # ── Verteilung: die Form der Verspätungen, plus der Extremwert ───────────
    dist_html = ""
    if s["distribution"]:
        callout = ""
        if s["worst_delay"]:
            r = s["worst_delay"]
            callout = (
                '<div class="callout"><span class="k">Größte Einzelverspätung</span>'
                f'<span class="v">+{_de(r["delay_minutes"])} min</span>'
                f'<span class="s">{_train(r)} · {_route(r)} · {r["date"]}</span></div>'
            )
        bars = _bars(
            "Verspätungsverteilung",
            [(r["bucket"], r["n"], f'{r["n"] / s["total"] * 100:.0f} % · {r["n"]}×')
             for r in s["distribution"]],
            note="Minuten Verspätung am Ausstieg · unter 6 min gilt als pünktlich",
            level="h2",
        )
        dist_html = f'<div class="section">{bars}{callout}</div>'

    # ── Trends & Muster ──────────────────────────────────────────────────────
    patterns = []
    if len(s["monthly"]) >= 2:
        patterns.append(_bars(
            "Ø Verspätung pro Monat",
            [(_ym_label(r["ym"]), r["avg_delay"], f'+{_dec(r["avg_delay"])} min · {r["n"]}×')
             for r in s["monthly"][-12:]],
            note="Monatstrend · zuletzt max. 12 Monate",
        ))
    if s["by_weekday"]:
        patterns.append(_bars(
            "Ø Verspätung nach Wochentag",
            [(w["label"], w["avg_delay"], f'+{_dec(w["avg_delay"])} min · {w["n"]}×')
             for w in s["by_weekday"]],
            note="nach Abfahrtsdatum",
        ))
    if s["by_hour"]:
        patterns.append(_bars(
            "Ø Verspätung nach Tageszeit",
            [(f'{r["bucket"]:02d}–{(r["bucket"] + 3) % 24:02d}', r["avg_delay"],
              f'+{_dec(r["avg_delay"])} min · {r["n"]}×') for r in s["by_hour"]],
            note="nach Abfahrtszeit · 3-Stunden-Fenster",
        ))
    if s["by_duration"]:
        patterns.append(_bars(
            "Ø Verspätung nach Fahrzeit",
            [(r["bucket"], r["avg_delay"], f'+{_dec(r["avg_delay"])} min · {r["n"]}×')
             for r in s["by_duration"]],
            note="nach Reisedauer der Fahrt",
        ))
    patterns_html = (
        f'<div class="section"><h2>Trends &amp; Muster</h2>'
        f'<div class="cols">{"".join(patterns)}</div></div>'
        if patterns else ""
    )

    # ── Ranglisten: nur Pünktlichkeit, immer mit Stichprobengröße ────────────
    tables = []
    if s["worst_trains"]:
        tables.append(_table(
            "Unpünktlichste Züge (Ø, ab 3 Fahrten)",
            s["worst_trains"],
            lambda r: (r["train"], f'+{_dec(r["avg_delay"])} min · {r["n"]}×'),
        ))
    if s["worst_routes"]:
        tables.append(_table(
            "Unpünktlichste Strecken (Ø, ab 3 Fahrten)",
            s["worst_routes"],
            lambda r: (r["route"], f'+{_dec(r["avg_delay"])} min · {r["n"]}×'),
        ))
    if s["series_delay"]:
        tables.append(_table(
            "Pünktlichkeit je Baureihe (Ø, ab 3 Fahrten)",
            s["series_delay"],
            lambda r: (SERIES_NAMES.get(r["series"], r["series"]),
                       f'+{_dec(r["avg_delay"])} min · {r["n"]}×'),
        ))
    tables_html = (
        f'<div class="section"><h2>Ranglisten</h2>'
        f'<div class="cols">{"".join(tables)}</div></div>'
        if tables else ""
    )

    return head + filters + ticket + dist_html + patterns_html + tables_html + foot


def _og_tags(title: str, description: str, url: str, image: str = "") -> str:
    """Open-Graph-/Twitter-Meta-Tags für Chat-Vorschaukarten (mit Bild, falls gesetzt)."""
    t = html.escape(title, quote=True)
    d = html.escape(description, quote=True)
    u = html.escape(url, quote=True)
    tags = (
        f"<meta property='og:title' content='{t}'>"
        f"<meta property='og:description' content='{d}'>"
        "<meta property='og:type' content='website'>"
        "<meta property='og:site_name' content='ICE Info'>"
        f"<meta property='og:url' content='{u}'>"
        f"<meta name='twitter:title' content='{t}'>"
        f"<meta name='twitter:description' content='{d}'>"
    )
    if image:
        im = html.escape(image, quote=True)
        tags += (
            f"<meta property='og:image' content='{im}'>"
            "<meta property='og:image:width' content='1200'>"
            "<meta property='og:image:height' content='630'>"
            f"<meta name='twitter:image' content='{im}'>"
            "<meta name='twitter:card' content='summary_large_image'>"
        )
    else:
        tags += "<meta name='twitter:card' content='summary'>"
    return tags


def _journey_page(inner: str, head_extra: str = "", title: str = "Fahrt · ICE Info") -> str:
    """Rahmen für die Einzel-Fahrt-Seite (ohne Auto-Refresh)."""
    return (
        "<!doctype html><html lang='de'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        "<meta name='robots' content='noindex'>"
        f"{head_extra}"
        f"<title>{html.escape(title)}</title>"
        f"<style>{STYLE}</style></head><body><div class='wrap'>"
        f"{inner}"
        "<footer>Anonym geteilte Fahrt · ICE Info · "
        "<a href='/'>zur Gesamtstatistik</a>"
        f"<div class='footer-legal'>{LEGAL_LINKS}</div>"
        f"{API_STATUS_HTML}</footer>"
        f"{API_STATUS_SCRIPT}</div></body></html>"
    )


def render_journey(jhash: str) -> str:
    if not re.fullmatch(r"[0-9a-f]{64}", jhash or ""):
        return _journey_page("<div class='empty'>Ungültiger Link.</div>")
    try:
        conn = _connect()
        with conn:
            r = conn.execute("SELECT * FROM journeys WHERE hash = ?", (jhash,)).fetchone()
    except sqlite3.OperationalError:
        r = None
    if not r:
        return _journey_page(
            "<div class='empty'>Diese Fahrt wurde nicht gefunden.<br>"
            "Sie ist möglicherweise nicht (mehr) geteilt.</div>"
        )

    h, m = divmod(int(r["duration_minutes"] or 0), 60)
    tiles = [
        _tile(_signed_min(r["delay_minutes"]), "Verspätung am Ausstieg", "min"),
    ]
    if r["final_station"]:
        label = "Endbahnhof" + (" (Prognose)" if r["final_delay_is_prognosis"] else "")
        tiles.append(_tile(_signed_min(r["final_delay_minutes"]), f'{label} · {r["final_station"]}', "min"))
    if r["departure_time"]:
        tiles.append(_tile(r["departure_time"], "Abfahrt"))
    if r["arrival_time"]:
        tiles.append(_tile(r["arrival_time"], "Ankunft"))
    if r["duration_minutes"]:
        tiles.append(_tile(f'{h} h {m:02d} min', "Reisezeit"))
    if r["distance_km"]:
        tiles.append(_tile(_de(r["distance_km"]), "Distanz", "km"))
    if r["stops_count"]:
        tiles.append(_tile(_de(r["stops_count"]), "Halte"))
    if r["series"]:
        tiles.append(_tile(SERIES_NAMES.get(r["series"], r["series"]), "Baureihe"))

    shared_at = (r["received_at"] or "")[:10]
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}", shared_at):  # ISO → deutsches Format
        y, m, d = shared_at.split("-")
        shared_at = f"{d}.{m}.{y}"
    header = (
        f"<header><h1 class='accent'>{_train(r)}</h1>"
        f"<p style='font-size:1.05rem;color:var(--ink);margin:0 0 2px'>{_route(r)}</p>"
        f"<p>{r['date']}"
        + (f" · geteilt am {shared_at}" if shared_at else "")
        + "</p></header>"
    )

    d = r["delay_minutes"]
    delay_str = "pünktlich" if d == 0 else f"{_signed_min(d)} min"
    og_title = f"{_train(r)} · {delay_str}"
    og_desc = f"{_route(r)} · {r['date']}"
    og = _og_tags(og_title, og_desc, f"{PUBLIC_BASE}/j/{jhash}",
                  image=f"{PUBLIC_BASE}/j/{jhash}/card.png")

    body = header + f'<div class="grid">{"".join(tiles)}</div>' + _timeline(r)

    return _journey_page(body, head_extra=og, title=og_title)


def _timeline(r) -> str:
    """Fahrtverlauf als vertikale Timeline (leer, wenn keine Halte geteilt wurden)."""
    try:
        stops = json.loads(r["stops_json"] or "[]")
    except (ValueError, TypeError, IndexError):
        stops = []
    if not stops:
        return ""
    has_prognosis = any(s.get("prognosis") for s in stops)
    items = ""
    for s in stops:
        name = html.escape(str(s.get("name", "")))
        t = html.escape(str(s.get("time", "")))
        dm = int(s.get("delayMinutes", 0) or 0)
        cancelled = bool(s.get("cancelled"))
        additional = bool(s.get("additional"))
        prognosis = bool(s.get("prognosis"))
        classes = []
        if cancelled:
            classes.append("cancelled")
        elif additional:
            classes.append("additional")
        if prognosis:
            classes.append("prognosis")
        li_class = f" class=\"{' '.join(classes)}\"" if classes else ""
        if cancelled:
            status = "<span class='delay-late'>entfällt</span>"
        elif prognosis:
            # hinter dem Ausstieg: Wert ist nur Prognose, nicht gemessen
            status = f"<span class='tag-prognosis'>{_signed_min(dm)} min · Prognose</span>"
        elif dm > 0:
            status = f"<span class='delay-late'>{_signed_min(dm)} min</span>"
        elif dm < 0:
            status = f"<span class='delay-ok'>{_signed_min(dm)} min</span>"
        else:
            status = "<span class='delay-ok'>pünktlich</span>"
        if additional and not cancelled:
            status += " · <span class='tag'>Zusatzhalt</span>"
        meta = " · ".join(x for x in [t, status] if x)
        items += f"<li{li_class}><div class='stop-name'>{name}</div><div class='stop-meta'>{meta}</div></li>"
    note = ("<div class='timeline-note'>Ab dem Ausstieg sind die Zeiten nur noch "
            "Prognose zum Ausstiegszeitpunkt, nicht gemessen.</div>") if has_prognosis else ""
    return (f'<div class="section" style="margin-top:40px"><h2>Fahrtverlauf</h2>'
            f'{note}<ol class="timeline">{items}</ol></div>')


def _font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


def _fit_font(draw, text, name, start, min_size, max_width):
    """Verkleinert die Schrift, bis der Text in max_width passt."""
    size = start
    while size > min_size:
        f = _font(name, size)
        if draw.textlength(text, font=f) <= max_width:
            return f
        size -= 4
    return _font(name, min_size)


def render_card(r) -> bytes:
    """Rendert das Open-Graph-Vorschaubild (1200×630 PNG) einer geteilten Fahrt."""
    W, H = 1200, 630
    bg = (250, 248, 245)      # Paper – bewusst warmweiß, nie dunkel
    ink = (28, 27, 31)
    muted = (107, 103, 99)    # Slate
    accent = (226, 0, 26)     # DB-Rot (nur funktional: Route/Verspätung)
    good = (26, 143, 76)
    hair = (231, 226, 219)    # Hairline auf Paper

    img = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], outline=hair, width=2)
    d.rectangle([0, 0, 12, H], fill=accent)  # roter Routen-Akzentstreifen

    pad = 80
    maxw = W - pad * 2

    d.text((pad, 66), f"{r['train_type']} {r['train_number']}",
           font=_font("spacegrotesk.ttf", 118), fill=ink)

    route = f"{r['origin']} → {r['destination']}"
    d.text((pad, 232), route, font=_fit_font(d, route, "inter.ttf", 52, 30, maxw), fill=ink)

    times = str(r["date"])
    if r["departure_time"] and r["arrival_time"]:
        times += f" · {r['departure_time']}–{r['arrival_time']}"
    d.text((pad, 300), times, font=_font("inter.ttf", 34), fill=muted)

    dm = int(r["delay_minutes"] or 0)
    dtxt = "pünktlich" if dm == 0 else f"{_signed_min(dm)} min"
    dcol = good if dm <= 0 else accent
    d.text((pad, 392), "Verspätung am Ausstieg", font=_font("inter.ttf", 32), fill=muted)
    d.text((pad, 430), dtxt, font=_font("spacegrotesk.ttf", 100), fill=dcol)

    fy = H - 92
    fw = _font("spacegrotesk.ttf", 40)
    d.text((pad, fy), "ICE", font=fw, fill=accent)
    d.text((pad + d.textlength("ICE", font=fw), fy), "info", font=fw, fill=ink)
    d.text((pad + d.textlength("ICEinfo", font=fw) + 16, fy + 8),
           "· Verspätungsstatistik", font=_font("inter.ttf", 28), fill=muted)

    buf = BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


@app.get("/", response_class=HTMLResponse)
def dashboard(period: str = "") -> str:
    return render(period)


@app.get("/j/{jhash}", response_class=HTMLResponse)
def journey(jhash: str) -> str:
    return render_journey(jhash)


@app.get("/j/{jhash}/card.png")
def journey_card(jhash: str):
    if not re.fullmatch(r"[0-9a-f]{64}", jhash or ""):
        raise HTTPException(status_code=404)
    try:
        conn = _connect()
        with conn:
            r = conn.execute("SELECT * FROM journeys WHERE hash = ?", (jhash,)).fetchone()
    except sqlite3.OperationalError:
        r = None
    if not r:
        raise HTTPException(status_code=404)
    return Response(
        content=render_card(r),
        media_type="image/png",
        headers={"Cache-Control": "public, max-age=86400"},
    )


@app.get("/fonts/{name}")
def font(name: str):
    if name not in _FONTS:
        raise HTTPException(status_code=404)
    return FileResponse(
        os.path.join(FONT_DIR, name),
        media_type="font/ttf",
        headers={"Cache-Control": "public, max-age=31536000, immutable"},
    )


@app.get("/api-status")
def api_status() -> dict:
    """Prüft die Schreib-API (intern im Docker-Netz) für die Live-Statusanzeige."""
    try:
        with urllib.request.urlopen(API_HEALTH_URL, timeout=2) as resp:
            return {"ok": resp.status == 200}
    except Exception:
        return {"ok": False}


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}
