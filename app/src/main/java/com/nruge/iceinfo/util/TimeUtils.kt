package com.nruge.iceinfo.util

fun calculateDelayMinutes(actualMs: Long, scheduledMs: Long): Int =
    if (actualMs > 0 && scheduledMs > 0)
        ((actualMs - scheduledMs) / 60000L).toInt()
    else 0

fun formatRemainingTime(distanceMeters: Int, speedKmh: Int): String {
    if (speedKmh <= 0) return "--"
    val remainingMinutes = (distanceMeters / 1000f / speedKmh * 60).toInt()
    val hours = remainingMinutes / 60
    val minutes = remainingMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}
