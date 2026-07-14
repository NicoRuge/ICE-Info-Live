package com.nruge.iceinfo

import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.util.ICE_HOSTS
import com.nruge.iceinfo.util.buildIceHttpClient
import com.nruge.iceinfo.util.calculateDelayMinutes
import com.nruge.iceinfo.util.detectDirectionChanges
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TrainRepository {

    private const val API_PATH_STATUS = "/api1/rs/status"
    private const val API_PATH_TRIP   = "/api1/rs/tripInfo/trip"
    private const val API_PATH_POIS   = "/api1/rs/pois/map"
    private const val API_PATH_CONN   = "/api1/rs/tripInfo/connection"
    private const val API_PATH_CONFIG = "/bap/api/config"

    private val hosts = ICE_HOSTS

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = buildIceHttpClient(json)

    /** GET with automatic HTTP fallback if HTTPS fails. */
    private suspend inline fun <reified T> getWithFallback(path: String): T {
        var lastException: Exception? = null
        for (host in hosts) {
            try {
                return client.get("$host$path").body()
            } catch (e: Exception) {
                Log.w("ICERepo", "GET $host$path failed: ${e.message}")
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("No hosts configured for $path")
    }

    /** Raw text GET with automatic HTTP fallback. */
    private suspend fun getRawWithFallback(path: String): String {
        var lastException: Exception? = null
        for (host in hosts) {
            try {
                return client.get("$host$path").bodyAsText()
            } catch (e: Exception) {
                Log.w("ICERepo", "GET $host$path failed: ${e.message}")
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("No hosts configured for $path")
    }

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    suspend fun fetchPois(lat: Double, lon: Double, radiusDeg: Double = 0.5): List<PoiItem> = withContext(Dispatchers.IO) {
        if (lat == 0.0 && lon == 0.0) return@withContext emptyList()
        val path = "$API_PATH_POIS/${lat - radiusDeg}/${lon - radiusDeg}/${lat + radiusDeg}/${lon + radiusDeg}"
        try {
            // The ICE portal returns either {"pois":[…]} or a bare array [… ] depending on firmware.
            val raw = getRawWithFallback(path)
            val element = json.parseToJsonElement(raw)
            when (element) {
                is JsonArray  -> json.decodeFromJsonElement<List<PoiItem>>(element)
                is JsonObject -> {
                    val inner = element["pois"] ?: return@withContext emptyList()
                    json.decodeFromJsonElement<List<PoiItem>>(inner)
                }
                else -> emptyList()
            }.sortedBy { it.distance }
        } catch (e: Exception) {
            Log.e("ICERepo", "POI Fehler ($path): ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchTrainStatus(): TrainStatus = withContext(Dispatchers.IO) {
        try {
            val status: StatusResponse = getWithFallback(API_PATH_STATUS)
            val tripResponse: TripResponse = getWithFallback(API_PATH_TRIP)
            val trip = tripResponse.trip ?: return@withContext fallback()
            mapToTrainStatus(status, trip)
        } catch (e: Exception) {
            Log.e("ICERepo", "Status Fehler: ${e.message}", e)
            fallback()
        }
    }

    private fun mapToTrainStatus(status: StatusResponse, trip: TripInfo): TrainStatus {
        val stops = trip.stops
        val lastStop = stops.lastOrNull()
        val destination = lastStop?.station?.name ?: "Unbekannt"
        val destTimetable = lastStop?.timetable
        val destScheduledMs = destTimetable?.scheduledArrivalTime ?: 0L
        val destActualMs = destTimetable?.actualArrivalTime ?: 0L
        val destinationEta = formatTime(if (destActualMs > 0L) destActualMs else destScheduledMs)
        val destinationTrack = lastStop?.track?.actual ?: ""
        val destinationDelay = calculateDelayMinutes(destActualMs, destScheduledMs)
        val totalDistance = lastStop?.info?.distanceFromStart ?: 0
        val currentDistance = trip.actualPosition
        val distanceToDestination = totalDistance - currentDistance
        var nextStopName = "Unbekannt"
        var eta = "--:--"
        var delayMinutes = 0
        var track = ""
        var trackChanged = false
        var delayReason = ""
        var distanceToNext = 0
        var distanceLastToNext = 0
        val stopList = mutableListOf<TrainStop>()
        var nextFound = false
        var nextStopEva = ""

        stops.forEachIndexed { i, stop ->
            val info = stop.info ?: return@forEachIndexed
            val passed = info.passed
            val timetable = stop.timetable

            val scheduledMs = timetable?.scheduledArrivalTime ?: 0L
            val actualMs = timetable?.actualArrivalTime ?: 0L
            val stopDelay = calculateDelayMinutes(actualMs, scheduledMs)

            val depScheduledMs = timetable?.scheduledDepartureTime ?: 0L
            val depActualMs = timetable?.actualDepartureTime ?: 0L
            val depDelay = calculateDelayMinutes(depActualMs, depScheduledMs)

            val stopTrack = stop.track?.actual ?: ""
            val stopTrackChanged = stop.track?.changed ?: false
            val stopName = stop.station?.name ?: "?"

            val isCancelled = stop.cancelled || info.status == 3
            val isNext = !passed && !nextFound && !isCancelled
            if (isNext) {
                nextStopEva = stop.station?.evaNr ?: ""
                nextFound = true
                nextStopName = stopName
                eta = formatTime(if (actualMs > 0L) actualMs else scheduledMs)
                delayMinutes = stopDelay
                track = stopTrack
                trackChanged = stopTrackChanged
                distanceToNext = info.distance

                val distanceFromStart = info.distanceFromStart
                distanceLastToNext = distanceFromStart - (if (i > 0) {
                    stops[i - 1].info?.distanceFromStart ?: 0
                } else 0)

                delayReason = stop.delayReasons?.firstOrNull()?.text ?: ""
            }

            stopList.add(TrainStop(
                name = stopName,
                evaNr = stop.station?.evaNr ?: "",
                scheduledArrival = formatTime(scheduledMs),
                actualArrival = formatTime(actualMs),
                delayMinutes = stopDelay,
                track = stopTrack,
                trackChanged = stopTrackChanged,
                passed = passed,
                isNext = isNext,
                distanceFromStart = info.distanceFromStart,
                scheduledArrivalMs = scheduledMs,
                scheduledDepartureMs = depScheduledMs,
                isAdditional = info.status == 2,
                scheduledDeparture = formatTime(depScheduledMs),
                actualDeparture = formatTime(depActualMs),
                departureDelayMinutes = depDelay,
                isCancelled = stop.cancelled || info.status == 3,
                latitude = stop.station?.geocoordinates?.latitude ?: 0.0,
                longitude = stop.station?.geocoordinates?.longitude ?: 0.0
            ))
        }

        return TrainStatus(
            trainType = trip.trainType,
            trainNumber = trip.vzn,
            speed = status.speed.toInt(),
            nextStop = nextStopName,
            destination = destination,
            nextStopEva = nextStopEva,
            eta = eta,
            delayMinutes = delayMinutes,
            track = track,
            trackChanged = trackChanged,
            delayReason = delayReason,
            distanceToNext = distanceToNext,
            distanceLastToNext = distanceLastToNext,
            stops = detectDirectionChanges(stopList),
            wagonClass = status.wagonClass,
            connectivity = status.connectivity?.currentState ?: "",
            nextConnectivity = status.connectivity?.nextState,
            connectivityRemainingSeconds = status.connectivity?.remainingTimeSeconds,
            tzn = status.tzn,
            series = status.series,
            latitude = status.latitude,
            longitude = status.longitude,
            distanceToDestination = distanceToDestination,
            actualPosition = trip.actualPosition,
            destinationEta = destinationEta,
            destinationTrack = destinationTrack,
            destinationDelay = destinationDelay,
            isConnected = true
        )
    }

    suspend fun fetchConnections(evaNr: String, ourArrivalMs: Long = 0L): List<ConnectingTrain> = withContext(Dispatchers.IO) {
        try {
            if (evaNr.isEmpty()) return@withContext emptyList()
            val response: ConnectionResponse = getWithFallback("$API_PATH_CONN/$evaNr")
            val now = System.currentTimeMillis()
            response.connections?.map { c ->
                val scheduledMs = c.timetable?.scheduledDepartureTime ?: 0L
                val actualMs = c.timetable?.actualDepartureTime ?: 0L
                val delayMin = calculateDelayMinutes(actualMs, scheduledMs)
                val effectiveDepartureMs = if (actualMs > 0) actualMs else scheduledMs
                val reachable = when {
                    effectiveDepartureMs <= 0L -> true
                    ourArrivalMs > 0L -> effectiveDepartureMs > ourArrivalMs
                    else -> effectiveDepartureMs > now
                }
                val transferMinutes = if (ourArrivalMs > 0L && effectiveDepartureMs > ourArrivalMs) {
                    ((effectiveDepartureMs - ourArrivalMs) / 60_000L).toInt()
                } else null
                ConnectingTrain(
                    trainType = c.trainType,
                    trainNumber = c.vzn,
                    destination = c.finalStation,
                    departure = formatTime(scheduledMs),
                    track = c.track?.actual ?: "",
                    trackChanged = c.track?.changed ?: false,
                    delayMinutes = delayMin,
                    reachable = reachable,
                    transferMinutes = transferMinutes
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ICERepo", "Connection Fehler: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchCoaches(): List<Coach> = withContext(Dispatchers.IO) {
        try {
            val response: CoachConfigResponse = getWithFallback(API_PATH_CONFIG)
            response.coachList?.coaches ?: emptyList()
        } catch (e: Exception) {
            Log.e("ICERepo", "Coach config Fehler: ${e.message}")
            emptyList()
        }
    }

    data class EndpointStatus(val label: String, val ok: Boolean, val detail: String, val body: String? = null)

    data class DebugData(
        val statusRaw: String,
        val statusError: String?,
        val tripRaw: String,
        val tripError: String?,
        val connectionRaw: String,
        val connectionError: String?,
        val evaNr: String
    )

    fun endpointLabels(): List<String> = listOf(
        "API::Status", "API::Trip", "API::Connections", "API::POIs", "API::Coaches"
    )

    suspend fun checkEndpoints(): List<EndpointStatus> = withContext(Dispatchers.IO) {
        listOf(
            "API::Status"      to API_PATH_STATUS,
            "API::Trip"        to API_PATH_TRIP,
            "API::Connections" to "$API_PATH_CONN/",
            "API::POIs"        to "$API_PATH_POIS/0/0/0/0",
            "API::Coaches"     to API_PATH_CONFIG,
        ).map { (label, path) ->
            var ok = false
            var detail = "Fehler"
            var body: String? = null
            for (host in hosts) {
                try {
                    val resp = client.get("$host$path")
                    ok = resp.status.isSuccess()
                    detail = if (ok) "OK" else "HTTP ${resp.status.value}"
                    body = resp.bodyAsText()
                    break
                } catch (e: Exception) {
                    detail = (e.message ?: e.javaClass.simpleName).take(60)
                }
            }
            EndpointStatus(label, ok, detail, body)
        }
    }

    suspend fun fetchDebugData(): DebugData = withContext(Dispatchers.IO) {
        var statusRaw = ""
        var statusError: String? = null
        try {
            statusRaw = getRawWithFallback(API_PATH_STATUS)
            if (statusRaw.isBlank()) statusError = "Leere Antwort"
        } catch (e: Exception) {
            statusError = e.message ?: "Unbekannter Fehler"
        }

        var tripRaw = ""
        var tripError: String? = null
        var evaNr = ""
        try {
            tripRaw = getRawWithFallback(API_PATH_TRIP)
            if (tripRaw.isBlank()) {
                tripError = "Leere Antwort"
            } else {
                try {
                    val tripResponse = json.decodeFromString<TripResponse>(tripRaw)
                    evaNr = tripResponse.trip?.stops
                        ?.firstOrNull { it.info?.passed == false }
                        ?.station?.evaNr ?: ""
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            tripError = e.message ?: "Unbekannter Fehler"
        }

        var connectionRaw = ""
        var connectionError: String? = null
        if (evaNr.isNotEmpty()) {
            try {
                connectionRaw = getRawWithFallback("$API_PATH_CONN/$evaNr")
            } catch (e: Exception) {
                connectionError = e.message ?: "Unbekannter Fehler"
            }
        } else {
            connectionError = "EVA-Nummer nicht verfügbar"
        }

        DebugData(statusRaw, statusError, tripRaw, tripError, connectionRaw, connectionError, evaNr)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return ""
        return timeFormatter.format(Instant.ofEpochMilli(ms))
    }

    private fun fallback() = sampleTrainStatus.copy(isConnected = false)
}
