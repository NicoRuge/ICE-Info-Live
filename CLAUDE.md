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
- Fahrtaufzeichnung (GPS-Track) mit GPX-Export und gespeicherter Fahrtenhistorie
- Persistente Live-Benachrichtigung und Home-Screen-Widget

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

**Secrets:** `DB_CLIENT_ID` / `DB_CLIENT_SECRET` für die DB-Marketplace-APIs kommen aus
`local.properties` (nicht eingecheckt) und werden via `BuildConfig` injiziert.

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

### Screens (Bottom-Navigation, siehe `Navigation.kt`)
`Home` (Status), `Stops`/Journey (Halte), `Map`, `Service` (Bahnhof), `Connections`
(Anschlüsse), `Menu` (Bordrestaurant), `Journeys` (aufgezeichnete Fahrten).

## 4. Genutzte externe APIs

| Quelle | Basis-URL | Zweck | Auth |
|--------|-----------|-------|------|
| **ICE-Bordportal** | `http(s)://iceportal.de` | Live-Status, Trip-Info, POIs, Anschlüsse, Bordmenü, Bestellungen | keine (nur im Zug-WLAN erreichbar) |
| **DB transport.rest** | `https://v6.db.transport.rest` | Abfahrtstafeln / Stationssuche | keine (öffentlich) |
| **DB Wagenreihung** | `https://www.bahn.de/web/api/reisebegleitung/wagenreihung/vehicle-sequence` | Wagenreihung / Sektoren | keine |
| **DB API Marketplace** (StaDa + FaSta) | `https://apis.deutschebahn.com/db-api-marketplace/apis/...` | Bahnhofsdaten + Facilities (Aufzüge etc.) | `DB_CLIENT_ID` / `DB_CLIENT_SECRET` |
| **Overpass (OSM)** | `https://overpass-api.de/api/interpreter` | Gleis-/Streckenfeatures (Tunnel, Brücken, Speed) | keine |
| **Open-Meteo** | `https://api.open-meteo.com/v1/forecast` + `geocoding-api.open-meteo.com` | Wetter + Geocoding für Zielort | keine |

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
sonst bleiben die DB-Marketplace-Features (Facilities) leer.

## 8. Hinweise für Änderungen

- Neue Datenquelle? → eigenes `*Repository` (object) + Modelle in `model/` + Verdrahtung im
  `MainViewModel` (neuer `StateFlow`).
- Neue UI? → Composable in `ui/components/`, Route in `Navigation.kt`/`AppNavigation.kt`,
  Strings in **beiden** `strings.xml`.
- ICE-Portal-Requests immer über `buildIceHttpClient(...)` + `ICE_HOSTS`-Fallback laufen lassen,
  nicht direkt mit einem Standard-Client (sonst scheitert SSL im Zug).
