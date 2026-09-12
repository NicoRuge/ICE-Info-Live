package com.nruge.iceinfo.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.SavedJourney
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

// Farbpalette für die einzelnen Fahrten (wird zyklisch wiederverwendet)
private val TrackPalette = listOf(
    Color(0xFF1E88E5), // blau
    Color(0xFFE53935), // rot
    Color(0xFF43A047), // grün
    Color(0xFFFB8C00), // orange
    Color(0xFF8E24AA), // lila
    Color(0xFF00ACC1), // türkis
    Color(0xFFD81B60), // pink
    Color(0xFF6D4C41)  // braun
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTracksSheet(
    journeys: List<SavedJourney>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Nur Fahrten mit aufgezeichnetem GPS-Track anzeigen
    val tracked = remember(journeys) {
        journeys.filter { it.recordedGps && it.trackPoints.size >= 2 }
    }

    // Datumsfilter: null = alle Fahrten
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    val dates = remember(tracked) { tracked.map { it.date }.distinct() }
    val visible = remember(tracked, selectedDate) {
        selectedDate?.let { d -> tracked.filter { it.date == d } } ?: tracked
    }

    val mapHeight = (LocalConfiguration.current.screenHeightDp * 0.68f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.journeys_all_tracks_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.journeys_with_gps, visible.size, visible.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }

        // ── Datumsfilter ──────────────────────────────────────────────────
        if (dates.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDate == null,
                        onClick = { selectedDate = null },
                        label = { Text(stringResource(R.string.journeys_filter_all)) }
                    )
                }
                items(dates) { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = {
                            selectedDate = if (selectedDate == date) null else date
                        },
                        label = { Text(date) }
                    )
                }
            }
        }

        // ── Legende: Farbe je Fahrt ───────────────────────────────────────
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visible.size) { index ->
                val journey = visible[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 18.dp, height = 4.dp)
                            .background(
                                trackColor(tracked, journey),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                    Text(
                        text = journey.name.ifBlank {
                            "${journey.originStation} → ${journey.destinationStation}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // ── Karte ─────────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = "com.nruge.iceinfo"
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isClickable = true
                    setOnTouchListener { v, event ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        if (event.action == MotionEvent.ACTION_UP) v.performClick()
                        false
                    }
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                val allGeoPoints = mutableListOf<GeoPoint>()
                visible.forEach { journey ->
                    val geo = journey.trackPoints.map { GeoPoint(it.lat, it.lon) }
                    allGeoPoints.addAll(geo)
                    Polyline().also { p ->
                        p.setPoints(geo)
                        p.outlinePaint.color = trackColor(tracked, journey).toArgb()
                        p.outlinePaint.strokeWidth = 10f
                        p.outlinePaint.isAntiAlias = true
                        p.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        p.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        p.setOnClickListener { _, _, _ -> true } // keine InfoWindow-Popups
                        mapView.overlays.add(p)
                    }
                }
                if (allGeoPoints.isNotEmpty()) {
                    val bbox = BoundingBox.fromGeoPoints(allGeoPoints)
                    mapView.post { mapView.zoomToBoundingBox(bbox, true, 80) }
                }
                mapView.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/** Stabile Farbe je Fahrt — unabhängig vom aktiven Datumsfilter. */
private fun trackColor(tracked: List<SavedJourney>, journey: SavedJourney): Color =
    TrackPalette[tracked.indexOfFirst { it.id == journey.id }
        .coerceAtLeast(0) % TrackPalette.size]
