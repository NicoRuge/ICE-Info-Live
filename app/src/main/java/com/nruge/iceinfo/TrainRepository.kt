package com.nruge.iceinfo

import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.util.calculateDelayMinutes
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Try HTTPS first; many older ICE portals also serve plain HTTP
    private val hosts = listOf("https://iceportal.de", "http://iceportal.de")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json, contentType = io.ktor.http.ContentType.Any)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 3000
            socketTimeoutMillis  = 5000
        }
        defaultRequest {
            header("Accept", "application/json")
        }
    }

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
        throw lastException!!
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
        throw lastException!!
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
        val destinationEta = formatTime(destScheduledMs)
        val destinationTrack = lastStop?.track?.actual ?: ""
        val destinationDelay = calculateDelayMinutes(destActualMs, destScheduledMs)
        val totalDistance = lastStop?.info?.distanceFromStart ?: 0
        val currentDistance = trip.actualPosition
        val distanceToDestination = totalDistance - currentDistance
        var nextStopName = "Unbekannt"
        var eta = "--:--"
        var delayMinutes = 0
        var track = ""
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

            val stopTrack = stop.track?.actual ?: ""
            val stopName = stop.station?.name ?: "?"

            val isNext = !passed && !nextFound
            if (isNext) {
                nextStopEva = stop.station?.evaNr ?: ""
                nextFound = true
                nextStopName = stopName
                eta = formatTime(scheduledMs)
                delayMinutes = stopDelay
                track = stopTrack
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
                passed = passed,
                isNext = isNext,
                distanceFromStart = info.distanceFromStart
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
            delayReason = delayReason,
            distanceToNext = distanceToNext,
            distanceLastToNext = distanceLastToNext,
            stops = stopList,
            wagonClass = status.wagonClass,
            connectivity = status.connectivity?.currentState ?: "",
            nextConnectivity = status.connectivity?.nextState,
            connectivityRemainingSeconds = status.connectivity?.remainingTimeSeconds,
            tzn = status.tzn,
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

    suspend fun fetchConnections(evaNr: String): List<ConnectingTrain> = withContext(Dispatchers.IO) {
        try {
            if (evaNr.isEmpty()) return@withContext emptyList()
            val response: ConnectionResponse = getWithFallback("$API_PATH_CONN/$evaNr")
            response.connections?.map { c ->
                val scheduledMs = c.timetable?.scheduledDepartureTime ?: 0L
                val actualMs = c.timetable?.actualDepartureTime ?: 0L
                val delayMin = calculateDelayMinutes(actualMs, scheduledMs)
                ConnectingTrain(
                    trainType = c.trainType,
                    trainNumber = c.vzn,
                    destination = c.finalStation,
                    departure = formatTime(scheduledMs),
                    track = c.track?.actual ?: "",
                    delayMinutes = delayMin,
                    reachable = !c.missed
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ICERepo", "Connection Fehler: ${e.message}")
            emptyList()
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return ""
        return timeFormatter.format(Instant.ofEpochMilli(ms))
    }

    private fun fallback() = sampleTrainStatus.copy(isConnected = false)
}
