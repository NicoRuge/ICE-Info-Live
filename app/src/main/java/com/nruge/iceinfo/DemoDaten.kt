package com.nruge.iceinfo

import com.nruge.iceinfo.model.*

val sampleTrainStatus = TrainStatus(
    distanceLastToNext = 120000,
    trainType = "ICE",
    trainNumber = "212",
    speed = 114,
    nextStop = "Göttingen",
    destination = "Hamburg Hbf",
    eta = "14:34",
    delayMinutes = 21,
    tzn = "ICE0701",
    track = "4",
    delayReason = "Warten auf Jan",
    distanceToNext = 86760,
    wagonClass = "FIRST",
    connectivity = "STRONG",
    latitude = 51.4825,
    longitude = 11.9906,
    stops = listOf(
        TrainStop("Hamburg-Altona", "08:12", "08:12", 0, "12", passed = true, isNext = false),
        TrainStop("Hannover Hbf", "09:31", "09:36", 5, "4", passed = true, isNext = false),
        TrainStop("Göttingen", "10:15", "10:17", 2, "10", passed = false, isNext = true),
        TrainStop("Kassel-Wilhelmshöhe", "10:35", "10:38", 3, "2", passed = false, isNext = false),
        TrainStop("Würzburg Hbf", "11:58", "12:02", 4, "6", passed = false, isNext = false),
        TrainStop("Nürnberg Hbf", "12:54", "12:59", 5, "9", passed = false, isNext = false),
        TrainStop("München Hbf", "14:11", "14:11", 0, "18", passed = false, isNext = false)
    ),
    destinationEta = "21:46",
    destinationTrack = "6",
    destinationDelay = 21,
    distanceToDestination = 467000
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
        reachable = true
    ),
    ConnectingTrain(
        trainType = "IC",
        trainNumber = "2045",
        destination = "Frankfurt (Main) Hbf",
        departure = "10:31",
        track = "3",
        delayMinutes = 5,
        reachable = true
    ),
    ConnectingTrain(
        trainType = "RE",
        trainNumber = "3",
        destination = "Hannover Hbf",
        departure = "10:18",
        track = "12",
        delayMinutes = 0,
        reachable = false
    ),
    ConnectingTrain(
        trainType = "ICE",
        trainNumber = "1077",
        destination = "München Hbf",
        departure = "10:47",
        track = "5",
        delayMinutes = 12,
        reachable = true
    ),
    ConnectingTrain(
        trainType = "RB",
        trainNumber = "87",
        destination = "Bebra",
        departure = "10:55",
        track = "1",
        delayMinutes = 0,
        reachable = true
    )
)