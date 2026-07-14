package com.nruge.iceinfo

import android.util.Log
import com.nruge.iceinfo.model.Coach
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.model.WagenreihungResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import com.nruge.iceinfo.util.directionChangesFromOrientations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object WagenreihungRepository {

    private const val BASE_URL =
        "https://www.bahn.de/web/api/reisebegleitung/wagenreihung/vehicle-sequence"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json, contentType = io.ktor.http.ContentType.Any) }
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 5_000
        }
        defaultRequest { header("Accept", "application/json") }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    /**
     * Lädt die Wagenreihung für den aktuellen Zug.
     *
     * @param status  Aktueller Zugstatus (liefert Zugnummer und Zuggattung).
     * @param queryStop  Der Halt, für den die Sektoren abgefragt werden sollen –
     *   idealerweise der geplante Ausstiegsbahnhof des Nutzers.
     *   Wenn `null`, wird auf den ersten Halt (Abfahrtsbahnhof) zurückgegriffen.
     *
     * Sektoren (A/B/C…) gelten immer nur für den abgefragten Bahnhof:
     * Bei einem Ausstiegsbahnhof wird die geplante Ankunftszeit verwendet,
     * beim Abfahrtsbahnhof die geplante Abfahrtszeit.
     */
    suspend fun fetch(status: TrainStatus, queryStop: TrainStop? = null): List<Coach> =
        withContext(Dispatchers.IO) {

        // Bestimme Bahnhof und Zeitstempel für die Abfrage
        val stop: TrainStop
        val timeMs: Long
        if (queryStop != null && queryStop.evaNr.isNotBlank()) {
            stop   = queryStop
            // Ausstiegsbahnhof → geplante Ankunftszeit; Fallback auf Abfahrt (z.B. Startbahnhof)
            timeMs = queryStop.scheduledArrivalMs
                .takeIf { it > 0L }
                ?: queryStop.scheduledDepartureMs
                    .takeIf { it > 0L }
                ?: return@withContext emptyList()
        } else {
            stop   = status.stops.firstOrNull() ?: return@withContext emptyList()
            timeMs = stop.scheduledDepartureMs
                .takeIf { it > 0L } ?: return@withContext emptyList()
        }

        val eva = stop.evaNr.ifBlank { return@withContext emptyList() }
        val trainNumber = status.trainNumber.toIntOrNull()
            ?: return@withContext emptyList()

        val instant = Instant.ofEpochMilli(timeMs)
        val date = dateFormatter.format(instant)
        // bahn.de erwartet ISO 8601 UTC mit Millisekunden: "2026-05-28T03:34:00.000Z"
        val time = instant.toString().replace("Z", ".000Z")

        Log.d("Wagenreihung", "Abfrage für $trainNumber @ ${stop.name} (${stop.evaNr})")

        try {
            val response: WagenreihungResponse = client.get(BASE_URL) {
                parameter("administrationId", 80)
                parameter("category", status.trainType)
                parameter("date", date)
                parameter("evaNumber", eva)
                parameter("number", trainNumber)
                parameter("time", time)
            }.body()

            response.groups
                .flatMap { it.vehicles }
                .map { vehicle ->
                    val cat = vehicle.type.category
                    val closed = vehicle.status != "OPEN"
                    Coach(
                        coachNumber        = vehicle.wagonIdentificationNumber,
                        hasFirstClass      = vehicle.type.hasFirstClass,
                        hasSecondClass     = vehicle.type.hasEconomyClass,
                        vehicleCategory    = cat,
                        sector             = vehicle.platformPosition.sector,
                        // Gesperrte Wagen führen keine nutzbaren Ausstattungen
                        amenities          = if (closed) emptySet() else vehicle.amenities
                            .filter { it.status != "UNAVAILABLE" }
                            .map { it.type }
                            .toSet(),
                        isClosed           = closed
                    )
                }
                .also { Log.d("Wagenreihung", "✓ ${it.size} Wagen geladen für $trainNumber") }
        } catch (e: Exception) {
            Log.w("Wagenreihung", "Fehler beim Laden der Wagenreihung: ${e.message}")
            emptyList()
        }
    }

    // --- Fahrtrichtungswechsel über Wagen-Orientierungen -------------------

    /** Pro Fahrt genau ein Abfrage-Durchlauf über alle Halte — auch bei Teilfehlern
     *  kein erneuter Versuch, um die API nicht mit Wiederholungen zu belasten. */
    private var directionCacheKey: String? = null
    private var directionCache: Map<String, Boolean> = emptyMap()
    private val directionMutex = Mutex()

    /**
     * Ermittelt Richtungswechsel-Halte über die `orientation`-Felder der Wagenreihung:
     * Flippt die Orientierung der Wagen zwischen zwei aufeinanderfolgenden Halten,
     * wendet der Zug am zweiten Halt. Liefert evaNr → wechselt-Richtung; Halte ohne
     * verwertbare Daten fehlen in der Map (dort greift die Heuristik als Fallback).
     */
    suspend fun fetchDirectionChanges(status: TrainStatus): Map<String, Boolean> =
        withContext(Dispatchers.IO) {

        val stops = status.stops.filter { !it.isCancelled && it.evaNr.isNotBlank() }
        if (stops.size < 3) return@withContext emptyMap()
        val trainNumber = status.trainNumber.toIntOrNull() ?: return@withContext emptyMap()
        val journeyKey =
            "${status.trainType}$trainNumber@${stops.first().evaNr}:${stops.first().scheduledDepartureMs}"

        directionMutex.withLock {
            if (directionCacheKey == journeyKey) return@withLock directionCache
            directionCacheKey = journeyKey
            directionCache = emptyMap()

            Log.d("Wagenreihung", "Richtungswechsel: frage ${stops.size} Halte ab ($journeyKey)")
            val orientations = stops.map { stop ->
                fetchOrientations(status.trainType, trainNumber, stop)
                    .also { delay(200) } // sanfte Taktung statt Request-Burst
            }
            directionCache = directionChangesFromOrientations(stops, orientations)
            Log.d("Wagenreihung", "✓ Richtungswechsel an: ${directionCache.filterValues { it }.keys}")
            directionCache
        }
    }

    /** Wagen-Schlüssel → Orientierung für einen Halt; `null` wenn nicht verfügbar. */
    private suspend fun fetchOrientations(
        category: String,
        trainNumber: Int,
        stop: TrainStop
    ): Map<String, String>? {
        // Abfahrtszeit bevorzugen: der Datensatz eines Halts ist der Abfahrtszustand,
        // dadurch erscheint der Orientierungs-Flip am Wende-Halt selbst.
        val timeMs = stop.scheduledDepartureMs.takeIf { it > 0L }
            ?: stop.scheduledArrivalMs.takeIf { it > 0L }
            ?: return null
        val instant = Instant.ofEpochMilli(timeMs)
        return try {
            val response: WagenreihungResponse = client.get(BASE_URL) {
                parameter("administrationId", 80)
                parameter("category", category)
                parameter("date", dateFormatter.format(instant))
                parameter("evaNumber", stop.evaNr)
                parameter("number", trainNumber)
                parameter("time", instant.toString().replace("Z", ".000Z"))
            }.body()
            response.groups
                .flatMap { it.vehicles }
                .mapNotNull { v ->
                    val key = v.vehicleID.ifBlank {
                        v.wagonIdentificationNumber.takeIf { it != 0 }?.toString().orEmpty()
                    }
                    if (key.isBlank() || v.orientation.isBlank()) null else key to v.orientation
                }
                .toMap()
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w("Wagenreihung", "Orientierung für ${stop.name} nicht ladbar: ${e.message}")
            null
        }
    }
}
