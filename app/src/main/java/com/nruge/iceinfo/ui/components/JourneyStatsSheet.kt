package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.util.IceUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyStatsSheet(
    journeys: List<SavedJourney>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Jahresfilter: null = alle Jahre
    var selectedYear by rememberSaveable { mutableStateOf<String?>(null) }
    val years = remember(journeys) {
        journeys.mapNotNull { it.date.takeLast(4).toIntOrNull()?.toString() }
            .distinct()
            .sortedDescending()
    }
    val visible = remember(journeys, selectedYear) {
        selectedYear?.let { y -> journeys.filter { it.date.endsWith(y) } } ?: journeys
    }

    val unknownSeries = stringResource(R.string.stats_unknown_series)
    val stats = remember(visible, unknownSeries) { computeStats(visible, unknownSeries) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.journeys_stats),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            // ── Jahresfilter ──────────────────────────────────────────────
            if (years.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedYear == null,
                            onClick = { selectedYear = null },
                            label = { Text(stringResource(R.string.journeys_filter_all)) }
                        )
                    }
                    items(years) { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = {
                                selectedYear = if (selectedYear == year) null else year
                            },
                            label = { Text(year) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Highlights ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HighlightCard(
                        label = stringResource(R.string.stats_journeys),
                        value = "${stats.count}",
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        label = stringResource(R.string.stats_kilometers),
                        value = "%,d km".format(stats.totalKm).replace(',', '.'),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HighlightCard(
                        label = stringResource(R.string.stats_time_on_train),
                        value = formatMinutes(stats.totalMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        label = stringResource(R.string.stats_total_delay),
                        value = formatMinutes(stats.totalDelayMinutes),
                        emphasize = stats.totalDelayMinutes > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HighlightCard(
                        label = stringResource(R.string.stats_punctuality),
                        value = stats.punctualityPercent?.let { "$it %" } ?: "–",
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        label = stringResource(R.string.stats_co2_saved),
                        value = formatCo2(stats.co2SavedKg),
                        modifier = Modifier.weight(1f)
                    )
                }
                stats.totalSpentEuro?.let { spent ->
                    HighlightCard(
                        label = stringResource(R.string.stats_spent),
                        value = "%.2f €".format(spent).replace('.', ','),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Rekorde ───────────────────────────────────────────────
                stats.topSpeedJourney?.let { j ->
                    RecordCard(
                        label = stringResource(R.string.stats_top_speed),
                        value = "${j.topSpeedKmh} km/h",
                        detail = "${j.trainType} ${j.trainNumber} · ${j.date}"
                    )
                }
                stats.longestJourney?.let { j ->
                    RecordCard(
                        label = stringResource(R.string.stats_longest_journey),
                        value = "${j.distanceKm} km",
                        detail = "${j.originStation} → ${j.destinationStation} · ${j.date}"
                    )
                }

                // ── Kilometer je Baureihe ─────────────────────────────────
                KmBarSection(
                    title = stringResource(R.string.stats_km_by_series),
                    entries = stats.kmBySeries
                )

                // ── Kilometer je Zugtyp ───────────────────────────────────
                KmBarSection(
                    title = stringResource(R.string.stats_km_by_type),
                    entries = stats.kmByType
                )

                // ── Meistgefahrene Strecke ────────────────────────────────
                stats.topRoute?.let { (origin, destination, count) ->
                    RecordCard(
                        label = stringResource(R.string.stats_top_route),
                        value = "$origin → $destination",
                        detail = "$count×",
                        valueStyle = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

// ── UI-Bausteine ─────────────────────────────────────────────────────────────

@Composable
private fun HighlightCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordCard(
    label: String,
    value: String,
    detail: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KmBarSection(
    title: String,
    entries: List<Pair<String, Int>>
) {
    if (entries.isEmpty()) return
    val maxKm = entries.maxOf { it.second }.coerceAtLeast(1)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            entries.forEach { (label, km) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(110.dp),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(km.toFloat() / maxKm)
                                .height(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$km km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Berechnung ───────────────────────────────────────────────────────────────

private data class JourneyStats(
    val count: Int,
    val totalKm: Int,
    val totalMinutes: Int,
    val totalDelayMinutes: Int,
    val punctualityPercent: Int?,
    val topSpeedJourney: SavedJourney?,
    val longestJourney: SavedJourney?,
    val kmBySeries: List<Pair<String, Int>>,
    val kmByType: List<Pair<String, Int>>,
    val topRoute: Triple<String, String, Int>?,
    val totalSpentEuro: Double?,
    val co2SavedKg: Int
)

private fun computeStats(journeys: List<SavedJourney>, unknownSeriesLabel: String): JourneyStats {
    val totalKm = journeys.sumOf { it.distanceKm.coerceAtLeast(0) }

    val kmBySeries = journeys
        .filter { it.distanceKm > 0 }
        .groupBy {
            IceUtils.getIceClassFromSeries(it.series, it.tzn.ifBlank { null })
                .ifBlank { unknownSeriesLabel }
        }
        .mapValues { (_, js) -> js.sumOf { it.distanceKm } }
        .toList()
        .sortedByDescending { it.second }
        // Nur zeigen, wenn mindestens eine Baureihe bekannt ist
        .takeIf { entries -> entries.any { it.first != unknownSeriesLabel } }
        ?: emptyList()

    val kmByType = journeys
        .filter { it.distanceKm > 0 && it.trainType.isNotBlank() }
        .groupBy { it.trainType }
        .mapValues { (_, js) -> js.sumOf { it.distanceKm } }
        .toList()
        .sortedByDescending { it.second }
        // Nur interessant, wenn mehr als ein Zugtyp vorkommt
        .takeIf { it.size > 1 }
        ?: emptyList()

    val topRoute = journeys
        .groupBy { it.originStation to it.destinationStation }
        .maxByOrNull { it.value.size }
        ?.takeIf { it.value.size > 1 }
        ?.let { Triple(it.key.first, it.key.second, it.value.size) }

    val spentValues = journeys.mapNotNull { parseEuro(it.price) }
    // UBA-Emissionsfaktoren pro Personen-km: Pkw ~166 g, Fernverkehr Bahn ~29 g
    val co2SavedKg = (totalKm * 0.137).toInt()

    return JourneyStats(
        count = journeys.size,
        totalKm = totalKm,
        totalMinutes = journeys.sumOf { it.durationMinutes.coerceAtLeast(0) },
        totalDelayMinutes = journeys.sumOf { it.delayMinutes.coerceAtLeast(0) },
        punctualityPercent = if (journeys.isEmpty()) null
            else (100.0 * journeys.count { it.delayMinutes < 6 } / journeys.size).toInt(),
        topSpeedJourney = journeys.filter { it.topSpeedKmh > 0 }.maxByOrNull { it.topSpeedKmh },
        longestJourney = journeys.filter { it.distanceKm > 0 }.maxByOrNull { it.distanceKm },
        kmBySeries = kmBySeries,
        kmByType = kmByType,
        topRoute = topRoute,
        totalSpentEuro = spentValues.takeIf { it.isNotEmpty() }?.sum(),
        co2SavedKg = co2SavedKg
    )
}

/** Extrahiert einen Euro-Betrag aus dem Freitext-Preisfeld ("39,90 €" → 39.90). */
private fun parseEuro(raw: String): Double? {
    if (raw.isBlank()) return null
    val match = Regex("""(\d+(?:[.,]\d{1,2})?)""").find(raw) ?: return null
    return match.groupValues[1].replace(',', '.').toDoubleOrNull()
}

private fun formatMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h >= 100 -> "$h h"
        h > 0    -> "$h h $m min"
        else     -> "$m min"
    }
}

private fun formatCo2(kg: Int): String =
    if (kg >= 1000) "%.1f t".format(kg / 1000.0).replace('.', ',') else "$kg kg"
