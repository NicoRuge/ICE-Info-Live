package com.nruge.iceinfo

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.ExperimentalComposeUiApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapCard(latitude: Double, longitude: Double) {
    android.util.Log.d("ICEMap", "MapCard aufgerufen: $latitude, $longitude")
    if (latitude == 0.0 && longitude == 0.0) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📍 Position",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            AndroidView(
                factory = { context: Context ->
                    Configuration.getInstance().userAgentValue = "com.nruge.iceinfo"
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isClickable = true
                        controller.setZoom(13.0)
                        setOnTouchListener { v, event ->
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                v.performClick()
                            }
                            false
                        }
                    }
                },
                update = { mapView ->
                    val point = GeoPoint(latitude, longitude)
                    mapView.controller.setCenter(point)

                    // Marker setzen
                    mapView.overlays.clear()
                    val marker = Marker(mapView)
                    marker.position = point
                    marker.title = "Zugposition"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                    mapView.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = "%.5f, %.5f".format(latitude, longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}