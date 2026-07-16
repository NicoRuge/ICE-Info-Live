package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.WeatherInfo
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopSelectionCard(
    status: TrainStatus,
    weather: WeatherInfo?,
    onTargetStopChange: (String?) -> Unit,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now()
) {
    var expanded by remember { mutableStateOf(false) }
    val stops = status.stops.filter { !it.passed && !it.isCancelled }
    val currentTarget = stops.find { it.evaNr == status.targetStopEva }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_target_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (currentTarget != null && currentTarget.scheduledArrival.isNotEmpty()) {
                    ArrivalTimeToggle(
                        scheduled = currentTarget.scheduledArrival,
                        actual = currentTarget.actualArrival,
                        delay = currentTarget.delayMinutes,
                        showRelative = showRelative,
                        referenceTime = referenceTime,
                        style = MaterialTheme.typography.titleMedium,
                        scheduledStyle = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Train, contentDescription = null)
                        Text(
                            text = currentTarget?.name ?: stringResource(R.string.home_no_target),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_no_target)) },
                        leadingIcon = {
                            if (currentTarget == null) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onTargetStopChange(null)
                            expanded = false
                        }
                    )
                    stops.forEach { stop ->
                        val isSelected = stop.evaNr == status.targetStopEva
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stop.name)
                                    Text(
                                        stop.scheduledArrival,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                onTargetStopChange(stop.evaNr)
                                expanded = false
                            }
                        )
                    }
                }
            }
            val labelText = if (currentTarget != null) {
                val targetIndex = stops.indexOf(currentTarget)
                if (targetIndex == 0) {
                    stringResource(R.string.travel_next_stop_exit)
                } else {
                    stringResource(R.string.travel_remaining_stops_target, targetIndex)
                }
            } else {
                val totalStops = status.stops.size
                val passedStops = status.stops.count { it.passed }
                stringResource(R.string.travel_stops_progress, passedStops, totalStops)
            }

            Text(
                text = labelText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (weather != null) {
                WeatherRow(weather = weather)
            }
        }
    }
}

@Composable
private fun WeatherRow(weather: WeatherInfo) {
    val jacket = weather.jacketRecommendation
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = weatherCodeToIcon(weather.weatherCode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "${weather.temperature.roundToInt()}°C",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (weather.windspeed >= 15) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${weather.windspeed.roundToInt()} km/h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (jacket != WeatherInfo.JacketType.NONE) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = stringResource(jacket.labelResId),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            Text(
                text = stringResource(jacket.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun weatherCodeToIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2 -> Icons.Default.WbCloudy
    in 45..48 -> Icons.Default.Cloud
    in 51..57 -> Icons.Default.WaterDrop
    in 61..67 -> Icons.Default.Umbrella
    in 71..77 -> Icons.Default.AcUnit
    in 80..82 -> Icons.Default.Umbrella
    in 85..86 -> Icons.Default.AcUnit
    in 95..99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.Cloud
}
