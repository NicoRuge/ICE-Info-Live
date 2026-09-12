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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.model.TrackPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val TrackColorSlow   = Color(0xFFAAAAAA) // grau  < 150 km/h
private val TrackColorMedium = Color(0xFF1E88E5) // blau  150–250 km/h
private val TrackColorFast   = Color(0xFFE53935) // rot   > 250 km/h

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTrackSheet(
    journey: SavedJourney,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val colorSlow   = TrackColorSlow.toArgb()
    val colorMedium = TrackColorMedium.toArgb()
    val colorFast   = TrackColorFast.toArgb()

    // Explizite Map-Höhe: Bildschirmhöhe minus Sheet-Overhead (Header + Legende + NavBar + DragHandle)
    val mapHeight = (LocalConfiguration.current.screenHeightDp * 0.68f).dp

    // Strings für die Marker-Snippets im AndroidView-Lambda vorab auflösen
    val departureSnippet = stringResource(R.string.track_departure, journey.departureTime)
    val arrivalSnippet = stringResource(R.string.track_arrival, journey.arrivalTime)

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
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${journey.trainType} ${journey.trainNumber}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${journey.originStation} → ${journey.destinationStation}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${journey.date}  ·  " +
                        stringResource(R.string.track_gps_points, journey.trackPoints.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }

        // ── Geschwindigkeits-Legende ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedLegendChip(color = TrackColorSlow,   label = "< 150 km/h")
            SpeedLegendChip(color = TrackColorMedium, label = "150–250 km/h")
            SpeedLegendChip(color = TrackColorFast,   label = "> 250 km/h")
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
                val pts = journey.trackPoints
                if (pts.size >= 2) {
                    addSpeedPolylines(mapView, pts, colorSlow, colorMedium, colorFast)
                    val geo = pts.map { GeoPoint(it.lat, it.lon) }
                    Marker(mapView).also { m ->
                        m.position = geo.first()
                        m.title = journey.originStation
                        m.snippet = departureSnippet
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(m)
                    }
                    Marker(mapView).also { m ->
                        m.position = geo.last()
                        m.title = journey.destinationStation
                        m.snippet = arrivalSnippet
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(m)
                    }
                    val bbox = BoundingBox.fromGeoPoints(geo)
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

@Composable
private fun SpeedLegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 4.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun addSpeedPolylines(
    mapView: MapView,
    points: List<TrackPoint>,
    colorSlow: Int,
    colorMedium: Int,
    colorFast: Int
) {
    fun speedColor(kmh: Int) = when {
        kmh < 150 -> colorSlow
        kmh < 250 -> colorMedium
        else      -> colorFast
    }

    var segment = mutableListOf(GeoPoint(points[0].lat, points[0].lon))
    var curColor = speedColor(points[0].speedKmh)

    for (i in 1 until points.size) {
        val c = speedColor(points[i].speedKmh)
        // Aktuellen Punkt dem Segment hinzufügen (Überlappung verhindert Lücken)
        segment.add(GeoPoint(points[i].lat, points[i].lon))

        if (c != curColor || i == points.size - 1) {
            if (segment.size >= 2) {
                Polyline().also { p ->
                    p.setPoints(segment)
                    p.outlinePaint.color = curColor
                    p.outlinePaint.strokeWidth = 12f
                    p.outlinePaint.isAntiAlias = true
                    p.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    p.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    mapView.overlays.add(p)
                }
            }
            // Neues Segment beginnt mit aktuellem Punkt
            segment = mutableListOf(GeoPoint(points[i].lat, points[i].lon))
            curColor = c
        }
    }
}
