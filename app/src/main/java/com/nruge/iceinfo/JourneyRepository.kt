package com.nruge.iceinfo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nruge.iceinfo.model.PendingRecording
import com.nruge.iceinfo.model.SavedJourney
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Context.journeyDataStore by preferencesDataStore(name = "journeys")

object JourneyRepository {

    private val JOURNEYS_KEY = stringPreferencesKey("saved_journeys")
    private val PENDING_RECORDING_KEY = stringPreferencesKey("pending_recording")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadJourneys(context: Context): List<SavedJourney> {
        val prefs = context.journeyDataStore.data.first()
        val raw = prefs[JOURNEYS_KEY] ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedJourney>>(raw) }
            .getOrDefault(emptyList())
    }

    suspend fun saveJourney(context: Context, journey: SavedJourney) {
        context.journeyDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<SavedJourney>>(prefs[JOURNEYS_KEY] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[JOURNEYS_KEY] = json.encodeToString(listOf(journey) + current)
        }
    }

    suspend fun deleteJourney(context: Context, id: String) {
        context.journeyDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<SavedJourney>>(prefs[JOURNEYS_KEY] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[JOURNEYS_KEY] = json.encodeToString(current.filter { it.id != id })
        }
    }

    // --- Export / Import ----------------------------------------------------

    private val sortFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    /** Serialisiert Fahrten für den Datei-Export (gleiches Format wie der DataStore). */
    fun encodeJourneys(journeys: List<SavedJourney>): String = json.encodeToString(journeys)

    /** Parst eine Export-Datei; null bei ungültigem Inhalt. */
    fun decodeJourneys(raw: String): List<SavedJourney>? =
        runCatching { json.decodeFromString<List<SavedJourney>>(raw) }.getOrNull()

    /**
     * Führt importierte Fahrten mit den gespeicherten zusammen (Duplikate anhand
     * der id übersprungen) und sortiert nach Abfahrt, neueste zuerst.
     * Liefert die Anzahl neu hinzugefügter Fahrten und die neue Gesamtliste.
     */
    suspend fun importJourneys(context: Context, imported: List<SavedJourney>): Pair<Int, List<SavedJourney>> {
        var added = 0
        var merged: List<SavedJourney> = emptyList()
        context.journeyDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<SavedJourney>>(prefs[JOURNEYS_KEY] ?: "[]")
            }.getOrDefault(emptyList())
            val existingIds = current.mapTo(mutableSetOf()) { it.id }
            val new = imported.filter { it.id.isNotBlank() && it.id !in existingIds }
            added = new.size
            merged = (current + new).sortedByDescending { sortKey(it) }
            prefs[JOURNEYS_KEY] = json.encodeToString(merged)
        }
        return added to merged
    }

    private fun sortKey(journey: SavedJourney): LocalDateTime = runCatching {
        LocalDateTime.parse("${journey.date} ${journey.departureTime}", sortFormatter)
    }.getOrDefault(LocalDateTime.MIN)

    // --- Zwischenstand einer laufenden Aufzeichnung ------------------------

    suspend fun savePendingRecording(context: Context, pending: PendingRecording) {
        context.journeyDataStore.edit { prefs ->
            prefs[PENDING_RECORDING_KEY] = json.encodeToString(pending)
        }
    }

    suspend fun loadPendingRecording(context: Context): PendingRecording? {
        val raw = context.journeyDataStore.data.first()[PENDING_RECORDING_KEY] ?: return null
        return runCatching { json.decodeFromString<PendingRecording>(raw) }.getOrNull()
    }

    suspend fun clearPendingRecording(context: Context) {
        context.journeyDataStore.edit { prefs -> prefs.remove(PENDING_RECORDING_KEY) }
    }
}
