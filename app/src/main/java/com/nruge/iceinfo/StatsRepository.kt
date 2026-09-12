package com.nruge.iceinfo

import android.content.Context
import android.util.Log
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.util.SettingsManager
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Crowdsourcing von Verspätungsdaten: lädt vom Nutzer manuell freigegebene
 * Fahrten (nur Statistikfelder, kein GPS-Track, keine persönlichen Angaben)
 * zur eigenen API hoch. Dedup passiert serverseitig per Hash über
 * installId|Zug|Start|Ziel|Datum — erneutes Teilen aktualisiert den Eintrag.
 */
object StatsRepository {

    private const val JOURNEYS_URL = "https://api.iceinfo.de/v1/journeys"

    /** Basis der öffentlichen Detailseite einer geteilten Fahrt (stats.iceinfo.de). */
    private const val JOURNEY_LINK_BASE = "https://stats.iceinfo.de/j/"

    /** Vollständiger, teilbarer Link zu einer geteilten Fahrt. */
    fun journeyUrl(hash: String): String = JOURNEY_LINK_BASE + hash

    /** Optisch gekürzter Hash für die Anzeige (voller Hash bleibt im Link). */
    fun shortHash(hash: String): String =
        if (hash.length <= 16) hash else "${hash.take(6)}…${hash.takeLast(6)}"

    @Serializable
    private data class ShareResponse(val hash: String)

    @Serializable
    private data class SharedJourneyDto(
        val trainType: String,
        val trainNumber: String,
        val origin: String,
        val destination: String,
        val date: String,
        val delayMinutes: Int,
        val series: String,
        val installId: String,
        val appVersion: String,
        val departureTime: String,
        val arrivalTime: String,
        val durationMinutes: Int,
        val distanceKm: Int,
        val stopsCount: Int,
        val tzn: String,
        val finalStation: String,
        val finalDelayMinutes: Int,
        val finalDelayIsPrognosis: Boolean,
        val stops: List<SharedStopDto>
    )

    @Serializable
    private data class SharedStopDto(
        val name: String,
        val time: String,
        val delayMinutes: Int,
        val cancelled: Boolean,
        val additional: Boolean,
        val prognosis: Boolean
    )

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 8000
            connectTimeoutMillis = 4000
            socketTimeoutMillis = 8000
        }
    }

    /** Liefert bei Erfolg den Server-Hash der Fahrt (auch bei erneutem Teilen), sonst null. */
    suspend fun shareJourney(context: Context, journey: SavedJourney): String? =
        withContext(Dispatchers.IO) {
            try {
                val response = client.post(JOURNEYS_URL) {
                    header(HttpHeaders.Authorization, "Bearer ${BuildConfig.STATS_API_TOKEN}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        SharedJourneyDto(
                            trainType = journey.trainType,
                            trainNumber = journey.trainNumber,
                            origin = journey.originStation,
                            destination = journey.destinationStation,
                            date = journey.date,
                            delayMinutes = journey.delayMinutes,
                            series = journey.series,
                            installId = SettingsManager.getInstallId(context),
                            appVersion = BuildConfig.VERSION_NAME,
                            departureTime = journey.departureTime,
                            arrivalTime = journey.arrivalTime,
                            durationMinutes = journey.durationMinutes,
                            distanceKm = journey.distanceKm,
                            stopsCount = journey.stopsCount,
                            tzn = journey.tzn,
                            finalStation = journey.finalStation,
                            finalDelayMinutes = journey.finalDelayMinutes,
                            finalDelayIsPrognosis = journey.finalDelayIsPrognosis,
                            stops = journey.stops.map {
                                SharedStopDto(it.name, it.time, it.delayMinutes, it.cancelled, it.additional, it.prognosis)
                            }
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    response.body<ShareResponse>().hash
                } else {
                    Log.w("StatsRepository", "shareJourney: HTTP ${response.status.value} ${response.bodyAsText().take(200)}")
                    null
                }
            } catch (e: Exception) {
                Log.w("StatsRepository", "shareJourney failed: ${e.message}")
                null
            }
        }
}
