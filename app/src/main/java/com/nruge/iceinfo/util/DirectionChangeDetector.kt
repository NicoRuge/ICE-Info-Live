package com.nruge.iceinfo.util

import com.nruge.iceinfo.model.TrainStop
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * EVA-Nummern deutscher Kopfbahnhöfe: Jeder Zug, der hier hält und weiterfährt,
 * wechselt zwangsläufig die Fahrtrichtung. Bei Bedarf erweitern.
 */
private val KOPFBAHNHOEFE = setOf(
    "8000105", // Frankfurt (Main) Hbf
    "8000096", // Stuttgart Hbf (bis Inbetriebnahme S21)
    "8000261", // München Hbf
    "8010205", // Leipzig Hbf
    "8002553", // Hamburg-Altona
    "8000250", // Wiesbaden Hbf
    "8003200", // Kassel Hbf
    "8000237"  // Lübeck Hbf
)

/**
 * Ab diesem Knickwinkel (Luftlinie Vorgänger→Halt vs. Halt→Nachfolger) gilt der Halt
 * als Richtungswechsel an einem Durchgangsbahnhof (V-förmiger Laufweg).
 * Bewusst hoch angesetzt: normale Kurvenführung soll nicht auslösen.
 */
private const val REVERSAL_ANGLE_DEG = 130.0

/**
 * Markiert Halte, an denen der Zug die Fahrtrichtung wechselt:
 * 1. Kopfbahnhöfe (EVA-Liste) — zuverlässig, gilt immer für Zwischenhalte.
 * 2. Geometrie-Heuristik über Halt-Koordinaten — erkennt V-förmige Laufwege an
 *    Durchgangsbahnhöfen. Richtungswechsel ohne sichtbaren Knick im Laufweg
 *    (z. B. Köln Hbf Richtung Schnellfahrstrecke) kann sie nicht erkennen.
 * Erster und letzter Halt bekommen nie einen Indikator; ausgefallene Halte werden
 * für die Richtungsbestimmung übersprungen.
 */
fun detectDirectionChanges(stops: List<TrainStop>): List<TrainStop> {
    val active = stops.filter { !it.isCancelled }
    if (active.size < 3) return stops
    val marked = active.filterIndexed { i, stop ->
        i > 0 && i < active.lastIndex &&
            (stop.evaNr in KOPFBAHNHOEFE || isGeometricReversal(active[i - 1], stop, active[i + 1]))
    }.mapTo(mutableSetOf()) { it.evaNr }
    if (marked.isEmpty()) return stops
    return stops.map { if (it.evaNr in marked && !it.isCancelled) it.copy(directionChange = true) else it }
}

/**
 * Leitet Richtungswechsel aus den Wagen-Orientierungen der Wagenreihungs-API ab.
 *
 * `orientations[i]` gehört zu `stops[i]` und mappt Wagen-Schlüssel → FORWARDS/BACKWARDS
 * (`null` = keine Daten für diesen Halt). Flippt die Mehrheit der gemeinsamen Wagen
 * zwischen Halt i-1 und i, wendet der Zug an Halt i (der Datensatz eines Halts
 * repräsentiert den Abfahrtszustand). Halte ohne Vergleichsdaten fehlen im Ergebnis —
 * dort greift die Heuristik aus [detectDirectionChanges]. Der letzte Halt wird nie
 * markiert; ein Richtungswechsel ist nur für weiterfahrende Züge relevant.
 */
fun directionChangesFromOrientations(
    stops: List<TrainStop>,
    orientations: List<Map<String, String>?>
): Map<String, Boolean> {
    if (stops.size != orientations.size) return emptyMap()
    val result = mutableMapOf<String, Boolean>()
    for (i in 1 until stops.lastIndex) {
        val prev = orientations[i - 1] ?: continue
        val cur = orientations[i] ?: continue
        val common = prev.keys intersect cur.keys
        if (common.size < 2) continue
        val flipped = common.count { prev[it] != cur[it] }
        result[stops[i].evaNr] = flipped * 2 > common.size
    }
    return result
}

private fun isGeometricReversal(prev: TrainStop, stop: TrainStop, next: TrainStop): Boolean {
    if (!prev.hasCoordinates() || !stop.hasCoordinates() || !next.hasCoordinates()) return false
    val bearingIn = bearingDeg(prev.latitude, prev.longitude, stop.latitude, stop.longitude)
    val bearingOut = bearingDeg(stop.latitude, stop.longitude, next.latitude, next.longitude)
    val diff = abs(bearingOut - bearingIn) % 360.0
    val turn = if (diff > 180.0) 360.0 - diff else diff
    return turn > REVERSAL_ANGLE_DEG
}

private fun TrainStop.hasCoordinates() = latitude != 0.0 || longitude != 0.0

private fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLambda = Math.toRadians(lon2 - lon1)
    val y = sin(dLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}
