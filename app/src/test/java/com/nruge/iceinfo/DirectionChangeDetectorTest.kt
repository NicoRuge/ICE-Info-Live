package com.nruge.iceinfo

import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.util.detectDirectionChanges
import com.nruge.iceinfo.util.directionChangesFromOrientations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionChangeDetectorTest {

    private fun stop(
        name: String,
        evaNr: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
        cancelled: Boolean = false
    ) = TrainStop(
        name = name, evaNr = evaNr,
        scheduledArrival = "10:00", actualArrival = "10:00", delayMinutes = 0,
        track = "1", passed = false, isNext = false,
        latitude = lat, longitude = lon, isCancelled = cancelled
    )

    @Test
    fun `kopfbahnhof zwischenhalt wird markiert`() {
        val stops = listOf(
            stop("Fulda", "8000115"),
            stop("Frankfurt (Main) Hbf", "8000105"),
            stop("Mannheim Hbf", "8000244")
        )
        val result = detectDirectionChanges(stops)
        assertTrue(result[1].directionChange)
        assertFalse(result[0].directionChange)
        assertFalse(result[2].directionChange)
    }

    @Test
    fun `kopfbahnhof als endbahnhof wird nicht markiert`() {
        val stops = listOf(
            stop("Fulda", "8000115"),
            stop("Hanau Hbf", "8000150"),
            stop("Frankfurt (Main) Hbf", "8000105")
        )
        val result = detectDirectionChanges(stops)
        assertFalse(result.any { it.directionChange })
    }

    @Test
    fun `geometrischer richtungswechsel an durchgangsbahnhof wird erkannt`() {
        // V-förmiger Laufweg: Nord → Süd → wieder Nord
        val stops = listOf(
            stop("A", "1", lat = 50.0, lon = 8.0),
            stop("B", "2", lat = 49.0, lon = 8.0),
            stop("C", "3", lat = 50.0, lon = 8.1)
        )
        val result = detectDirectionChanges(stops)
        assertTrue(result[1].directionChange)
    }

    @Test
    fun `gerader laufweg wird nicht markiert`() {
        val stops = listOf(
            stop("A", "1", lat = 50.0, lon = 8.0),
            stop("B", "2", lat = 51.0, lon = 8.2),
            stop("C", "3", lat = 52.0, lon = 8.4)
        )
        val result = detectDirectionChanges(stops)
        assertFalse(result.any { it.directionChange })
    }

    @Test
    fun `orientation flip markiert den wendehalt`() {
        // Nachgestellt vom Live-Test ICE 526: Würzburg → Aschaffenburg → Frankfurt (Wende) → Flughafen
        val stops = listOf(stop("Würzburg", "1"), stop("Aschaffenburg", "2"), stop("Frankfurt", "3"), stop("Flughafen", "4"))
        val a = mapOf("31" to "BACKWARDS", "32" to "FORWARDS", "33" to "BACKWARDS")
        val flipped = mapOf("31" to "FORWARDS", "32" to "BACKWARDS", "33" to "FORWARDS")
        val result = directionChangesFromOrientations(stops, listOf(a, a, flipped, flipped))
        assertEquals(mapOf("2" to false, "3" to true), result)
    }

    @Test
    fun `halte ohne orientation daten fehlen im ergebnis`() {
        val stops = listOf(stop("A", "1"), stop("B", "2"), stop("C", "3"), stop("D", "4"))
        val o = mapOf("31" to "FORWARDS", "32" to "BACKWARDS")
        val result = directionChangesFromOrientations(stops, listOf(o, null, o, o))
        assertNull(result["2"]) // kein Vergleich möglich → Heuristik-Fallback greift
        assertNull(result["3"])
    }

    @Test
    fun `letzter halt wird nie aus orientation markiert`() {
        val stops = listOf(stop("A", "1"), stop("B", "2"), stop("C", "3"))
        val o = mapOf("31" to "FORWARDS", "32" to "BACKWARDS")
        val flipped = mapOf("31" to "BACKWARDS", "32" to "FORWARDS")
        val result = directionChangesFromOrientations(stops, listOf(o, o, flipped))
        assertNull(result["3"])
    }

    @Test
    fun `einzelner abweichender wagen loest keinen wechsel aus`() {
        val stops = listOf(stop("A", "1"), stop("B", "2"), stop("C", "3"))
        val a = mapOf("31" to "FORWARDS", "32" to "BACKWARDS", "33" to "FORWARDS")
        val b = mapOf("31" to "BACKWARDS", "32" to "BACKWARDS", "33" to "FORWARDS") // nur 1 von 3 geflippt
        val result = directionChangesFromOrientations(stops, listOf(a, b, b))
        assertEquals(false, result["2"])
    }

    @Test
    fun `ausgefallener kopfbahnhof wird uebersprungen und nicht markiert`() {
        val stops = listOf(
            stop("Fulda", "8000115"),
            stop("Frankfurt (Main) Hbf", "8000105", cancelled = true),
            stop("Mannheim Hbf", "8000244"),
            stop("Karlsruhe Hbf", "8000191")
        )
        val result = detectDirectionChanges(stops)
        assertFalse(result.any { it.directionChange })
    }
}
