"""ICE Info Stats API — nimmt geteilte Fahrten aus der App entgegen.

Endpunkte:
    GET  /v1/health    — Lebenszeichen (ohne Auth)
    POST /v1/journeys  — Fahrt einliefern (Bearer-Token nötig)

Dedup: Der Server bildet SHA-256 über installId|trainType|trainNumber|origin|
destination|date und nutzt ihn als Primärschlüssel (INSERT OR REPLACE) —
dieselbe Fahrt derselben Installation überschreibt sich selbst.
"""

import hashlib
import json
import os
import re
import sqlite3
import time
from contextlib import closing

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, Field, field_validator

API_TOKEN = os.environ["API_TOKEN"]  # bewusst ohne Default: ohne Token kein Start
DB_PATH = os.environ.get("DB_PATH", "/data/journeys.db")

# Limits gegen Missbrauch — großzügig für echte Nutzung
MAX_PER_INSTALL_PER_DAY = 20
MAX_PER_IP_PER_HOUR = 60

API_DESCRIPTION = """\
Crowdsourcing-Schnittstelle der **ICE-Info-Live**-App: Nutzer teilen manuell
(pro Fahrt einzeln bestätigt) aufgezeichnete Fahrten. Dieser Dienst nimmt sie per
`POST /v1/journeys` entgegen und legt sie in einer SQLite-Datei ab.

**Auth:** `POST /v1/journeys` erfordert `Authorization: Bearer <API_TOKEN>`.

**Dedup:** Primärschlüssel ist `SHA-256(installId|trainType|trainNumber|origin|
destination|date)`. Erneutes Teilen derselben Fahrt überschreibt den Eintrag
(`INSERT OR REPLACE`); `delayMinutes` ist bewusst **nicht** Teil des Hashes,
damit ein Re-Share eine korrigierte Verspätung aktualisiert.

**Limits:** 20 Fahrten/Tag pro `installId`, 60 Requests/Stunde pro IP.

Die öffentliche Anzeige (Dashboard und Einzelfahrt-Seiten `/j/{hash}`) läuft in
einem eigenen Dienst unter `stats.iceinfo.de`.
"""

app = FastAPI(
    title="ICE Info Stats API",
    version="1.0",
    description=API_DESCRIPTION,
    docs_url="/docs",          # Swagger UI: https://api.iceinfo.de/docs
    redoc_url="/redoc",        # ReDoc:      https://api.iceinfo.de/redoc
    openapi_tags=[
        {"name": "Journeys", "description": "Fahrten einliefern (Bearer-Token nötig)."},
        {"name": "OAuth", "description": "Träwelling-OAuth-Redirect-Weiche für die App."},
        {"name": "Health", "description": "Lebenszeichen (ohne Auth)."},
    ],
)

_ip_hits: dict[str, list[float]] = {}  # in-memory Rate-Limit pro IP


def _db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def _init_db() -> None:
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    with closing(_db()) as conn, conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS journeys (
                hash          TEXT PRIMARY KEY,
                train_type    TEXT NOT NULL,
                train_number  TEXT NOT NULL,
                origin        TEXT NOT NULL,
                destination   TEXT NOT NULL,
                date          TEXT NOT NULL,
                delay_minutes INTEGER NOT NULL,
                series        TEXT NOT NULL DEFAULT '',
                install_id    TEXT NOT NULL,
                app_version   TEXT NOT NULL DEFAULT '',
                received_at   TEXT NOT NULL DEFAULT (datetime('now'))
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_install ON journeys(install_id)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_train ON journeys(train_number, date)")
        # Migration: später ergänzte Spalten in bestehenden Datenbanken nachziehen
        existing = {row[1] for row in conn.execute("PRAGMA table_info(journeys)")}
        for column, ddl in [
            ("departure_time", "TEXT NOT NULL DEFAULT ''"),
            ("arrival_time", "TEXT NOT NULL DEFAULT ''"),
            ("duration_minutes", "INTEGER NOT NULL DEFAULT 0"),
            ("distance_km", "INTEGER NOT NULL DEFAULT 0"),
            ("stops_count", "INTEGER NOT NULL DEFAULT 0"),
            ("tzn", "TEXT NOT NULL DEFAULT ''"),
            ("final_station", "TEXT NOT NULL DEFAULT ''"),
            ("final_delay_minutes", "INTEGER NOT NULL DEFAULT 0"),
            ("final_delay_is_prognosis", "INTEGER NOT NULL DEFAULT 0"),
            ("stops_json", "TEXT NOT NULL DEFAULT '[]'"),
        ]:
            if column not in existing:
                conn.execute(f"ALTER TABLE journeys ADD COLUMN {column} {ddl}")


_init_db()


class Stop(BaseModel):
    """Einzelner Halt im Fahrtverlauf."""

    name: str = Field(max_length=80, description="Bahnhofsname.")
    time: str = Field(default="", max_length=5, description='Uhrzeit "HH:MM" (leer erlaubt).')
    delayMinutes: int = Field(default=0, ge=-30, le=600, description="Verspätung an diesem Halt in Minuten.")
    cancelled: bool = Field(default=False, description="Halt entfällt.")
    additional: bool = Field(default=False, description="Zusatzhalt.")
    prognosis: bool = Field(default=False, description="Liegt hinter dem Ausstieg → Wert ist nur Prognose, nicht gemessen.")


class Journey(BaseModel):
    """Eine geteilte Fahrt.

    Felder ab ``departureTime`` sind optional (App ≥ 6.4); ältere Clients lassen
    sie weg, fehlende Werte landen als ``""``/``0`` in der Datenbank.
    """

    trainType: str = Field(min_length=1, max_length=10, description='Zuggattung, z. B. "ICE", "IC".')
    trainNumber: str = Field(min_length=1, max_length=10, description='Zugnummer, z. B. "512".')
    origin: str = Field(min_length=2, max_length=80, description="Einstiegsbahnhof des Nutzers.")
    destination: str = Field(min_length=2, max_length=80, description="Ausstiegsbahnhof des Nutzers (Bezugspunkt für delayMinutes).")
    date: str = Field(description='Fahrtdatum "DD.MM.YYYY".')
    delayMinutes: int = Field(ge=-30, le=600, description="Verspätung am Ausstiegshalt (destination) in Minuten.")
    series: str = Field(default="", max_length=10, description='Baureihe, z. B. "412".')
    installId: str = Field(min_length=8, max_length=64, description="Zufällige, nicht personenbezogene Installations-ID (Dedup & Missbrauchsschutz).")
    appVersion: str = Field(default="", max_length=20, description='App-Version, z. B. "6.4".')
    # Optionale Zusatzfelder (ab App 6.4) — alte Clients dürfen sie weglassen
    departureTime: str = Field(default="", max_length=5, description='Planmäßige Abfahrt "HH:MM" (leer erlaubt).')
    arrivalTime: str = Field(default="", max_length=5, description='Ankunft "HH:MM" (leer erlaubt).')
    durationMinutes: int = Field(default=0, ge=0, le=2000, description="Reisedauer in Minuten.")
    distanceKm: int = Field(default=0, ge=0, le=3000, description="Distanz in Kilometern.")
    stopsCount: int = Field(default=0, ge=0, le=60, description="Anzahl der Halte.")
    tzn: str = Field(default="", max_length=12, description='Triebzugnummer, z. B. "ICE0304".')
    # Verspätung am Zug-Endbahnhof (delayMinutes ist am Ausstieg gemessen).
    # isPrognosis=True → Nutzer stieg vorher aus, Wert ist nur die Prognose.
    finalStation: str = Field(default="", max_length=80, description="Endbahnhof des Zuges.")
    finalDelayMinutes: int = Field(default=0, ge=-30, le=600, description="Verspätung am Endbahnhof (ggf. Prognose, s. finalDelayIsPrognosis).")
    finalDelayIsPrognosis: bool = Field(default=False, description="true → Nutzer stieg vor dem Endbahnhof aus; finalDelayMinutes ist nur Prognose.")
    stops: list["Stop"] = Field(default_factory=list, max_length=80, description="Fahrtverlauf (Halte mit Zeiten und Verspätungen).")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "trainType": "ICE", "trainNumber": "512",
                    "origin": "München Hbf", "destination": "Berlin Hbf",
                    "date": "23.07.2026", "delayMinutes": 14,
                    "series": "412",
                    "installId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                    "appVersion": "6.4",
                    "departureTime": "14:02", "arrivalTime": "18:47",
                    "durationMinutes": 285, "distanceKm": 623, "stopsCount": 5,
                    "tzn": "ICE0304",
                    "finalStation": "Berlin Hbf", "finalDelayMinutes": 14,
                    "finalDelayIsPrognosis": False,
                    "stops": [
                        {"name": "München Hbf", "time": "14:02", "delayMinutes": 0},
                        {"name": "Nürnberg Hbf", "time": "15:08", "delayMinutes": 3},
                        {"name": "Erfurt Hbf", "time": "16:15", "delayMinutes": 12, "additional": True},
                        {"name": "Halle (Saale) Hbf", "time": "16:55", "delayMinutes": 14, "prognosis": True},
                        {"name": "Berlin Hbf", "time": "18:47", "delayMinutes": 14, "prognosis": True},
                    ],
                }
            ]
        }
    }

    @field_validator("departureTime", "arrivalTime")
    @classmethod
    def _time_format(cls, v: str) -> str:
        if v and not re.fullmatch(r"\d{2}:\d{2}", v):
            raise ValueError("Zeit muss HH:MM sein")
        return v

    @field_validator("date")
    @classmethod
    def _date_format(cls, v: str) -> str:
        if not re.fullmatch(r"\d{2}\.\d{2}\.\d{4}", v):
            raise ValueError("date muss DD.MM.YYYY sein")
        return v


def _check_ip_limit(ip: str) -> None:
    now = time.monotonic()
    hits = [t for t in _ip_hits.get(ip, []) if now - t < 3600]
    if len(hits) >= MAX_PER_IP_PER_HOUR:
        raise HTTPException(429, "Zu viele Anfragen")
    hits.append(now)
    _ip_hits[ip] = hits


@app.get("/v1/health", tags=["Health"], summary="Lebenszeichen der Schreib-API")
def health() -> dict:
    return {"status": "ok"}


@app.get("/callback", tags=["OAuth"], summary="Träwelling-OAuth-Redirect an die App")
def oauth_callback(request: Request) -> RedirectResponse:
    """OAuth-Redirect-Weiche für die App (Träwelling verlangt eine https-Redirect-URI).

    Leitet den Authorization-Code per 302 an den App-Deep-Link weiter. Bei PKCE ist
    der Code ohne den geräteseitigen code_verifier wertlos, der kurze Durchlauf über
    den Server ist daher unbedenklich (der Code wird bewusst nicht geloggt).
    """
    query = request.url.query
    target = "iceinfo://traewelling/callback"
    if query:
        target += "?" + query
    return RedirectResponse(url=target, status_code=302)


@app.post(
    "/v1/journeys",
    status_code=201,
    tags=["Journeys"],
    summary="Fahrt einliefern",
    responses={
        201: {"description": "Fahrt gespeichert (bzw. bestehende überschrieben).",
              "content": {"application/json": {"example": {"hash": "3f2c1a…64hex"}}}},
        401: {"description": "Fehlendes oder ungültiges Bearer-Token.",
              "content": {"application/json": {"example": {"detail": "Ungültiges Token"}}}},
        429: {"description": "Rate-Limit: 'Zu viele Anfragen' (> 60/Std pro IP) oder "
                             "'Tageslimit erreicht' (≥ 20/Tag pro installId).",
              "content": {"application/json": {"example": {"detail": "Tageslimit erreicht"}}}},
    },
)
def create_journey(
    journey: Journey,
    request: Request,
    authorization: str = Header(default=""),
) -> dict:
    if authorization != f"Bearer {API_TOKEN}":
        raise HTTPException(401, "Ungültiges Token")

    # Hinter Cloudflare steht die echte Client-IP im CF-Connecting-IP-Header
    ip = request.headers.get("cf-connecting-ip") or (request.client.host if request.client else "?")
    _check_ip_limit(ip)

    key = "|".join([
        journey.installId, journey.trainType, journey.trainNumber,
        journey.origin, journey.destination, journey.date,
    ])
    journey_hash = hashlib.sha256(key.encode()).hexdigest()

    with closing(_db()) as conn, conn:
        (count,) = conn.execute(
            "SELECT COUNT(*) FROM journeys WHERE install_id = ? AND received_at > datetime('now', '-1 day')",
            (journey.installId,),
        ).fetchone()
        if count >= MAX_PER_INSTALL_PER_DAY:
            raise HTTPException(429, "Tageslimit erreicht")

        conn.execute(
            """
            INSERT OR REPLACE INTO journeys
                (hash, train_type, train_number, origin, destination, date,
                 delay_minutes, series, install_id, app_version,
                 departure_time, arrival_time, duration_minutes, distance_km,
                 stops_count, tzn, final_station, final_delay_minutes,
                 final_delay_is_prognosis, stops_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                journey_hash, journey.trainType, journey.trainNumber,
                journey.origin, journey.destination, journey.date,
                journey.delayMinutes, journey.series, journey.installId,
                journey.appVersion,
                journey.departureTime, journey.arrivalTime,
                journey.durationMinutes, journey.distanceKm,
                journey.stopsCount, journey.tzn,
                journey.finalStation, journey.finalDelayMinutes,
                int(journey.finalDelayIsPrognosis),
                json.dumps([s.model_dump() for s in journey.stops], ensure_ascii=False),
            ),
        )

    return {"hash": journey_hash}
