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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DepartureBoardRepository {

    private const val BASE_URL = "https://v6.db.transport.rest"

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

    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    /**
     * Departures at [evaNr] starting at [fromMs] (epoch millis), within [durationMin] minutes.
     * Excludes the user's current train via [excludeTripId] (transport.rest tripId or partial match).
     */
    suspend fun fetchDepartures(
        evaNr: String,
        fromMs: Long,
        durationMin: Int = 90
    ): List<Departure> = withContext(Dispatchers.IO) {
        if (evaNr.isBlank()) return@withContext emptyList()
        try {
            val whenIso = OffsetDateTime.ofInstant(Instant.ofEpochMilli(fromMs), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val response: DeparturesResponse = client.get("$BASE_URL/stops/$evaNr/departures") {
                parameter("when", whenIso)
                parameter("duration", durationMin)
                parameter("results", 30)
            }.body()

            response.departures.mapNotNull { d ->
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
                    cancelled = d.cancelled == true
                )
            }
        } catch (e: Exception) {
            Log.e("DepartureBoard", "fetchDepartures failed: ${e.message}")
            emptyList()
        }
    }

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
