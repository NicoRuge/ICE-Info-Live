package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val speed: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tzn: String = "",
    val series: String = "",
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
    val delayReasons: List<DelayReason>? = null,
    val cancelled: Boolean = false  // Ausfall kommt i.d.R. über info.status (siehe StopInfo)
)

@Serializable
data class Station(
    val name: String = "",
    val evaNr: String = "",
    val geocoordinates: Geocoordinates? = null
)

@Serializable
data class Geocoordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Serializable
data class StopInfo(
    val passed: Boolean = false,
    val distance: Int = 0,
    val distanceFromStart: Int = 0,
    // 0 = regulärer Halt, 1 = entfällt, 2 = Zusatzhalt (verifiziert per Debug-Report 2026-07-19)
    val status: Int = 0
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
    val actual: String = "",
    val scheduled: String = ""
) {
    /** Gleis wurde geändert, wenn Soll- und Ist-Gleis beide vorhanden sind und sich unterscheiden. */
    val changed: Boolean
        get() = scheduled.isNotEmpty() && actual.isNotEmpty() && scheduled != actual
}

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

@Serializable
data class Coach(
    val coachNumber: Int = 0,
    val hasFirstClass: Boolean = false,
    val hasSecondClass: Boolean = false,
    /** Fahrzeugkategorie aus der DB Wagenreihungs-API.
     *  z.B. LOCOMOTIVE, PASSENGERCARRIAGE_FIRST_CLASS, HALFDININGCAR_ECONOMY_CLASS, CONTROLCAR_* */
    val vehicleCategory: String = "",
    /** Bahnsteig-Sektor aus der DB Wagenreihungs-API, z.B. "A", "B", "C" */
    val sector: String = "",
    /** Ausstattungs-Merkmale des Wagens aus der DB Wagenreihungs-API.
     *  Bekannte Werte: ZONE_QUIET, ZONE_FAMILY, ZONE_PHONE,
     *  BIKE_SPACE, CABIN_INFANT, WHEELCHAIR_SPACE, SEATS_BAHN_COMFORT */
    val amenities: Set<String> = emptySet(),
    /** Wagen ist gesperrt (API-Status != "OPEN", z.B. "CLOSED") – kein Einstieg möglich. */
    val isClosed: Boolean = false
)

// ─── DB Wagenreihungs-API (bahn.de) ───────────────────────────────────────────

@Serializable
data class WagenreihungResponse(
    val departurePlatform: String = "",
    val departurePlatformSchedule: String = "",
    val groups: List<WagenreihungGroup> = emptyList()
)

@Serializable
data class WagenreihungGroup(
    val vehicles: List<WagenreihungVehicle> = emptyList()
)

@Serializable
data class WagenreihungVehicle(
    val wagonIdentificationNumber: Int = 0,
    val vehicleID: String = "",
    val status: String = "OPEN",
    /** FORWARDS/BACKWARDS relativ zur Fahrtrichtung — flippt an einem Wende-Halt. */
    val orientation: String = "",
    val type: WagenreihungVehicleType = WagenreihungVehicleType(),
    val platformPosition: WagenreihungPlatformPosition = WagenreihungPlatformPosition(),
    val amenities: List<WagenreihungAmenity> = emptyList()
)

@Serializable
data class WagenreihungAmenity(
    val type: String = "",
    val status: String = "AVAILABLE",
    val amount: Int = 0
)

@Serializable
data class WagenreihungVehicleType(
    val category: String = "",
    val hasFirstClass: Boolean = false,
    val hasEconomyClass: Boolean = false
)

@Serializable
data class WagenreihungPlatformPosition(
    val sector: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0
)

@Serializable
data class CoachList(
    val coaches: List<Coach> = emptyList()
)

@Serializable
data class CoachConfigResponse(
    val coachList: CoachList? = null
)