package com.nruge.iceinfo.model

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
    val passed: Boolean,
    val isNext: Boolean,
    val distanceFromStart: Int = 0
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

@Serializable
data class ConnectingTrain(
    val trainType: String = "",
    val trainNumber: String = "",
    val destination: String = "",
    val departure: String = "",
    val track: String = "",
    val delayMinutes: Int = 0,
    val reachable: Boolean = true
)