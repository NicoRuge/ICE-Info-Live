# ICE Info Stats API

Kleiner FastAPI-Dienst, der von Nutzern manuell geteilte Fahrten entgegennimmt
(Crowdsourcing für Verspätungsstatistiken). Läuft als Docker-Container hinter
dem vorhandenen Cloudflare Tunnel unter `https://api.iceinfo.de`.

## Deployment (Hetzner)

```bash
# 1. Ordner auf den Server kopieren
scp -r server root@<server-ip>:/opt/iceinfo-api
ssh root@<server-ip>

# 2. Token erzeugen und als .env ablegen
cd /opt/iceinfo-api
echo "API_TOKEN=$(openssl rand -hex 32)" > .env
chmod 600 .env
cat .env   # Token notieren → kommt in local.properties der App

# 3. Starten
docker compose up -d --build

# 4. Lokal testen
curl http://127.0.0.1:8091/v1/health
```

## Cloudflare Tunnel: api.iceinfo.de anlegen

Cloudflare Dashboard → Zero Trust → Networks → Tunnels → euren Tunnel →
**Public Hostname** → Add:

- Subdomain `api`, Domain `iceinfo.de`
- Service-Typ `HTTP`

Als Service-Ziel je nach Netzwerk-Setup von cloudflared (prüfen mit
`docker inspect cloudflared --format '{{json .NetworkSettings.Networks}}'`):

- cloudflared im **Default-Bridge-Netz** oder mit `network_mode: host`:
  Ziel `http://172.17.0.1:8091` (Docker-Host-Gateway) bzw. `http://localhost:8091`
- cloudflared in einem **eigenen Compose-Netzwerk**: das Netzwerk in
  `docker-compose.yml` einkommentieren/eintragen und als Ziel
  `http://iceinfo-api:8000` verwenden

Danach von außen testen:

```bash
curl https://api.iceinfo.de/v1/health
```

## API

`POST /v1/journeys` — Header `Authorization: Bearer <API_TOKEN>`, Body:

```json
{
  "trainType": "ICE", "trainNumber": "512",
  "origin": "München Hbf", "destination": "Berlin Hbf",
  "date": "23.07.2026", "delayMinutes": 14,
  "series": "412", "installId": "<uuid>", "appVersion": "6.4",
  "departureTime": "14:02", "arrivalTime": "18:47",
  "durationMinutes": 285, "distanceKm": 623, "stopsCount": 5,
  "tzn": "ICE0304",
  "finalStation": "München Hbf", "finalDelayMinutes": 14,
  "finalDelayIsPrognosis": false
}
```

Die Felder ab `departureTime` sind optional (ältere App-Versionen lassen sie
weg); fehlende Werte landen als `''`/`0` in der Datenbank.

**Ausstieg vs. Endbahnhof:** `delayMinutes` ist am Ausstiegshalt des Nutzers
gemessen (`destination`), `finalDelayMinutes` am Endbahnhof des Zuges
(`finalStation`). Ist `finalDelayIsPrognosis = 1`, stieg der Nutzer vor dem
Endbahnhof aus — dann ist `finalDelayMinutes` nur die Prognose zum
Ausstiegszeitpunkt, nicht die tatsächliche Endverspätung. Für saubere
Endbahnhof-Statistik daher `WHERE final_delay_is_prognosis = 0` filtern; für
Verspätung an einem konkreten Halt nach `destination` gruppieren.

Antwort `201` mit `{"hash": "…"}`. Dedup: Der Server hasht
`installId|trainType|trainNumber|origin|destination|date` (SHA-256) als
Primärschlüssel — erneutes Teilen derselben Fahrt überschreibt den Eintrag
(`INSERT OR REPLACE`), Duplikate sind unmöglich. `delayMinutes` ist bewusst
nicht Teil des Hashes, damit ein Re-Share eine korrigierte Verspätung
aktualisiert.

Limits: 20 Fahrten/Tag pro Install-ID, 60 Requests/Stunde pro IP.
Play Integrity ist bewusst noch nicht drin; ein zusätzlicher Header kann
später serverseitig geprüft werden, ohne die API-Version zu brechen.

## Dashboard: stats.iceinfo.de (öffentlich)

Der Service `dashboard` rendert Aggregate (Fahrten gesamt, Kilometer, Ø/Summe
Verspätung, Pünktlichkeit, Rekorde, häufigste/unpünktlichste Züge & Strecken)
als selbst-enthaltene HTML-Seite auf `127.0.0.1:8093`. Read-only
(`PRAGMA query_only`), aktualisiert sich alle 5 Minuten selbst.

Tunnel: Public Hostname `stats.iceinfo.de` → HTTP → `localhost:8093`.
Bewusst **ohne** Cloudflare Access (öffentliche Community-Statistik, nur
Aggregate, keine Install-IDs/persönlichen Felder). Soll es privat sein, wie bei
`data.iceinfo.de` eine Access-Application davorlegen.

## Web-Ansicht: data.iceinfo.de (Datasette + Cloudflare Access)

Datasette (Service `datasette` in der Compose-Datei) zeigt die Datenbank als
durchsuchbare Tabelle inkl. freier SQL-Abfragen, read-only, auf
`127.0.0.1:8092`.

1. **Tunnel:** Public Hostname `data.iceinfo.de` → HTTP → `localhost:8092`
   (gleicher Ablauf wie bei `api`).
2. **Login davor — Pflicht, sonst ist die Seite öffentlich:**
   Zero Trust → Access → Applications → **Add an application** → Self-hosted:
   - Domain: `data.iceinfo.de`
   - Policy: Action **Allow**, Include → **Emails** → eigene E-Mail-Adresse(n)
   - Login-Methode: One-time PIN reicht (Code kommt per Mail)

   Danach verlangt Cloudflare vor jedem Zugriff ein Login; der Container
   selbst bleibt ohne Auth.

## Auswertung

```bash
docker exec -i iceinfo-api python -c "
import sqlite3
for r in sqlite3.connect('/data/journeys.db').execute(
    'SELECT train_type||train_number, COUNT(*), AVG(delay_minutes) '
    'FROM journeys GROUP BY 1 ORDER BY 3 DESC LIMIT 20'):
    print(r)"
```

Oder die Datei `data/journeys.db` per `scp` holen und lokal mit SQLite/Pandas
auswerten.

## Backup

```bash
# als root-Cron (crontab -e), täglich 04:00:
0 4 * * * sqlite3 /opt/iceinfo-api/data/journeys.db ".backup /opt/iceinfo-api/data/backup-$(date +\%u).db"
```

Rotiert über Wochentage (7 Backups).
