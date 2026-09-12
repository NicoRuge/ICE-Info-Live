package com.nruge.iceinfo

import android.content.Context
import android.util.Log
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Träwelling-Check-in für die aktuelle Fahrt. Nutzt den OAuth-Token aus
 * [TraewellingAuth]. Ablauf:
 *   1. Einstiegsbahnhof (Name) → Träwelling-Station-ID via Autocomplete
 *   2. Abfahrten dort → passenden Zug per `line.name` matchen → tripId + lineName
 *   3. POST /trains/checkin (Ziel per IBNR-Kennung = evaNr, kein zweiter Lookup nötig)
 */
object TraewellingRepository {

    private const val BASE = "https://traewelling.de/api/v1"
    private val berlin = ZoneId.of("Europe/Berlin")

    sealed interface CheckInResult {
        data object Success : CheckInResult
        data object NotConnected : CheckInResult
        data object TrainNotFound : CheckInResult
        data class Failure(val message: String?) : CheckInResult
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 8000
            socketTimeoutMillis = 15000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, TraewellingAuth.USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }
    }

    suspend fun checkIn(
        context: Context,
        status: TrainStatus,
        exitStop: TrainStop
    ): CheckInResult = withContext(Dispatchers.IO) {
        val token = TraewellingAuth.validAccessToken(context) ?: return@withContext CheckInResult.NotConnected

        // Einstiegsbahnhof = Ursprung wie bei der Aufzeichnung (letzter passierter Halt)
        val boarding = status.stops.lastOrNull { it.passed } ?: status.stops.firstOrNull()
        ?: return@withContext CheckInResult.Failure("Kein Einstiegsbahnhof bekannt")

        try {
            // 1. Träwelling-Station-ID des Einstiegsbahnhofs
            val query = boarding.name.replace("/", " ").trim()
            val stations: StationSearchDto = client.get("$BASE/trains/station/autocomplete/${query.encodeURLPath()}") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            val stationId = stations.data.firstOrNull()?.id
                ?: return@withContext CheckInResult.Failure("Bahnhof bei Träwelling nicht gefunden")

            // 2. Abfahrten dort → passenden Zug matchen
            val whenIso = boarding.scheduledDepartureMs.takeIf { it > 0 }
                ?.let { Instant.ofEpochMilli(it).atZone(berlin).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
            val departures: DeparturesDto = client.get("$BASE/station/$stationId/departures") {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (whenIso != null) parameter("when", whenIso)
            }.body()

            val trainNo = status.trainNumber.trim()
            val match = departures.data.firstOrNull { dep ->
                dep.tripId.isNotBlank() &&
                    dep.line.name.replace(" ", "").contains(trainNo)
            } ?: return@withContext CheckInResult.TrainNotFound

            // 3. Check-in — Ziel per IBNR-Kennung (= evaNr des Ausstiegshalts)
            val arrivalIso = exitStop.scheduledArrivalMs.takeIf { it > 0 }
                ?.let { Instant.ofEpochMilli(it).atZone(berlin).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
                ?: return@withContext CheckInResult.Failure("Keine Ankunftszeit am Ziel")

            val response = client.post("$BASE/trains/checkin") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    CheckinBody(
                        tripId = match.tripId,
                        lineName = match.line.name,
                        start = stationId,
                        destinationIdentifier = exitStop.evaNr,
                        destinationIdentifierType = "de_db_ibnr",
                        departure = match.plannedWhen ?: whenIso ?: return@withContext CheckInResult.Failure("Keine Abfahrtszeit"),
                        arrival = arrivalIso
                    )
                )
            }
            when {
                response.status.isSuccess() -> CheckInResult.Success
                response.status.value == 409 -> CheckInResult.Failure("Überschneidung mit einem bestehenden Check-in")
                else -> {
                    val body = response.bodyAsText().take(200)
                    Log.w("TraewellingRepo", "checkin HTTP ${response.status.value}: $body")
                    CheckInResult.Failure("Fehler ${response.status.value}")
                }
            }
        } catch (e: Exception) {
            Log.w("TraewellingRepo", "checkin failed: ${e.message}")
            CheckInResult.Failure(e.message)
        }
    }

    @Serializable
    private data class StationSearchDto(val data: List<StationDto> = emptyList())

    @Serializable
    private data class StationDto(val id: Long, val name: String = "")

    @Serializable
    private data class DeparturesDto(val data: List<DepartureDto> = emptyList())

    @Serializable
    private data class DepartureDto(
        val tripId: String = "",
        val line: LineDto = LineDto(),
        val plannedWhen: String? = null,
        @SerialName("when") val realWhen: String? = null
    )

    @Serializable
    private data class LineDto(val name: String = "")

    @Serializable
    private data class CheckinBody(
        val tripId: String,
        val lineName: String,
        val start: Long,
        val destinationIdentifier: String,
        val destinationIdentifierType: String,
        val departure: String,
        val arrival: String
    )
}
