# ICE Info Live — Projekt-Brain für LLMs

> Diese Datei ist der zentrale Einstiegspunkt für KI-Assistenten (Claude Code u.a.), um das
> Projekt schnell zu verstehen. Sie beschreibt Zweck, Technik, Architektur und die genutzten
> externen APIs. Bei strukturellen Änderungen am Projekt bitte hier mitpflegen.

## 1. Was ist das?

**ICE Info** ist eine native Android-App (Play Store: `com.nruge.iceinfo`), die für Reisende
in deutschen ICE-/IC-Zügen Live-Informationen anzeigt. Sie verbindet sich mit dem WLAN-internen
Bordportal des Zuges (`iceportal.de`) sowie mit öffentlichen Bahn-/Geo-APIs und zeigt u.a.:

- Live-Geschwindigkeit, nächster Halt, Verspätung, Ankunfts-/Abfahrtszeiten
- Streckenkarte (OSM) und Streckeninformationen (Tunnel, Brücken, Höchstgeschwindigkeit)
- Wagenreihung (Sektoren, Wagenfolge) und Sitzplatz-/Wagenwahl
- Bordrestaurant-Menü inkl. Bestellung am Platz
- Anschlusszüge und Abfahrtstafeln am nächsten Halt
- Bahnhofs-Services / Facilities (Aufzüge etc.)
- Wetter am Zielort
- Fahrtaufzeichnung (GPS-Track, Koordinaten auf 5 Nachkommastellen gerundet) mit GPX-Export,
  gespeicherter Fahrtenhistorie (benennbar/editierbar: Grund, Ticketart, Preis, Sitzplatz,
  Notizen, Baureihe), Gesamtkarte aller Tracks und Statistikübersicht
- Persistente Live-Benachrichtigung und Home-Screen-Widget
- Verspätungs-Crowdsourcing: einzelne gespeicherte Fahrten lassen sich **manuell** (per Button
  + Bestätigungsdialog) anonym an eine eigene Statistik-API teilen; öffentliches Dashboard unter
  `stats.iceinfo.de`. Übertragen werden nur Statistikfelder — **kein GPS-Track, keine
  persönlichen Angaben** (Preis, Sitzplatz, Notizen bleiben lokal)

Repo: https://github.com/NicoRuge/ICE-Info-Live · Entwickler: Nico Ruge

## 2. Tech-Stack

| Bereich            | Technologie |
|--------------------|-------------|
| Sprache            | Kotlin |
| UI                 | Jetpack Compose, Material 3 (Compose BOM) |
| Navigation         | Navigation-Compose |
| Architektur        | Single-Activity + MVVM (`MainViewModel` + `StateFlow`) |
| Nebenläufigkeit    | Kotlin Coroutines |
| HTTP               | Ktor Client (OkHttp-Engine) |
| JSON               | kotlinx.serialization |
| Bilder             | Coil (Compose) |
| Karten             | osmdroid (OpenStreetMap) |
| Persistenz         | DataStore Preferences |
| Widget             | Glance (AppWidget) |
| Hintergrundarbeit  | Foreground Service + WorkManager |
| Crash-Reporting    | Firebase Crashlytics (inkl. NDK) |
| Updates            | Play In-App-Updates (`app-update-ktx`) |
| Build              | Gradle (Kotlin DSL), Version Catalog (`libs.versions.toml`) |

**SDK:** `minSdk 33`, `targetSdk 36`, `compileSdk 36`. Release-Build mit R8/Minify +
Resource-Shrinking. App-Version siehe `app/build.gradle.kts` (`versionName` / `versionCode`).

**Secrets:** `DB_CLIENT_ID` / `DB_CLIENT_SECRET` (DB-Marketplace-APIs) und `STATS_API_TOKEN`
(eigene Stats-API) kommen aus `local.properties` (nicht eingecheckt) und werden via
`BuildConfig` injiziert.

## 3. Projektstruktur

Package-Root: `com.nruge.iceinfo`

```
app/src/main/
├── java/com/nruge/iceinfo/
│   ├── *Repository.kt        ← Daten-/API-Layer (je Quelle ein Repository, meist `object`)
│   ├── model/                ← @Serializable Datenmodelle (API-DTOs + Domänenmodelle)
│   ├── ui/
│   │   ├── MainViewModel.kt  ← zentraler State-Hub (alle StateFlows)
│   │   ├── AppNavigation.kt  ← NavHost / Screen-Routing
│   │   ├── Navigation.kt     ← Screen-Definitionen + Bottom-Nav-Items
│   │   ├── *Dialog.kt        ← Dialoge (Onboarding, Changelog, Settings, Record …)
│   │   ├── components/       ← Compose-Screens & UI-Bausteine
│   │   └── theme/            ← Farben, Typografie, Material-Theme
│   ├── util/                 ← HTTP-Client, Settings, Zeit-/ICE-Helfer, GPX-Export
│   ├── widget/               ← Glance-Widget (TrainWidget + Receiver + Updater)
│   ├── IceNotificationService.kt  ← Foreground Service (Live-Polling alle ~5 s)
│   ├── IceInfoApplication.kt      ← Application-Klasse
│   └── MainActivity.kt
├── assets/                   ← ice_names*.json (Zugnamen), ice_special_names.json
└── res/                      ← values (de), values-en, values-night, drawable, xml/…
```

`StatsRepository.kt` (im Package-Root wie die anderen Repositories) kapselt den POST an die
eigene Stats-API. Das Backend liegt getrennt vom App-Code im Top-Level-Verzeichnis `server/`
(siehe Abschnitt 4a).

### Screens (siehe `Navigation.kt`)
Bottom-Navigation (nur bei Zug-WLAN/Demo sichtbar): `Home` (Zug, inkl. ausklappbarer
Speisekarten-Sektion am Ende — lädt erst beim Aufklappen), `Stops` (Strecke, inkl. Karte),
`Service` (Bahnhof), `Connections` (Anschlüsse + Abfahrtstafel), `Journeys` (aufgezeichnete
Fahrten). Overlay über das Top-Bar-Menü: „Meine Fahrten" (Journeys-Overlay — bewusst auch
**offline** erreichbar, da die Bottom-Navigation ohne Zug-WLAN ausgeblendet ist).

## 4. Genutzte externe APIs

| Quelle | Basis-URL | Zweck | Auth |
|--------|-----------|-------|------|
| **ICE-Bordportal** | `http(s)://iceportal.de` | Live-Status, Trip-Info, POIs, Anschlüsse, Bordmenü, Bestellungen | keine (nur im Zug-WLAN erreichbar) |
| **bahn.de Abfahrten** | `https://www.bahn.de/web/api/reiseloesung/abfahrten` | Abfahrtstafel (Primärquelle; Zeiten lokal Europe/Berlin ohne Offset) | keine |
| **DB transport.rest** | `https://v6.db.transport.rest` | Abfahrtstafel-**Fallback** (community-gehostet, häufig Timeouts) / Stationssuche | keine (öffentlich) |
| **DB Wagenreihung** | `https://www.bahn.de/web/api/reisebegleitung/wagenreihung/vehicle-sequence` | Wagenreihung / Sektoren | keine |
| **DB API Marketplace** (StaDa + FaSta) | `https://apis.deutschebahn.com/db-api-marketplace/apis/...` | Bahnhofsdaten + Facilities (Aufzüge etc.) | `DB_CLIENT_ID` / `DB_CLIENT_SECRET` |
| **Overpass (OSM)** | `https://overpass-api.de/api/interpreter` | Gleis-/Streckenfeatures (Tunnel, Brücken, Speed) | keine |
| **Open-Meteo** | `https://api.open-meteo.com/v1/forecast` + `geocoding-api.open-meteo.com` | Wetter + Geocoding für Zielort | keine |
| **ICE Info Stats API** (eigen) | `https://api.iceinfo.de/v1` | Crowdsourcing: manuell geteilte Fahrten für Verspätungsstatistik (`StatsRepository`); Server-Code unter `server/` (FastAPI + SQLite, Hetzner hinter Cloudflare Tunnel) | `STATS_API_TOKEN` aus `local.properties` |

### ICE-Bordportal-Endpunkte (relativ zu `iceportal.de`)
- `/api1/rs/status` — Live-Status (Speed, Position, Verbindung)
- `/api1/rs/tripInfo/trip` — Fahrt-/Haltinformationen
- `/api1/rs/tripInfo/connection` — Anschlüsse
- `/api1/rs/pois/map` — Points of Interest auf der Karte
- `/bap/api/config`, `/bap/api/products`, `/bap/api/availabilities` — Bordrestaurant-Config/Menü
- `/bap/api/orders` — Bestellung aufgeben

**Wichtige Besonderheit (`util/IcePortalClient.kt`):**
`iceportal.de` ist nur im Captive-WLAN des Zuges erreichbar und hat je nach Zug-Firmware
eine selbstsignierte / unvollständige Zertifikatskette. Deshalb:
- HTTP wird **vor** HTTPS probiert (`ICE_HOSTS = ["http://iceportal.de", "https://iceportal.de"]`),
  Repositories haben einen `getWithFallback`-Mechanismus.
- Der OkHttp-Client für ICE-Hosts deaktiviert SSL-Chain-Validierung (`trustAll` + permissiver
  Hostname-Verifier). Das ist bewusst und sicher, weil es eine bekannte lokale Appliance ist —
  **nicht** auf öffentliche Hosts ausweiten.
- Ein geteilter `CookieJar` hält Session-Cookies, damit Menü-Abruf und Bestellung dieselbe
  Session nutzen.

### 4a. Eigenes Backend (`server/`)

Kleines, eigenständiges Backend für das Verspätungs-Crowdsourcing (nicht Teil des App-Builds).
Läuft auf einem Hetzner-Server als Docker-Container hinter einem **Cloudflare Tunnel**
(kein offener Port, HTTPS via Cloudflare). Drei Subdomains:

| Domain | Dienst | Zweck | Zugriff |
|--------|--------|-------|---------|
| `api.iceinfo.de/v1` | FastAPI (`server/main.py`) | Schreib-API: nimmt geteilte Fahrten an (`POST /v1/journeys`, Bearer-Token) | Token (`STATS_API_TOKEN`) |
| `stats.iceinfo.de` | FastAPI (`server/dashboard.py`) | Öffentliches Statistik-Dashboard (selbst-enthaltenes HTML, read-only) | öffentlich |
| `data.iceinfo.de` | Datasette | Rohdaten-Ansicht der SQLite-DB | Cloudflare Access (Login) |

- **Persistenz:** eine SQLite-Datei (`/data/journeys.db`) im WAL-Modus, geteilt über ein
  Docker-Volume. Die API schreibt (`INSERT OR REPLACE`), Dashboard/Datasette lesen nur
  (Dashboard mit `PRAGMA query_only`; eine reine `mode=ro`-Verbindung kann WAL aus einem
  zweiten Prozess **nicht** lesen — Volume daher nicht `:ro` mounten).
- **Dedup:** Der **Server** bildet den Hash `installId|trainType|trainNumber|origin|destination|date`
  (SHA-256) als Primärschlüssel. Gleiche Fahrt derselben Installation → Overwrite (kein Duplikat);
  verschiedene Nutzer im selben Zug → verschiedene Zeilen (bewusst, zwei Messungen).
  `delayMinutes` ist **nicht** Teil des Hashes (Re-Share aktualisiert den Wert).
- **Ausstieg vs. Endbahnhof:** `delayMinutes` ist am Ausstiegshalt des Nutzers gemessen;
  `finalDelayMinutes`/`finalStation` am Zug-Endbahnhof. `finalDelayIsPrognosis = true` heißt,
  der Nutzer stieg vorher aus → der Endwert ist nur die Prognose zum Ausstiegszeitpunkt.
- **Deployment:** `docker compose up -d --build` im `server/`-Verzeichnis; Token in `server/.env`.
  Details + Cloudflare-Schritte in `server/README.md`.

Das DTO auf App-Seite (`StatsRepository.SharedJourneyDto`) und das Pydantic-Modell in
`server/main.py` müssen feldkompatibel bleiben. Neue Felder additiv halten (optionale
Pydantic-Felder + `ALTER TABLE`-Migration in `_init_db`), damit alte App-Versionen weiter posten.

## 5. Architektur-/Code-Konventionen

- **Repositories** sind meist Kotlin-`object`-Singletons mit `suspend`-Funktionen, die auf
  `Dispatchers.IO` laufen. Jede Datenquelle hat ihr eigenes Repository.
- **Ktor/JSON** überall mit `Json { ignoreUnknownKeys = true; coerceInputValues = true }` —
  tolerant gegenüber unvollständigen/wechselnden API-Antworten.
- **State** fließt ausschließlich über `MainViewModel`-`StateFlow`s in die Compose-UI; UI ist
  zustandslos und liest per `collectAsState`.
- **Lokalisierung:** Strings in `res/values/strings.xml` (DE) + `res/values-en/strings.xml`.
  Keine hartkodierten User-facing-Strings in Compose.
- **Demo-/Mock-Modus:** `DemoDaten.kt` + `_isMockMode` erlauben Testen ohne echtes Zug-WLAN.
- **Persistenz:** Einstellungen über `SettingsManager` (DataStore); gespeicherte Fahrten über
  `JourneyRepository` (DataStore, JSON-serialisiert).

## 6. Hintergrund-Features

- **`IceNotificationService`** — Foreground Service (`specialUse`), pollt den Bordstatus
  ~alle 5 s und zeigt eine persistente Live-Benachrichtigung (Speed, nächster Halt, Ankunft).
- **Widget** (`widget/`) — Glance-App-Widget mit Zug-Status, aktualisiert via `WidgetUpdater`.
- **Fahrtaufzeichnung** — zeichnet GPS-Track + Live-Daten auf, exportiert als GPX
  (`util/GpxExporter.kt`), speichert Historie (`JourneyRepository` / `SavedJourney`).

## 7. Build & Run

```bash
./gradlew assembleDebug        # Debug-APK bauen
./gradlew installDebug         # auf verbundenes Gerät/Emulator installieren
./gradlew assembleRelease      # R8-minifizierter Release-Build (lokal mit Debug-Key signiert)
./gradlew test                 # Unit-Tests
./gradlew connectedAndroidTest # Instrumented Tests (Gerät nötig)
```

Vor dem Release-Build muss `local.properties` die `DB_CLIENT_ID`/`DB_CLIENT_SECRET` enthalten,
sonst bleiben die DB-Marketplace-Features (Facilities) leer. Ohne `STATS_API_TOKEN` scheitert
zudem das Fahrten-Teilen (API antwortet mit 401).

## 8. Hinweise für Änderungen

- Neue Datenquelle? → eigenes `*Repository` (object) + Modelle in `model/` + Verdrahtung im
  `MainViewModel` (neuer `StateFlow`).
- Neue UI? → Composable in `ui/components/`, Route in `Navigation.kt`/`AppNavigation.kt`,
  Strings in **beiden** `strings.xml`.
- ICE-Portal-Requests immer über `buildIceHttpClient(...)` + `ICE_HOSTS`-Fallback laufen lassen,
  nicht direkt mit einem Standard-Client (sonst scheitert SSL im Zug).
- Neues Feld an die Stats-API? → in **beiden** Seiten additiv ergänzen: `SharedJourneyDto`
  (`StatsRepository.kt`) **und** Pydantic-Modell + Spalte + `ALTER TABLE`-Migration + INSERT in
  `server/main.py`; Dashboard-Aggregat bei Bedarf in `server/dashboard.py`. Alte App-Versionen
  dürfen das Feld weglassen (optional mit Default).
