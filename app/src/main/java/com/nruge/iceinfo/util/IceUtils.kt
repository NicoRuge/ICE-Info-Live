package com.nruge.iceinfo.util

import android.content.Context
import com.nruge.iceinfo.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object IceUtils {

    // ── Baureihen ────────────────────────────────────────────────────────────

    private data class SeriesInfo(val bezeichnung: String, val vmaxKmh: Int)

    private val SERIES_MAP = mapOf(
        "401" to SeriesInfo("ICE 1",            280),
        "402" to SeriesInfo("ICE 2",            280),
        "403" to SeriesInfo("ICE 3",            300),
        "406" to SeriesInfo("ICE 3M",           300),
        "407" to SeriesInfo("ICE 3 Velaro D",   320),
        "408" to SeriesInfo("ICE 3neo",         320),
        "411" to SeriesInfo("ICE T",            230),
        "412" to SeriesInfo("ICE 4",            265),
        "415" to SeriesInfo("ICE T (5-teilig)", 230),
        "605" to SeriesInfo("ICE TD",           200)
    )

    // ── Tz-Namen (ice_names.json + ice_special_names.json) ───────────────────

    @Serializable
    data class TzNameEntry(
        val tz: String,
        val name: String,
        val taufdatum: String,
        val bemerkung: String
    )

    @Serializable
    data class SpecialNameEntry(
        val tz: String,
        val name: String,
        val taufdatum: String,
        val bemerkung: String
    )

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private var tzNames: Map<String, TzNameEntry> = emptyMap()
    private var specialNames: Map<String, SpecialNameEntry> = emptyMap()

    fun init(context: Context) {
        tzNames = context.assets.open("ice_names.json").bufferedReader().use { it.readText() }
            .let { jsonParser.decodeFromString<List<TzNameEntry>>(it) }
            .associateBy { it.tz }
        specialNames = context.assets.open("ice_special_names.json").bufferedReader().use { it.readText() }
            .let { jsonParser.decodeFromString<List<SpecialNameEntry>>(it) }
            .associateBy { it.tz }
    }

    // ── Hilfsfunktionen ──────────────────────────────────────────────────────

    /** API liefert z.B. "ICE0304" oder "ICE09046" → gibt "304" bzw. "9046" zurück */
    fun parseTzNumber(tzn: String): String = tzn.removePrefix("ICE").trimStart('0')

    /**
     * Leitet die Baureihe aus der Tz-Nummer ab, wenn die API das series-Feld
     * nicht befüllt. Bekannte Tz-Bereiche laut DB-Flottendaten.
     */
    fun inferSeriesFromTzn(tzn: String): String {
        val tz = parseTzNumber(tzn).toIntOrNull() ?: return ""
        return when (tz) {
            in 51..190    -> "401"   // ICE 1
            in 201..260   -> "402"   // ICE 2
            in 300..368   -> "403"   // ICE 3
            in 4600..4699 -> "406"   // ICE 3M (PBKA)
            in 4701..4717 -> "407"   // ICE 3 Velaro D
            in 1101..1132,
            in 1151..1178 -> "411"   // ICE T (7-teilig)
            in 1501..1524 -> "415"   // ICE T (5-teilig)
            in 8001..8099 -> "408"   // ICE 3neo
            in 9001..9499 -> "412"   // ICE 4
            in 601..624   -> "605"   // ICE TD
            else          -> ""
        }
    }

    fun getIceClassFromSeries(series: String, tzn: String? = null): String {
        val effectiveSeries = series.ifEmpty { tzn?.let { inferSeriesFromTzn(it) } ?: "" }
        if (effectiveSeries == "412" && tzn != null) {
            val tz = parseTzNumber(tzn).toIntOrNull() ?: return "ICE 4"
            return when (tz) {
                in 9001..9137 -> "ICE 4 (12-teilig)"
                in 9201..9250 -> "ICE 4 (7-teilig)"
                in 9451..9499 -> "ICE 4 (13-teilig)"
                else          -> "ICE 4"
            }
        }
        return SERIES_MAP[effectiveSeries]?.bezeichnung ?: ""
    }

    fun getIceVmax(series: String, tzn: String? = null): Int? {
        val effectiveSeries = series.ifEmpty { tzn?.let { inferSeriesFromTzn(it) } ?: "" }
        return SERIES_MAP[effectiveSeries]?.vmaxKmh
    }

    /** Gibt den offiziellen Taufnamen (Stadt) für eine Tz-Nummer zurück. */
    fun getTzName(tzn: String): TzNameEntry? = tzNames[parseTzNumber(tzn)]

    /** Gibt eine Sonderlackierungs-/Themenbeschriftung zurück, falls vorhanden. */
    fun getSpecialName(tzn: String): SpecialNameEntry? = specialNames[parseTzNumber(tzn)]

    @Suppress("UNUSED_PARAMETER")
    fun getIceDrawable(tzn: String): Int = R.drawable.ice
}
