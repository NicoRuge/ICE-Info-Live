package com.nruge.iceinfo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.PoiItem
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.samplePois

@Composable
fun TimelineStopRow(stop: TrainStop, isFirst: Boolean, isLast: Boolean) {
    val isPassed = stop.passed
    val isNext = stop.isNext
    val isDelayed = stop.delayMinutes > 0

    val travelledLine = MaterialTheme.colorScheme.primary
    val pendingLine = MaterialTheme.colorScheme.outlineVariant

    val rowBackground = if (isNext)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else
        Color.Transparent

    val nameColor = when {
        isPassed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        isNext -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        when {
                            isFirst -> Color.Transparent
                            isPassed || isNext -> travelledLine
                            else -> pendingLine
                        }
                    )
            )
            when {
                isNext -> Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                isPassed -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(travelledLine)
                )
                else -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        when {
                            isLast -> Color.Transparent
                            isPassed -> travelledLine
                            else -> pendingLine
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(rowBackground)
                .padding(
                    vertical = if (isNext) 12.dp else 8.dp,
                    horizontal = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    color = nameColor,
                    style = if (isNext) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyMedium,
                    fontWeight = when {
                        isNext -> FontWeight.Bold
                        isPassed -> FontWeight.Normal
                        else -> FontWeight.Medium
                    }
                )
                if (stop.track.isNotEmpty()) {
                    Text(
                        text = "Gleis ${stop.track}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = if (isPassed) 0.5f else 1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isDelayed && !isPassed) {
                    Text(
                        text = stop.scheduledArrival,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = stop.actualArrival.ifEmpty { stop.scheduledArrival },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stop.scheduledArrival,
                        color = nameColor,
                        style = if (isNext) MaterialTheme.typography.titleSmall
                                else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.stops_title),
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
                Text(stringResource(R.string.stops_none))
            }
        }
        PoisCard(status = status, pois = pois)
    }

}

@Composable
fun PoisCard(status: TrainStatus, pois: List<PoiItem>) {
    val context = LocalContext.current
    val displayPois = if (status.isConnected && pois.isNotEmpty()) pois else samplePois

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pois_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!status.isConnected) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = stringResource(R.string.pois_demo),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    displayPois.forEachIndexed { index, poi ->
                        val icon = when (poi.type) {
                            "CITY" -> Icons.Default.LocationCity
                            "RIVER" -> Icons.Default.Water
                            "MOUNTAIN" -> Icons.Default.Terrain
                            "LAKE" -> Icons.Default.Water
                            "MONUMENT" -> Icons.Default.AccountBalance
                            "FOREST" -> Icons.Default.Forest
                            else -> Icons.Default.Place
                        }
                        val distanceText = if (poi.distance < 1000) {
                            "${poi.distance} m"
                        } else {
                            "${"%.1f".format(poi.distance / 1000.0)} km"
                        }

                        ListItem(
                            modifier = Modifier.clickable {
                                val query = Uri.encode(poi.name)
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=$query")
                                    )
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            overlineContent = {
                                Text(poi.type.lowercase().replaceFirstChar { it.uppercase() })
                            },
                            headlineContent = {
                                Text(poi.name, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = if (poi.description.isNotEmpty()) {
                                { Text(poi.description) }
                            } else null,
                            trailingContent = {
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(distanceText, style = MaterialTheme.typography.labelMedium) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (index < displayPois.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}