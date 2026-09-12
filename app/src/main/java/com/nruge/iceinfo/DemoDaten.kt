package com.nruge.iceinfo

import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.model.MenuCategory
import com.nruge.iceinfo.model.MenuItem
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.model.TrackPoint

val sampleTrainStatus = TrainStatus(
    distanceLastToNext = 120000,
    trainType = "ICE",
    trainNumber = "212",
    speed = 114,
    nextStop = "Hannover Hbf",
    destination = "München Hbf",
    eta = "09:34",
    delayMinutes = 3,
    tzn = "ICE0304",
    series = "408",
    track = "4",
    trackChanged = true,
    delayReason = "Kurzfristiger Personalausfall",
    distanceToNext = 86760,
    wagonClass = "FIRST",
    connectivity = "STRONG",
    nextConnectivity = "MIDDLE",
    connectivityRemainingSeconds = 600,
    latitude = 51.4825,
    longitude = 11.9906,
    stops = listOf(
        // Startbahnhof — kein Delay, nur Abfahrt
        TrainStop(
            name = "Hamburg-Altona", evaNr = "8002545",
            scheduledArrival = "", actualArrival = "", delayMinutes = 0,
            track = "12", passed = true, isNext = false, distanceFromStart = 0,
            scheduledDeparture = "08:13", actualDeparture = "08:13", departureDelayMinutes = 0
        ),
        // 3 min Delay → grün
        TrainStop(
            name = "Hamburg Hbf", evaNr = "8002549",
            scheduledArrival = "08:24", actualArrival = "08:27", delayMinutes = 3,
            track = "14", passed = true, isNext = false, distanceFromStart = 25000,
            scheduledDeparture = "08:26", actualDeparture = "08:29", departureDelayMinutes = 3
        ),
        // Ausgefallener Halt
        TrainStop(
            name = "Hannover-Linden", evaNr = "8000226",
            scheduledArrival = "09:18", actualArrival = "09:18", delayMinutes = 3,
            track = "2", passed = false, isNext = false, distanceFromStart = 140000,
            scheduledDeparture = "09:20", actualDeparture = "09:20", departureDelayMinutes = 3,
            isCancelled = true
        ),
        // Nächster Halt — 3 min Delay → grün
        TrainStop(
            name = "Hannover Hbf", evaNr = "8000152",
            scheduledArrival = "09:31", actualArrival = "09:34", delayMinutes = 3,
            track = "4", trackChanged = true, passed = false, isNext = true, distanceFromStart = 150000,
            scheduledDeparture = "09:33", actualDeparture = "09:36", departureDelayMinutes = 3
        ),
        // -5 min → überpünktlich → Rainbow 🌈
        TrainStop(
            name = "Göttingen", evaNr = "8000128",
            scheduledArrival = "10:15", actualArrival = "10:10", delayMinutes = -5,
            track = "10", passed = false, isNext = false, distanceFromStart = 250000,
            scheduledDeparture = "10:17", actualDeparture = "10:17", departureDelayMinutes = 0
        ),
        // 4 min Delay → grün (Grenzfall)
        TrainStop(
            name = "Kassel-Wilhelmshöhe", evaNr = "8003197",
            scheduledArrival = "10:35", actualArrival = "10:39", delayMinutes = 4,
            track = "2", passed = false, isNext = false, distanceFromStart = 300000,
            scheduledDeparture = "10:37", actualDeparture = "10:41", departureDelayMinutes = 4
        ),
        // Zusatzhalt — 4 min Delay → grün
        TrainStop(
            name = "Fulda", evaNr = "8000052",
            scheduledArrival = "11:12", actualArrival = "11:16", delayMinutes = 4,
            track = "3", passed = false, isNext = false, distanceFromStart = 400000, isAdditional = true,
            scheduledDeparture = "11:14", actualDeparture = "11:18", departureDelayMinutes = 4
        ),
        // 7 min Delay → rot; zeigt außerdem den Fahrtrichtungswechsel-Indikator
        TrainStop(
            name = "Würzburg Hbf", evaNr = "8000260",
            scheduledArrival = "11:58", actualArrival = "12:05", delayMinutes = 7,
            track = "6", passed = false, isNext = false, distanceFromStart = 500000,
            scheduledDeparture = "12:01", actualDeparture = "12:08", departureDelayMinutes = 7,
            directionChange = true
        ),
        // 12 min Delay → rot
        TrainStop(
            name = "Nürnberg Hbf", evaNr = "8000284",
            scheduledArrival = "12:54", actualArrival = "13:06", delayMinutes = 12,
            track = "9", passed = false, isNext = false, distanceFromStart = 600000,
            scheduledDeparture = "12:57", actualDeparture = "13:09", departureDelayMinutes = 12
        ),
        // Endbahnhof — 21 min Delay → rot, nur Ankunft
        TrainStop(
            name = "München Hbf", evaNr = "8000261",
            scheduledArrival = "14:11", actualArrival = "14:32", delayMinutes = 21,
            track = "18", passed = false, isNext = false, distanceFromStart = 800000,
            scheduledDeparture = "", actualDeparture = "", departureDelayMinutes = 0
        ),
    ),
    destinationEta = "14:11",
    destinationTrack = "6",
    destinationDelay = 21,
    distanceToDestination = 467000,
    actualPosition = 130000
)

// Echte Wagenreihung ICE 10 (ICE 3 Neo, Baureihe 408) Hamburg–Basel
val sampleCoaches = listOf(
    Coach(
        coachNumber = 21,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "CONTROLCAR_ECONOMY_CLASS",
        sector = "A",
        amenities = setOf("BIKE_SPACE", "WHEELCHAIR_SPACE")
    ),
    Coach(
        coachNumber = 22,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "PASSENGERCARRIAGE_ECONOMY_CLASS",
        sector = "A",
        amenities = setOf("ZONE_QUIET")
    ),
    Coach(
        coachNumber = 23,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "PASSENGERCARRIAGE_ECONOMY_CLASS",
        sector = "B",
        amenities = setOf("ZONE_FAMILY", "CABIN_INFANT")
    ),
    Coach(
        coachNumber = 0,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "PASSENGERCARRIAGE_ECONOMY_CLASS",
        sector = "B",
        amenities = emptySet(),
        isClosed = true
    ),
    Coach(
        coachNumber = 24,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "PASSENGERCARRIAGE_ECONOMY_CLASS",
        sector = "B",
        amenities = emptySet()
    ),
    Coach(
        coachNumber = 25,
        hasFirstClass = false, hasSecondClass = true,
        vehicleCategory = "HALFDININGCAR_ECONOMY_CLASS",
        sector = "B",
        amenities = emptySet()
    ),
    Coach(
        coachNumber = 26,
        hasFirstClass = true, hasSecondClass = false,
        vehicleCategory = "PASSENGERCARRIAGE_FIRST_CLASS",
        sector = "C",
        amenities = setOf("SEATS_BAHN_COMFORT")
    ),
    Coach(
        coachNumber = 27,
        hasFirstClass = true, hasSecondClass = false,
        vehicleCategory = "PASSENGERCARRIAGE_FIRST_CLASS",
        sector = "C",
        amenities = setOf("SEATS_SEVERELY_DISABLED")
    ),
    Coach(
        coachNumber = 28,
        hasFirstClass = true, hasSecondClass = false,
        vehicleCategory = "CONTROLCAR_FIRST_CLASS",
        sector = "C",
        amenities = setOf("SEATS_BAHN_COMFORT", "ZONE_QUIET")
    ),
)

val samplePois = listOf(
    PoiItem(
        name = "Erfurter Dom",
        type = "MONUMENT",
        distance = 2500,
        latitude = 51.4825,
        longitude = 11.9906,
        description = "Gotischer Dom aus dem 14. Jahrhundert"
    ),
    PoiItem(
        name = "Saale",
        type = "RIVER",
        distance = 5000,
        latitude = 51.4825,
        longitude = 11.9906,
        description = "Nebenfluss der Elbe"
    ),
    PoiItem(
        name = "Halle (Saale)",
        type = "CITY",
        distance = 8000,
        latitude = 51.4825,
        longitude = 11.9700,
        description = "Größte Stadt Sachsen-Anhalts"
    ),
    PoiItem(
        name = "Petersberg",
        type = "MOUNTAIN",
        distance = 12000,
        latitude = 51.5500,
        longitude = 11.9500,
        description = "234 m hoher Tafelberg"
    ),
    PoiItem(
        name = "Concordiasee",
        type = "LAKE",
        distance = 15000,
        latitude = 51.6000,
        longitude = 11.8500,
        description = "Ehemaliger Braunkohletagebau"
    )
)

val sampleConnections = listOf(
    ConnectingTrain(
        trainType = "ICE",
        trainNumber = "598",
        destination = "Berlin Hbf",
        departure = "10:24",
        track = "7",
        delayMinutes = 0,
        reachable = true,
        transferMinutes = 18
    ),
    ConnectingTrain(
        trainType = "IC",
        trainNumber = "2045",
        destination = "Frankfurt (Main) Hbf",
        departure = "10:31",
        track = "3",
        trackChanged = true,
        delayMinutes = 5,
        reachable = true,
        transferMinutes = 3
    ),
    ConnectingTrain(
        trainType = "RE",
        trainNumber = "3",
        destination = "Hannover Hbf",
        departure = "10:18",
        track = "12",
        delayMinutes = 0,
        reachable = false,
        transferMinutes = null
    ),
    ConnectingTrain(
        trainType = "ICE",
        trainNumber = "1077",
        destination = "München Hbf",
        departure = "10:47",
        track = "5",
        delayMinutes = 12,
        reachable = true,
        transferMinutes = 25
    ),
    ConnectingTrain(
        trainType = "RB",
        trainNumber = "87",
        destination = "Bebra",
        departure = "10:55",
        track = "1",
        delayMinutes = 0,
        reachable = true,
        transferMinutes = null
    )
)

val sampleWeather = WeatherInfo(
    stationName = "München Hbf",
    temperature = 14.0,
    precipitation = 0.0,
    windspeed = 18.0,
    weatherCode = 2
)

val sampleOsmTrackData = OsmTrackData(
    trackInfo = TrackInfo(
        maxSpeed = 280,
        electrified = "contact_line",
        voltage = 15000,
        tracks = 2,
        usage = "main"
    ),
    features = listOf(
        RailFeature("Göttingen", RailFeatureType.STATION, 8.3),
        RailFeature("Mühlbergtunnel", RailFeatureType.TUNNEL, 14.2),
        RailFeature("Kassel-Wilhelmshöhe", RailFeatureType.STATION, 47.8),
        RailFeature("Sinntalviadukt", RailFeatureType.BRIDGE, 73.1),
        RailFeature("Landrückentunnel", RailFeatureType.TUNNEL, 78.4),
        RailFeature("Fulda", RailFeatureType.STATION, 91.6),
        RailFeature("Würzburg Hbf", RailFeatureType.STATION, 156.2)
    )
)

val sampleDepartures = listOf(
    Departure(line = "ICE 372", destination = "Basel SBB", scheduledTime = "11:08", delayMinutes = 0, platform = "9"),
    Departure(line = "RE 7", destination = "Kassel-Wilhelmshöhe", scheduledTime = "11:14", delayMinutes = 3, platform = "4"),
    Departure(line = "S 1", destination = "Hannover-Bismarckstr.", scheduledTime = "11:17", delayMinutes = 0, platform = "2"),
    Departure(line = "ICE 1075", destination = "München Hbf", scheduledTime = "11:23", delayMinutes = 12, platform = "5", platformChanged = true),
    Departure(line = "IC 2027", destination = "Köln Hbf", scheduledTime = "11:29", delayMinutes = 0, platform = "7", cancelled = true),
    Departure(line = "RB 87", destination = "Bebra", scheduledTime = "11:36", delayMinutes = 0, platform = "1"),
    Departure(line = "ICE 884", destination = "Hamburg-Altona", scheduledTime = "11:42", delayMinutes = 5, platform = "8")
)

// ---------------------------------------------------------------------------
// Demo-Fahrten für die Fahrten-History
// ---------------------------------------------------------------------------

/** Schlüssel-Wegpunkte entlang der Strecke Hamburg-Altona → München Hbf
 *  (NBS Hannover–Würzburg, dann Würzburg–Nürnberg–München via NBS).
 *  lat, lon, speedKmh, secondsFromStart */
private data class Wp(val lat: Double, val lon: Double, val spd: Int, val sec: Int)

private val hamburgMuenchenWaypoints = listOf(
    Wp(53.5677,  9.9364,   0,     0),   // Hamburg-Altona – dep 08:13
    Wp(53.5532, 10.0056,  60,   840),   // Hamburg Hbf – arr 08:27
    Wp(53.5532, 10.0056,   0,   960),   // Hamburg Hbf – dep 08:29
    Wp(53.4562,  9.9926, 180,  1500),   // Harburg
    Wp(53.3100, 10.2200, 250,  2100),   // Lüneburg-Süd
    Wp(52.9648, 10.5645, 250,  3100),   // Uelzen
    Wp(52.7500, 10.3200, 250,  3900),   // Lehrte
    Wp(52.6206, 10.0848, 250,  4400),   // Celle
    Wp(52.3766,  9.7415, 100,  4860),   // Hannover Hbf – arr 09:34
    Wp(52.3766,  9.7415,   0,  4980),   // Hannover Hbf – dep 09:36
    Wp(52.1900,  9.8200, 280,  5600),   // NBS Hannover-Würzburg Nordabschnitt
    Wp(51.9700,  9.8500, 280,  6200),   // NBS – Kreiensen-Bereich
    Wp(51.7500,  9.9700, 280,  6700),   // NBS – Northeim-Bereich
    Wp(51.5368,  9.9268, 100,  7020),   // Göttingen – arr 10:10
    Wp(51.5368,  9.9268,   0,  7440),   // Göttingen – dep 10:17
    Wp(51.4200,  9.7500, 250,  7800),   // NBS südlich Göttingen
    Wp(51.3149,  9.4416, 100,  8760),   // Kassel-Wilhelmshöhe – arr 10:39
    Wp(51.3149,  9.4416,   0,  8880),   // Kassel-Wilhelmshöhe – dep 10:41
    Wp(51.1500,  9.6200, 280,  9500),   // NBS – Bebraer Kurve
    Wp(50.8680,  9.7060, 280, 10200),   // NBS – Bad Hersfeld
    Wp(50.6600,  9.7200, 280, 10700),   // NBS – Lauterbach-Tunnel
    Wp(50.5548,  9.6836, 100, 10980),   // Fulda – arr 11:16
    Wp(50.5548,  9.6836,   0, 11100),   // Fulda – dep 11:18
    Wp(50.3700,  9.8000, 250, 11750),   // NBS – Schlüchterner Tunnel
    Wp(50.1500,  9.9200, 250, 12400),   // NBS – Jossa-Bereich
    Wp(49.9800,  9.9600, 200, 13100),   // Nähe Gemünden
    Wp(49.8023,  9.9358, 100, 13920),   // Würzburg Hbf – arr 12:05
    Wp(49.8023,  9.9358,   0, 14100),   // Würzburg Hbf – dep 12:08
    Wp(49.6500, 10.1500, 160, 14700),   // Nähe Kitzingen
    Wp(49.5500, 10.5000, 160, 15400),   // Nähe Neustadt/Aisch
    Wp(49.4980, 10.7500, 160, 16000),   // Nähe Ansbach-Nord
    Wp(49.4454, 11.0825, 100, 17580),   // Nürnberg Hbf – arr 13:06
    Wp(49.4454, 11.0825,   0, 17760),   // Nürnberg Hbf – dep 13:09
    Wp(49.2900, 11.2500, 220, 18450),   // NBS Nürnberg-München – Feucht
    Wp(49.0300, 11.3500, 300, 19200),   // NBS – Ingolstadt-Bereich
    Wp(48.7600, 11.4200, 300, 20100),   // NBS – Pfaffenhofen
    Wp(48.5600, 11.5000, 280, 20900),   // NBS – Freising-Nord
    Wp(48.4000, 11.5600, 200, 21700),   // Nähe Freising
    Wp(48.2600, 11.5500, 160, 22200),   // Nordeinfahrt München
    Wp(48.1402, 11.5581,  40, 22680),   // München Hbf – einfahrend
    Wp(48.1402, 11.5581,   0, 22740),   // München Hbf – arr 14:32
)

private fun buildTrackPoints(waypoints: List<Wp>, intervalSec: Int = 30): List<TrackPoint> {
    if (waypoints.size < 2) return emptyList()
    val points = mutableListOf<TrackPoint>()
    val totalSec = waypoints.last().sec
    var t = 0
    while (t <= totalSec) {
        // Finde das aktuelle Segment
        val idx = waypoints.indexOfLast { it.sec <= t }.coerceAtLeast(0)
        val from = waypoints[idx]
        val to = waypoints.getOrNull(idx + 1) ?: from
        val frac = if (to.sec == from.sec) 1.0
                   else (t - from.sec).toDouble() / (to.sec - from.sec)
        points.add(
            TrackPoint(
                lat = from.lat + (to.lat - from.lat) * frac,
                lon = from.lon + (to.lon - from.lon) * frac,
                speedKmh = (from.spd + (to.spd - from.spd) * frac).toInt(),
                secondsFromStart = t
            )
        )
        t += intervalSec
    }
    return points
}

val sampleJourneys: List<SavedJourney> = listOf(
    // Fahrt 1 – ICE 212 Hamburg → München, MIT GPS-Spur
    SavedJourney(
        id = "demo-ice212-hh-muc",
        trainType = "ICE",
        trainNumber = "212",
        originStation = "Hamburg-Altona",
        destinationStation = "München Hbf",
        date = "12.05.2025",
        departureTime = "08:13",
        arrivalTime = "14:32",
        delayMinutes = 21,
        distanceKm = 778,
        topSpeedKmh = 300,
        avgSpeedKmh = 187,
        durationMinutes = 379,
        stopsCount = 7,
        recordedGps = true,
        trackPoints = buildTrackPoints(hamburgMuenchenWaypoints, intervalSec = 30),
        tzn = "ICE8007",
        series = "408"
    ),
    // Fahrt 2 – ICE 599 Frankfurt → Berlin, OHNE GPS
    SavedJourney(
        id = "demo-ice599-fra-ber",
        trainType = "ICE",
        trainNumber = "599",
        originStation = "Frankfurt (Main) Hbf",
        destinationStation = "Berlin Hbf",
        date = "28.04.2025",
        departureTime = "09:55",
        arrivalTime = "13:48",
        delayMinutes = 0,
        distanceKm = 546,
        topSpeedKmh = 280,
        avgSpeedKmh = 210,
        durationMinutes = 233,
        stopsCount = 3,
        recordedGps = false,
        trackPoints = emptyList(),
        tzn = "ICE9046",
        series = "412"
    ),
    // Fahrt 3 – ICE 77 Köln → München, OHNE GPS, stark verspätet
    SavedJourney(
        id = "demo-ice77-cgn-muc",
        trainType = "ICE",
        trainNumber = "77",
        originStation = "Köln Hbf",
        destinationStation = "München Hbf",
        date = "03.04.2025",
        departureTime = "07:02",
        arrivalTime = "11:58",
        delayMinutes = 34,
        distanceKm = 611,
        topSpeedKmh = 270,
        avgSpeedKmh = 163,
        durationMinutes = 296,
        stopsCount = 4,
        recordedGps = false,
        trackPoints = emptyList(),
        tzn = "ICE0304",
        series = "403"
    )
)
private fun menuItem(id: Int, title: String, subject: String = "", imgPath: String, eurPrice: Double, @Suppress("UNUSED_PARAMETER") chfPrice: Double, decls: List<String> = emptyList(), visible: Boolean = true) = MenuItem(
    id = id, title = title, subject = subject,
    imageUrl = imgPath,
    eurPrice = eurPrice,
    declarationKeys = decls,
    visible = visible
)

private val IMG = "img/grains/Sites/ICE-Portal/Germany/de/Release_2.0/Startseite_3.0/BaP_-_Bestellen_am_Platz/Speisekarte"

val sampleMenuCategories = listOf(
    MenuCategory("Aktion", listOf(
        menuItem(9000000, "Burger mit Beef-Bacon-Zwiebel-Chutney & Pommes", "dazu Ketchup oder Mayo", "$IMG/00_Aktion/2026_02-Februar/Burger__Pommes/Bild_Burger_mit_Beef-Bacon-Zwiebel-Chutney__Pommes.data.jpg", 14.5, 17.4, listOf("1","2","5","9")),
        menuItem(9000001, "Burger", "mit Beef-Bacon-Zwiebel-Chutney", "$IMG/00_Aktion/2026_02-Februar/Burger/Bild_Burger.data.jpg", 10.9, 13.1, listOf("1","2","5","9")),
        menuItem(9000002, "Orangina Original", "Orangenlimonade, 0,25 l Flasche", "$IMG/00_Aktion/2026_02-Februar/Orangina/Bild_Orangina_Original.data.jpg", 4.2, 5.0),
        menuItem(9000003, "Peroni Nastro Azzurro", "Lagerbier, 0,33 l Flasche", "$IMG/00_Aktion/2026_02-Februar/Peroni_Nastro_Azzurro/Bild_Peroni_Nastro_Azzurro.data.jpg", 4.2, 5.0),
    )),
    MenuCategory("Snacks", listOf(
        menuItem(9000004, "Sandwich Chicken Caesar Style", "mit Pulled Chicken, italienischem Hartkäse, Frühlingszwiebeln & Tomaten", "$IMG/03_Snacks/Sandwich_Chicken_Caesar_Style/Bild_Sandwich_Chicken_Caesar_Style.data.jpg", 7.9, 9.5),
        menuItem(9000005, "Pizza Mozza-Bella Deluxe", "mit Kirschtomaten & Mozzarella", "$IMG/00_Aktion/2026_02-Februar/Pizza_Mozza-Bella_Deluxe/Bild_Pizza_Mozza-Bella_Deluxe.data.jpg", 6.9, 8.3),
        menuItem(9000006, "Pizza O Sala Mio Premium", "mit Salami & Käse", "$IMG/00_Aktion/2026_02-Februar/Pizza_O_Sala_Mio_Premium/Bild_Pizza_O_Sala_Mio_Premium.data.jpg", 6.9, 8.3),
        menuItem(9000007, "Vegetarisches Vollkorn-Haferbrot", "belegt mit Bergkäse, Kräuterfrischkäse und eingelegten Zwiebeln", "$IMG/03_Snacks/Vegetarisches_Vollkorn-Haferbrot/Bild_Vegetarisches_Vollkorn-Haferbrot.data.jpg", 6.5, 7.8, listOf("1")),
        menuItem(9000028, "Vegane Currywurst mit BIO Brötchen", "& Tortilla-Crunch", "$IMG/03_Snacks/Vegane_Currywurst/Bild_Vegane_Currywurst.data.jpg", 7.9, 9.5, visible = false),
    )),
    MenuCategory("Hauptgerichte", listOf(
        menuItem(9000008, "Hähnchen Tikka Masala", "mit Basmatireis", "$IMG/01_Hauptgerichte/Haehnchen_Tikka_Masala/Bild_Haehnchen_Tikka_Masala.data.jpg", 14.9, 17.9),
        menuItem(9000009, "Rotes Thai Curry mit Shiitake-Pilzen", "Gemüse & Basmatireis", "$IMG/01_Hauptgerichte/Rotes_Thai_Curry_mit_Shiitake-Pilze/Bild_Rotes_Thai_Curry_mit_Shiitake-Pilze.data.jpg", 13.9, 16.7, listOf("1")),
        menuItem(9000010, "Chili con Carne", "vom Rind, mit Sour Cream & BIO Brötchen", "$IMG/01_Hauptgerichte/Chili_con_Carne/Bild_Chili_con_Carne.data.jpg", 12.5, 15.0, listOf("2")),
        menuItem(9000011, "Veganes Chili sin Carne", "mit BIO Brötchen", "$IMG/01_Hauptgerichte/Veganes_Chili_sin_Carne/Bild_Veganes_Chili_sin_Carne.data.jpg", 12.5, 15.0),
    )),
    MenuCategory("Frühstück", listOf(
        menuItem(9000012, "Egg Drop Sandwich", "mit Putenbacon, Cheddar & Sriracha Sauce", "$IMG/00_Aktion/2025_05_Mai/Egg_Drop_Sandwich/Bild_Egg_Drop_Sandwich.data.jpg", 7.2, 8.6, listOf("1","5","9")),
        menuItem(9000013, "Französisch", "Croissant, 2 BIO Brötchen, Butter, Bionella, Honig & Konfitüre", "$IMG/05_Fruehstueck/Franzoesisch/Bild_Franzoesisch.data.jpg", 7.9, 9.5),
        menuItem(9000014, "Warmes Croissant Brötchen mit Gouda", "Tomaten, Zwiebeln und Gurkenaufstrich", "$IMG/05_Fruehstueck/Croissant_Broetchen_Kaese/Bild_Croissant_Broetchen_Kaese.data.jpg", 6.9, 8.3, listOf("4")),
        menuItem(9000015, "Zimtschnecke", "90g", "$IMG/06_Suess__Salzig/Zimtschnecke/Bild_Zimtschnecke.data.jpg", 4.2, 5.0, visible = false),
    )),
    MenuCategory("Süß & salzig", listOf(
        menuItem(9000016, "Apfelkuchen", "mit Butterstreusel", "$IMG/06_Suess__Salzig/Apfelkuchen/Bild_Apfelkuchen.data.jpg", 4.8, 5.8),
        menuItem(9000017, "Zimtschnecke", "90g", "$IMG/06_Suess__Salzig/Zimtschnecke/Bild_Zimtschnecke.data.jpg", 4.2, 5.0),
        menuItem(9000018, "Tony's Chocolonely Vollmilchschokolade", "mit Karamell & Meersalz, 47g", "$IMG/06_Suess__Salzig/Tonys_Chocolonely_Vollmilchschokolade/Bild_Tonys_Chocolonely_Vollmilchschokolade.data.jpg", 2.9, 3.5),
        menuItem(9000019, "Vegane Treets Erdnüsse mit ChoViva", "100 g", "$IMG/06_Suess__Salzig/Vegane_Treets_Erdnuesse_mit_ChoViva/Bild_Vegane_Treets_Erdnuesse_mit_ChoViva.data.jpg", 3.6, 4.3, listOf("1","8")),
    )),
    MenuCategory("Suppe", listOf(
        menuItem(9000020, "Vegane Tomatensuppe", "mit BIO Brötchen", "$IMG/04_Suppen__Salate/Vegane_Tomatensuppe/Bild_Tomatensuppe.data.jpg", 7.9, 9.5),
    )),
    MenuCategory("Kindermenü", listOf(
        menuItem(9000021, "Kindermenü: Haferkater Porridge", "Apfel-Zimt, Getränk, Süßigkeit, Smoothie & Spielzeug", "$IMG/02_Kindermenue/Kindermenue_bis_14_J._Porridge_Apfel-Zimt/Bild_NEU_Kindermenue_bis_14_J._Porridge_Apfel-Zimt.data.jpg", 7.9, 9.5),
        menuItem(9000022, "Kindermenü: Vegane Gemüsebolognese", "Getränk, Süßigkeit, Smoothie & Spielzeug", "$IMG/02_Kindermenue/Kindermenue_bis_14_J._Vegane_Gemuesebolognese/Bild_Kindermenue_NEU_bis_14_J._Vegane_Gemuesebolognese.data.jpg", 9.9, 11.9),
        menuItem(9000023, "Kindermenü: Pommes frites", "Getränk, Süßigkeit, Smoothie & Spielzeug", "$IMG/02_Kindermenue/Kindermenue_bis_14_J._Pommes_frites/NEU_Bild_Kindermenue_Pommes_16-5.data.jpg", 7.9, 9.5),
        menuItem(9000024, "BIO MOGLi Quetschie", "Erdbeere mit Apfel & Banane 100g", "$IMG/02_Kindermenue/BIO_MOGLi_Quetschie/Bild_BIO_MOGLi_Quetschie.data.jpg", 3.2, 3.8),
    )),
    MenuCategory("Vegetarisch", listOf(
        menuItem(9000025, "Vegetarisches Vollkorn-Haferbrot", "belegt mit Bergkäse, Kräuterfrischkäse und eingelegten Zwiebeln", "$IMG/03_Snacks/Vegetarisches_Vollkorn-Haferbrot/Bild_Vegetarisches_Vollkorn-Haferbrot.data.jpg", 6.5, 7.8, listOf("1")),
        menuItem(9000026, "Pizza Mozza-Bella Deluxe", "mit Kirschtomaten & Mozzarella", "$IMG/00_Aktion/2026_02-Februar/Pizza_Mozza-Bella_Deluxe/Bild_Pizza_Mozza-Bella_Deluxe.data.jpg", 6.9, 8.3),
        menuItem(9000027, "Warmes Croissant Brötchen mit Gouda", "Tomaten, Zwiebeln und Gurkenaufstrich", "$IMG/05_Fruehstueck/Croissant_Broetchen_Kaese/Bild_Croissant_Broetchen_Kaese.data.jpg", 6.9, 8.3, listOf("4")),
    )),
    MenuCategory("Vegan", listOf(
        menuItem(9000029, "Vegane Tomatensuppe", "mit BIO Brötchen", "$IMG/04_Suppen__Salate/Vegane_Tomatensuppe/Bild_Tomatensuppe.data.jpg", 7.9, 9.5),
        menuItem(9000030, "Vegane Treets Erdnüsse mit ChoViva", "100 g", "$IMG/06_Suess__Salzig/Vegane_Treets_Erdnuesse_mit_ChoViva/Bild_Vegane_Treets_Erdnuesse_mit_ChoViva.data.jpg", 3.6, 4.3, listOf("1","8")),
        menuItem(9000031, "Vegane Currywurst mit BIO Brötchen", "& Tortilla-Crunch", "$IMG/03_Snacks/Vegane_Currywurst/Bild_Vegane_Currywurst.data.jpg", 7.9, 9.5),
        menuItem(9000032, "Vegane Currywurst mit Pommes frites", "", "$IMG/03_Snacks/Vegane_Currywurst_mit_Pommes_frites/Bild_Vegane_Currywurst_mit_Pommes_frites.data.jpg", 10.9, 13.1),
    )),
    MenuCategory("Heißgetränke", listOf(
        menuItem(9000033, "Filterkaffee", "", "$IMG/07_Heissgetraenke/Filterkaffee/Bild_Kaffee.data.jpg", 3.9, 4.7),
        menuItem(9000034, "Filterkaffee mit OATLY", "", "$IMG/07_Heissgetraenke/Filterkaffee_mit_Oatly/Bild_Filterkaffee_Oatly.data.jpg", 3.9, 4.7),
        menuItem(9000035, "Kaffee löslich, entkoffeiniert", "", "$IMG/07_Heissgetraenke/Kaffee_loeslich_entkoffeiniert/Bild_Kaffee_loeslich_entkoffeiniert.data.jpg", 3.9, 4.7),
        menuItem(9000036, "Kaffee löslich, entkoffeiniert mit OATLY", "", "$IMG/07_Heissgetraenke/Kaffee_loeslich_entkoffeiniert_mit_OATLY_Haferdrink/Bild_Kaffee_loeslich_entkoffeiniert.data.jpg", 3.9, 4.7),
    )),
    MenuCategory("Kaltgetränke", listOf(
        menuItem(9000037, "share Mineralwasser sprudelnd", "0,5 l Flasche", "$IMG/08_Kaltgetraenke/share_Mineralwasser_sprudelnd/Bild_share_Mineralwasser_sprudelnd.data.jpg", 3.8, 4.6),
        menuItem(9000038, "share Mineralwasser still", "0,5 l Flasche", "$IMG/08_Kaltgetraenke/share_Mineralwasser_still/Bild_share_Mineralwasser_still.data.jpg", 3.8, 4.6),
        menuItem(9000039, "Orangina Original", "Orangenlimonade, 0,25 l Flasche", "$IMG/00_Aktion/2026_02-Februar/Orangina/Bild_Orangina_Original.data.jpg", 4.2, 5.0),
        menuItem(9000040, "Krombacher Spezi", "0,33 l Flasche", "$IMG/08_Kaltgetraenke/Krombacher_Spezi/Bild_Krombacher_Spezi.data.jpg", 3.9, 4.7, listOf("1","12")),
    )),
    MenuCategory("Alkoholische Getränke", listOf(
        menuItem(9000041, "Peroni Nastro Azzurro", "Lagerbier, 0,33 l Flasche", "$IMG/00_Aktion/2026_02-Februar/Peroni_Nastro_Azzurro/Bild_Peroni_Nastro_Azzurro.data.jpg", 4.2, 5.0),
        menuItem(9000042, "Starnberger Hell", "0,5 l Flasche", "$IMG/11_Alkoholische_Getraenke/Starnberger_Hell/Bild_Starnberger_Hell.data.jpg", 5.2, 6.2),
        menuItem(9000043, "König Pilsener", "0,33 l Flasche", "$IMG/11_Alkoholische_Getraenke/Koenig_Pilsener/Bild_Koenig_Pilsener.data.jpg", 4.2, 5.0),
        menuItem(9000044, "Jever Fun Alkoholfrei", "0,33 l Flasche", "$IMG/11_Alkoholische_Getraenke/Jever_Fun_Alkoholfrei/Bild_Jever_Fun_Alkoholfrei.data.jpg", 4.2, 5.0),
    )),
)
