package com.nruge.iceinfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TrainRepository {

    private const val STATUS_URL = "https://iceportal.de/api1/rs/status"
    private const val TRIP_URL = "https://iceportal.de/api1/rs/tripInfo/trip"
    private const val POI_URL = "https://iceportal.de/api1/rs/pois/all"

    suspend fun fetchPois(): List<PoiItem> = withContext(Dispatchers.IO){
        try {
            val json = httpGet(POI_URL) ?: return@withContext emptyList()
            parsePois(json)
            } catch (e: Exception) {
            android.util.Log.e("POI Fehler", "Fehler: ${e.message}")
            emptyList()
        }
    }

    private fun parsePois(json: String): List<PoiItem> {
        val root = JSONObject(json)
        val pois = root.optJSONArray("pois") ?: return emptyList()
        val poiList = mutableListOf<PoiItem>()
        for (i in 0 until pois.length()) {
            val poi = pois.getJSONObject(i)
            poiList.add(
                PoiItem(
                    name = poi.optString("name", ""),
                    type = poi.optString("type", ""),
                    distance = poi.optInt("distance", 0),
                    latitude = poi.optDouble("latitude", 0.0),
                    longitude = poi.optDouble("longitude", 0.0),
                    description = poi.optString("description", "")
                )
            )
        }
        return poiList.sortedBy { it.distance }
    }

    suspend fun fetchTrainStatus(): TrainStatus = withContext(Dispatchers.IO) {
        try {
            val statusJson = httpGet(STATUS_URL)
            val tripJson = httpGet(TRIP_URL)
            val tzn: String = ""
            android.util.Log.d("ICERepo", "Status: $statusJson")
            android.util.Log.d("ICERepo", "Trip: $tripJson")

            if (statusJson == null || tripJson == null) {
                android.util.Log.d("ICERepo", "JSON ist null -> Fallback")
                return@withContext fallback()
            }

            parseTrainStatus(statusJson, tripJson)

        } catch (e: Exception) {
            android.util.Log.e("ICERepo", "Fehler: ${e.message}")
            fallback()
        }
    }

    private fun parseTrainStatus(statusJson: String, tripJson: String): TrainStatus {
        val status = JSONObject(statusJson)
        val trip = JSONObject(tripJson).optJSONObject("trip") ?: return fallback()
        val stops = trip.optJSONArray("stops")

        val speed = status.optDouble("speed", 0.0).toInt()
        val trainType = trip.optString("trainType", "ICE")
        val trainNumber = trip.optString("vzn", "")
        val latitude = status.optDouble("latitude", 0.0)
        val longitude = status.optDouble("longitude", 0.0)
        val tzn = status.optString("tzn", "")
        val wagonClass = status.optString("wagonClass", "")
        val connectivity = JSONObject(statusJson)
            .optJSONObject("connectivity")
            ?.optString("currentState", "") ?: ""

        var nextStop = "Unbekannt"
        var destination = "Unbekannt"
        var eta = "--:--"
        var delayMinutes = 0
        var track = ""
        var delayReason = ""
        var distanceToNext = 0
        var destinationEta = ""
        var destinationTrack = ""
        var destinationDelay = 0
        var distanceLastToNext = 0
        var distanceToDestination = 0
        val stopList = mutableListOf<TrainStop>()
        var nextFound = false

        if (stops != null) {
            // Letzter Halt = Ziel
            val lastStop = stops.getJSONObject(stops.length() - 1)
            destination = lastStop.optJSONObject("station")
                ?.optString("name", "Unbekannt") ?: "Unbekannt"

            // Zieldetails
            val destTimetable = lastStop.optJSONObject("timetable")
            val destScheduledMs = destTimetable?.optLong("scheduledArrivalTime", 0L) ?: 0L
            val destActualMs = destTimetable?.optLong("actualArrivalTime", 0L) ?: 0L
            val destinationEta = formatTime(destScheduledMs)
            val destinationTrack = lastStop.optJSONObject("track")?.optString("actual", "") ?: ""
            val destinationDelay = if (destActualMs > 0 && destScheduledMs > 0)
                ((destActualMs - destScheduledMs) / 60000L).toInt() else 0
            val totalDistance = lastStop.optJSONObject("info")?.optInt("distanceFromStart", 0) ?: 0
            val currentDistance = trip.optInt("actualPosition", 0)
            val distanceToDestination = totalDistance - currentDistance

            for (i in 0 until stops.length()) {
                val stop = stops.getJSONObject(i)
                val info = stop.optJSONObject("info") ?: continue
                val passed = info.optBoolean("passed", true)
                val timetable = stop.optJSONObject("timetable")

                val scheduledMs = timetable?.optLong("scheduledArrivalTime", 0L) ?: 0L
                val actualMs = timetable?.optLong("actualArrivalTime", 0L) ?: 0L
                val stopDelay = if (actualMs > 0 && scheduledMs > 0)
                    ((actualMs - scheduledMs) / 60000L).toInt() else 0

                val stopTrack = stop.optJSONObject("track")
                    ?.optString("actual", "") ?: ""
                val stopName = stop.optJSONObject("station")
                    ?.optString("name", "?") ?: "?"

                val isNext = !passed && !nextFound
                if (isNext) {
                    nextFound = true
                    nextStop = stopName
                    eta = formatTime(scheduledMs)
                    delayMinutes = stopDelay
                    track = stopTrack
                    distanceToNext = info.optInt("distance", 0)

                    val distanceFromStart = info.optInt("distanceFromStart", 0)
                    distanceLastToNext = distanceFromStart - (if (i > 0) {
                        stops.getJSONObject(i - 1).optJSONObject("info")?.optInt("distanceFromStart", 0) ?: 0
                    } else 0)
                    val reasons = stop.optJSONArray("delayReasons")
                    if (reasons != null && reasons.length() > 0) {
                        delayReason = reasons.getJSONObject(0).optString("text", "")
                    }
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
        }

        return TrainStatus(
            distanceLastToNext = distanceLastToNext,
            trainType = trainType,
            trainNumber = trainNumber,
            speed = speed,
            nextStop = nextStop,
            destination = destination,
            eta = eta,
            delayMinutes = delayMinutes,
            track = track,
            delayReason = delayReason,
            distanceToNext = distanceToNext,
            stops = stopList,
            wagonClass = wagonClass,
            connectivity = connectivity,
            tzn = tzn,
            latitude = latitude,
            longitude = longitude,
            distanceToDestination = distanceToDestination,
            destinationEta = destinationEta,
            destinationTrack = destinationTrack,
            destinationDelay = destinationDelay
        )
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return ""
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ms))
    }

    private fun httpGet(urlString: String): String? {
        return try {
            var currentUrl = urlString
            var conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
            }

            // Redirect manuell folgen
            val code = conn.responseCode
            android.util.Log.d("ICERepo", "Response Code: $code für $currentUrl")

            if (code in 300..399) {
                val newUrl = conn.getHeaderField("Location")
                android.util.Log.d("ICERepo", "Redirect nach: $newUrl")
                conn = URL(newUrl).openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    connectTimeout = 2000
                    readTimeout = 2000
                    setRequestProperty("Accept", "application/json")
                }
            }

            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                android.util.Log.d("ICERepo", "Finaler Code: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ICERepo", "Exception: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // Wird angezeigt wenn kein ICE WLAN erreichbar ist
    private fun fallback() = sampleTrainStatus.copy(isConnected = false)
}