package com.nruge.iceinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus = sampleTrainStatus,
    isDarkTheme: Boolean = false,
    isMockMode: Boolean = false,
    demoSpeed: Int = 114,
    showDemoSpeed: Boolean = true,
    onDemoSpeedChange: (Int) -> Unit = {},
    onTargetStopChange: (String?) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrainHeader(status = status)

        StopSelectionCard(
            status = status,
            onTargetStopChange = onTargetStopChange
        )
        TravelSummaryCard(status = status)

        ConnectivityRow(status = status, isDarkTheme = isDarkTheme)

        if (status.delayReason.isNotEmpty()) {
            DelayReasonCard(reason = status.delayReason)
        }
        if (isMockMode && showDemoSpeed) {
            DemoSpeedCard(demoSpeed = demoSpeed, onDemoSpeedChange = onDemoSpeedChange)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopSelectionCard(
    status: TrainStatus,
    onTargetStopChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val stops = status.stops.filter { !it.passed }
    val currentTarget = stops.find { it.evaNr == status.targetStopEva }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Dein Ausstieg",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            text = currentTarget?.name ?: "Kein Ausstieg gewählt",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.large
                ) {
                    DropdownMenuItem(
                        text = { Text("Kein Ausstieg gewählt") },
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
        }
    }
}

@Composable
private fun DemoSpeedCard(demoSpeed: Int, onDemoSpeedChange: (Int) -> Unit) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.demo_speed_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$demoSpeed km/h",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (isExpanded) {
                Slider(
                    value = demoSpeed.toFloat(),
                    onValueChange = { onDemoSpeedChange(it.toInt()) },
                    valueRange = 0f..300f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                        activeTickColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        inactiveTickColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "0 km/h", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "150 km/h", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "300 km/h", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun FullAppPreview() {
    ICEInfoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ICEinfo", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    var expanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.LeadingButton(
                                    onClick = { /* Notification action */ }
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Benachrichtigung")
                                }
                            },
                            trailingButton = {
                                SplitButtonDefaults.TrailingButton(
                                    checked = expanded,
                                    onCheckedChange = { expanded = it }
                                ) {
                                    Icon(
                                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Mehr Optionen",
                                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize)
                                    )
                                }
                            }
                        )

                        if (expanded) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Option 1") },
                                    onClick = { expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Option 2") },
                                    onClick = { expanded = false }
                                )
                            }
                        }
                    }

                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Map, null) },
                        label = { Text("Karte") }
                    )
                }
            }
        ) { innerPadding ->
            HomeScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
