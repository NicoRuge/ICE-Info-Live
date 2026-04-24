package com.nruge.iceinfo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.model.PoiItem
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop

@Composable
fun TimelineStopRow(stop: TrainStop, isFirst: Boolean, isLast: Boolean) {
    val dotColor = when {
        stop.isNext -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val lineColor = if (stop.passed)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textColor = when {
        stop.isNext -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isFirst) Color.Transparent else lineColor)
            )
            Box(
                modifier = Modifier
                    .size(if (stop.isNext) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        if (isLast) Color.Transparent
                        else if (stop.passed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stop.name,
                color = textColor,
                fontWeight = if (stop.isNext) FontWeight.Bold else FontWeight.Normal,
                style = if (stop.isNext) MaterialTheme.typography.bodyLarge
                else MaterialTheme.typography.bodyMedium,
                textDecoration = if (stop.passed) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stop.track.isNotEmpty()) {
                    Text(
                        text = "Gl.${stop.track}",
                        color = textColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = stop.scheduledArrival,
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (stop.isNext) FontWeight.SemiBold else FontWeight.Normal
                )
                if (stop.delayMinutes > 0 && !stop.passed) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "+${stop.delayMinutes}",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun StopsScreen(
    status: TrainStatus,
    modifier: Modifier = Modifier,
    pois: List<PoiItem> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (status.stops.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "🛤️ Streckenverlauf",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    status.stops.forEachIndexed { index, stop ->
                        TimelineStopRow(
                            stop = stop,
                            isFirst = index == 0,
                            isLast = index == status.stops.lastIndex
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Keine Halteinformationen verfügbar.")
            }
        }
        PoisCard(status = status, pois = pois)
    }

}

@Composable
fun PoisCard(status: TrainStatus, pois: List<PoiItem>) {
    val context = LocalContext.current
    val displayPois = if (status.isConnected && pois.isNotEmpty()) pois else samplePois
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏔️ Points of Interest",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!status.isConnected) {
                    Text(
                        text = "Demo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            displayPois.forEachIndexed { index, poi ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val query = Uri.encode(poi.name)
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/search?q=$query")
                            )
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (poi.type) {
                                "CITY" -> "🏙️"
                                "RIVER" -> "🌊"
                                "MOUNTAIN" -> "⛰️"
                                "LAKE" -> "💧"
                                "MONUMENT" -> "🏛️"
                                else -> "📍"
                            }
                        )
                        Column {
                            Text(
                                text = poi.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (poi.description.isNotEmpty()) {
                                Text(
                                    text = poi.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Text(
                        text = "${poi.distance / 1000} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (index < displayPois.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}