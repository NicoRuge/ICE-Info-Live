package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

/**
 * Persistierter Snapshot einer laufenden Fahrtaufzeichnung. Wird während der Fahrt
 * regelmäßig gespeichert, damit bei App-Ende, Prozess-Tod oder WLAN-Verlust nichts
 * verloren geht. Beim nächsten App-Start wird er entweder als laufende Aufzeichnung
 * fortgesetzt oder — wenn die Reise laut Plan vorbei ist — als Fahrt gespeichert.
 */
@Serializable
data class PendingRecording(
    val id: String,
    val trainType: String,
    val trainNumber: String,
    val originStation: String,
    val destinationEvaNr: String,
    val destinationStation: String,
    val date: String,
    val departureTime: String,
    val originDistanceFromStart: Int,
    val destinationDistanceFromStart: Int,
    val stopsCount: Int,
    val recordGps: Boolean = false,
    val startMs: Long,
    val speedSamples: List<Int> = emptyList(),
    val trackPoints: List<TrackPoint> = emptyList(),
    val topSpeedKmh: Int = 0,
    // Letzter bekannter Stand für den Offline-Abschluss (ohne Zug-WLAN):
    val destinationScheduledArrivalMs: Long = 0L,
    val lastDelayMinutes: Int = 0,
    val lastPassedCount: Int = 0,
    val lastDistanceFromStart: Int = 0
)
