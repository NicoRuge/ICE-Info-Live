package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val speed: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tzn: String = "",
    val wagonClass: String = "",
    val connectivity: Connectivity? = null
)

@Serializable
data class Connectivity(
    val currentState: String = "",
    val nextState: String? = null,
    val remainingTimeSeconds: Int? = null
)

@Serializable
data class TripResponse(
    val trip: TripInfo? = null
)

@Serializable
data class TripInfo(
    val trainType: String = "ICE",
    val vzn: String = "",
    val actualPosition: Int = 0,
    val stops: List<ApiStop> = emptyList()
)

@Serializable
data class ApiStop(
    val station: Station? = null,
    val info: StopInfo? = null,
    val timetable: Timetable? = null,
    val track: Track? = null,
    val delayReasons: List<DelayReason>? = null
)

@Serializable
data class Station(
    val name: String = "",
    val evaNr: String = ""
)

@Serializable
data class StopInfo(
    val passed: Boolean = false,
    val distance: Int = 0,
    val distanceFromStart: Int = 0
)

@Serializable
data class Timetable(
    val scheduledArrivalTime: Long = 0,
    val actualArrivalTime: Long = 0,
    val scheduledDepartureTime: Long = 0,
    val actualDepartureTime: Long = 0
)

@Serializable
data class Track(
    val actual: String = ""
)

@Serializable
data class DelayReason(
    val text: String = ""
)

@Serializable
data class PoiResponse(
    val pois: List<PoiItem>? = null
)

@Serializable
data class ConnectionResponse(
    val connections: List<ApiConnection>? = null
)

@Serializable
data class ApiConnection(
    val trainType: String = "",
    val vzn: String = "",
    val finalStation: String = "",
    val timetable: Timetable? = null,
    val track: Track? = null,
    val missed: Boolean = false
)