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
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

object TrainRepository {

    private const val STATUS_URL = "https://iceportal.de/api1/rs/status"
    private const val TRIP_URL = "https://iceportal.de/api1/rs/tripInfo/trip"
    private const val POI_URL = "https://iceportal.de/api1/rs/pois/all"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 2000
            connectTimeoutMillis = 2000
        }
        defaultRequest {
            header("Accept", "application/json")
        }
        // Redirects are handled by default in Ktor OkHttp engine
    }

    suspend fun fetchPois(): List<PoiItem> = withContext(Dispatchers.IO) {
        try {
            val response: PoiResponse = client.get(POI_URL).body()
            response.pois?.sortedBy { it.distance } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("POI Fehler", "Fehler: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchTrainStatus(): TrainStatus = withContext(Dispatchers.IO) {
        try {
            val status: StatusResponse = client.get(STATUS_URL).body()
            val tripResponse: TripResponse = client.get(TRIP_URL).body()
            val trip = tripResponse.trip ?: return@withContext fallback()

            mapToTrainStatus(status, trip)
        } catch (e: Exception) {
            android.util.Log.e("ICERepo", "Fehler: ${e.message}")
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
                scheduledArrival = formatTime(scheduledMs),
                actualArrival = formatTime(actualMs),
                delayMinutes = stopDelay,
                track = stopTrack,
                passed = passed,
                isNext = isNext
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
            tzn = status.tzn,
            latitude = status.latitude,
            longitude = status.longitude,
            distanceToDestination = distanceToDestination,
            destinationEta = destinationEta,
            destinationTrack = destinationTrack,
            destinationDelay = destinationDelay,
            isConnected = true
        )
    }

    suspend fun fetchConnections(evaNr: String): List<ConnectingTrain> = withContext(Dispatchers.IO) {
        try {
            if (evaNr.isEmpty()) return@withContext emptyList()
            val response: ConnectionResponse = client.get(
                "https://iceportal.de/api1/rs/tripInfo/connection/$evaNr"
            ).body()
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
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    private fun fallback() = sampleTrainStatus.copy(isConnected = false)
}
