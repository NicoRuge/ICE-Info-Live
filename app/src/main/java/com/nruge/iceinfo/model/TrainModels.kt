package com.nruge.iceinfo.model

import androidx.annotation.StringRes
import com.nruge.iceinfo.R
import kotlinx.serialization.Serializable

enum class AppTheme { LIGHT, DARK, SYSTEM }

@Serializable
data class TrainStatus(
    val trainType: String,
    val trainNumber: String,
    val speed: Int,
    val nextStop: String,
    val destination: String,
    val eta: String,
    val delayMinutes: Int = 0,
    val track: String = "",
    val trackChanged: Boolean = false,
    val delayReason: String = "",
    val distanceToNext: Int = 0,
    val distanceLastToNext: Int = 0,
    val nextStopEva: String = "",
    val stops: List<TrainStop> = emptyList(),
    val wagonClass: String = "",
    val connectivity: String = "",
    val nextConnectivity: String? = null,
    val connectivityRemainingSeconds: Int? = null,
    val tzn: String = "",
    val series: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceToDestination: Int = 0,
    val actualPosition: Int = 0,
    val destinationEta: String = "",
    val destinationTrack: String = "",
    val destinationDelay: Int = 0,
    val isConnected: Boolean = true,
    val targetStopEva: String? = null
)

@Serializable
data class TrainStop(
    val name: String,
    val evaNr: String,
    val scheduledArrival: String,
    val actualArrival: String,
    val delayMinutes: Int,
    val track: String,
    val trackChanged: Boolean = false,
    val passed: Boolean,
    val isNext: Boolean,
    val distanceFromStart: Int = 0,
    val scheduledArrivalMs: Long = 0L,
    val scheduledDepartureMs: Long = 0L,
    val isAdditional: Boolean = false,
    val scheduledDeparture: String = "",
    val actualDeparture: String = "",
    val departureDelayMinutes: Int = 0,
    val isCancelled: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val directionChange: Boolean = false
) {
    val effectiveArrivalMs: Long
        get() = if (scheduledArrivalMs > 0L) scheduledArrivalMs + delayMinutes * 60_000L else 0L
}

@Serializable
data class Departure(
    val line: String,
    val destination: String,
    val scheduledTime: String,
    val delayMinutes: Int = 0,
    val platform: String = "",
    val platformChanged: Boolean = false,
    val cancelled: Boolean = false,
    /** Geplante Abfahrt als Epoch-Millis; 0 = unbekannt (z.B. Demo-Daten). */
    val plannedMs: Long = 0L
)

@Serializable
data class PoiItem(
    val name: String,
    val type: String,
    val distance: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

data class WeatherInfo(
    val stationName: String,
    val temperature: Double,
    val precipitation: Double,
    val windspeed: Double,
    val weatherCode: Int
) {
    enum class JacketType(@StringRes val labelResId: Int) {
        NONE(R.string.jacket_none),
        LIGHT(R.string.jacket_light),
        WARM(R.string.jacket_warm),
        RAIN(R.string.jacket_rain),
        WIND(R.string.jacket_wind)
    }

    val jacketRecommendation: JacketType
        get() = when {
            precipitation > 0.1 -> JacketType.RAIN
            temperature < 8 -> JacketType.WARM
            temperature < 16 -> JacketType.LIGHT
            windspeed > 40 -> JacketType.WIND
            else -> JacketType.NONE
        }
}

@Serializable
data class ConnectingTrain(
    val trainType: String = "",
    val trainNumber: String = "",
    val destination: String = "",
    val departure: String = "",
    val track: String = "",
    val trackChanged: Boolean = false,
    val delayMinutes: Int = 0,
    val reachable: Boolean = true,
    val transferMinutes: Int? = null
)