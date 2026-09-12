package com.nruge.iceinfo

import android.util.Log
import com.nruge.iceinfo.model.Departure
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DepartureBoardRepository {

    // Primärquelle: internes Web-API von bahn.de (gleiches Backend wie die Website,
    // kein API-Key, gleiches Muster wie WagenreihungRepository). transport.rest ist
    // nur noch Fallback, da die community-gehostete Instanz häufig Timeouts hat.
    private const val BAHN_URL = "https://www.bahn.de/web/api/reiseloesung/abfahrten"
    private const val FALLBACK_URL = "https://v6.db.transport.rest"

    /** Bahn-Produkte, die auf der Abfahrtstafel erscheinen (kein Bus/Tram/U-Bahn). */
    private val RAIL_PRODUCTS = setOf("ICE", "EC_IC", "IR", "REGIONAL", "SBAHN")

    private const val MAX_RESULTS = 30

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json, contentType = io.ktor.http.ContentType.Any)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 8000
            connectTimeoutMillis = 4000
            socketTimeoutMillis = 8000
        }
        defaultRequest {
            header("Accept", "application/json")
        }
    }

    // Zeiten von bahn.de kommen als lokale Zeit ohne Offset (Europe/Berlin).
    private val bahnZone = ZoneId.of("Europe/Berlin")

    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    /**
     * Departures at [evaNr] starting at [fromMs] (epoch millis), within [durationMin] minutes.
     */
    suspend fun fetchDepartures(
        evaNr: String,
        fromMs: Long,
        durationMin: Int = 90
    ): List<Departure> = withContext(Dispatchers.IO) {
        if (evaNr.isBlank()) return@withContext emptyList()
        try {
            fetchFromBahnDe(evaNr, fromMs, durationMin)
        } catch (e: Exception) {
            Log.w("DepartureBoard", "bahn.de failed (${e.message}), trying transport.rest")
            try {
                fetchFromTransportRest(evaNr, fromMs, durationMin)
            } catch (e2: Exception) {
                Log.e("DepartureBoard", "fetchDepartures failed: ${e2.message}")
                emptyList()
            }
        }
    }

    // ── bahn.de ──────────────────────────────────────────────────────────────

    private suspend fun fetchFromBahnDe(
        evaNr: String,
        fromMs: Long,
        durationMin: Int
    ): List<Departure> {
        val from = Instant.ofEpochMilli(fromMs).atZone(bahnZone)
        val response: BahnBoardResponse = client.get(BAHN_URL) {
            parameter("ortExtId", evaNr)
            parameter("datum", from.format(DateTimeFormatter.ISO_LOCAL_DATE))
            parameter("zeit", from.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        }.body()

        val windowEndMs = fromMs + durationMin * 60_000L
        return response.entries.asSequence()
            .filter { it.verkehrmittel?.produktGattung in RAIL_PRODUCTS }
            .mapNotNull { entry ->
                val plannedMs = entry.zeit?.let { parseBahnTime(it) } ?: return@mapNotNull null
                val actualMs = entry.ezZeit?.let { parseBahnTime(it) }
                val lineName = (entry.verkehrmittel?.mittelText ?: entry.verkehrmittel?.name)
                    ?.trim().orEmpty()
                if (lineName.isEmpty()) return@mapNotNull null
                TimedDeparture(
                    // Fürs Zeitfenster zählt die tatsächliche Abfahrt: ein verspäteter Zug,
                    // der planmäßig vor der Ankunft führe, ist ggf. noch erreichbar.
                    effectiveMs = actualMs ?: plannedMs,
                    departure = Departure(
                        line = lineName,
                        destination = entry.terminus.orEmpty(),
                        scheduledTime = displayFormatter.format(Instant.ofEpochMilli(plannedMs)),
                        delayMinutes = if (actualMs != null) ((actualMs - plannedMs) / 60_000L).toInt() else 0,
                        platform = entry.ezGleis ?: entry.gleis.orEmpty(),
                        platformChanged = !entry.ezGleis.isNullOrEmpty() &&
                            !entry.gleis.isNullOrEmpty() &&
                            entry.ezGleis != entry.gleis,
                        cancelled = entry.meldungen.any {
                            it.text?.contains("fällt aus", ignoreCase = true) == true
                        },
                        plannedMs = plannedMs
                    )
                )
            }
            .filter { it.effectiveMs in fromMs..windowEndMs }
            .sortedBy { it.effectiveMs }
            .take(MAX_RESULTS)
            .map { it.departure }
            .toList()
    }

    private fun parseBahnTime(value: String): Long? = try {
        LocalDateTime.parse(value).atZone(bahnZone).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }

    /** Zwischentyp, damit nach Zeitfenster gefiltert/sortiert werden kann. */
    private data class TimedDeparture(val departure: Departure, val effectiveMs: Long)

    // ── transport.rest (Fallback) ────────────────────────────────────────────

    private suspend fun fetchFromTransportRest(
        evaNr: String,
        fromMs: Long,
        durationMin: Int
    ): List<Departure> {
        val whenIso = OffsetDateTime.ofInstant(Instant.ofEpochMilli(fromMs), ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val response: DeparturesResponse = client.get("$FALLBACK_URL/stops/$evaNr/departures") {
            parameter("when", whenIso)
            parameter("duration", durationMin)
            parameter("results", MAX_RESULTS)
        }.body()

        return response.departures.mapNotNull { d ->
            val plannedMs = d.plannedWhen?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
                ?: return@mapNotNull null
            val delayMin = (d.delay ?: 0) / 60
            val lineName = d.line?.name?.trim().orEmpty()
            if (lineName.isEmpty()) return@mapNotNull null
            Departure(
                line = lineName,
                destination = d.direction.orEmpty(),
                scheduledTime = displayFormatter.format(Instant.ofEpochMilli(plannedMs)),
                delayMinutes = delayMin,
                platform = d.platform ?: d.plannedPlatform.orEmpty(),
                platformChanged = !d.platform.isNullOrEmpty() &&
                    !d.plannedPlatform.isNullOrEmpty() &&
                    d.platform != d.plannedPlatform,
                cancelled = d.cancelled == true,
                plannedMs = plannedMs
            )
        }
    }

    // ── DTOs bahn.de ─────────────────────────────────────────────────────────

    @Serializable
    private data class BahnBoardResponse(
        val entries: List<BahnBoardEntry> = emptyList()
    )

    @Serializable
    private data class BahnBoardEntry(
        val zeit: String? = null,
        val ezZeit: String? = null,
        val gleis: String? = null,
        val ezGleis: String? = null,
        val terminus: String? = null,
        val verkehrmittel: BahnVerkehrsmittel? = null,
        val meldungen: List<BahnMeldung> = emptyList()
    )

    @Serializable
    private data class BahnVerkehrsmittel(
        val name: String? = null,
        val mittelText: String? = null,
        val produktGattung: String? = null
    )

    @Serializable
    private data class BahnMeldung(
        val text: String? = null
    )

    // ── DTOs transport.rest ──────────────────────────────────────────────────

    @Serializable
    private data class DeparturesResponse(
        val departures: List<TrDeparture> = emptyList()
    )

    @Serializable
    private data class TrDeparture(
        @SerialName("plannedWhen") val plannedWhen: String? = null,
        @SerialName("when") val whenStr: String? = null,
        val delay: Int? = null,
        val platform: String? = null,
        val plannedPlatform: String? = null,
        val direction: String? = null,
        val line: TrLine? = null,
        val cancelled: Boolean? = null
    )

    @Serializable
    private data class TrLine(
        val name: String? = null
    )
}
