package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val speedKmh: Int,
    val secondsFromStart: Int
) {
    /**
     * Koordinaten auf 5 Nachkommastellen gerundet (≈ 1 m Genauigkeit) — kürzt die
     * JSON-Repräsentation pro Punkt deutlich, ohne sichtbaren Versatz auf der Karte.
     */
    fun rounded() = copy(lat = round5(lat), lon = round5(lon))

    private companion object {
        fun round5(value: Double): Double = kotlin.math.round(value * 100_000.0) / 100_000.0
    }
}
