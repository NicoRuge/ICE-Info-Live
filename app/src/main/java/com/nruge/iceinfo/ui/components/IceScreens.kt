package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.nruge.iceinfo.R
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.ui.theme.*
import com.nruge.iceinfo.util.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import  androidx.compose.runtime.key

@Composable
fun NoWifiScreen(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onMockMode: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ice),
                contentDescription = "ICE",
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-50).dp)
            )
            Text(
                text = "Kein ICE WLAN gefunden",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Verbinde dich mit \"WIFIonICE\"\nund tippe auf Verbinden.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("🔄 Verbinden")
            }
            Spacer(modifier = Modifier.height(50.dp))
            TextButton(onClick = onMockMode) {
                Text(
                    text = "Demo-Modus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FullAppPreview() {
    ICEInfoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ICEinfo Live", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Notifications, null) },
                        label = { Text("Notification") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Map, null) },
                        label = { Text("Karte") }
                    )
                }
            }
        ) { innerPadding ->
            MainScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus = sampleTrainStatus,
    isDarkTheme: Boolean = false,
    isMockMode: Boolean = false,
    demoSpeed: Int = 114,
    onDemoSpeedChange: (Int) -> Unit = {}
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
        Spacer(modifier = Modifier.height(10.dp))
        TrainHeader(status = status)
        if (isMockMode) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🎮 Demo-Geschwindigkeit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$demoSpeed km/h",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = demoSpeed.toFloat(),
                        onValueChange = { onDemoSpeedChange(it.toInt()) },
                        valueRange = 0f..300f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 km/h", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                        Text("150 km/h", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                        Text("300 km/h", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        TravelSummaryCard(status = status)
        ConnectivityRow(status = status, isDarkTheme = isDarkTheme)
        
        if (status.delayReason.isNotEmpty()) {
            DelayReasonCard(reason = status.delayReason)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun TrainHeader(status: TrainStatus) {
    val density = LocalDensity.current
    var trackWidth by remember { mutableStateOf(300f) }

    // Geschwindigkeit auf 10er runden damit Animation nicht bei jedem km/h neu startet
    val speedBucket = (status.speed / 10) * 10

    // Animationsdauer basierend auf Geschwindigkeit
    // Bei 0 km/h = sehr langsam, bei 300 km/h = sehr schnell
    val animDuration = if (status.speed > 0) {
        (300000 / status.speed.coerceAtLeast(1)).coerceIn(500, 8000)
    } else {
        8000
    }

    key(speedBucket) {
        val infiniteTransition = rememberInfiniteTransition(label = "tracks")

        val targetValue = if (status.speed > 0) -trackWidth / density.density else 0f

        val trackOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(animDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "trackScroll"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .graphicsLayer { clip = false }
        ) {
            // Schienen animiert scrollend
            Row(
                modifier = Modifier
                    .height(70.dp)
                    .wrapContentWidth(unbounded = true, align = Alignment.Start)
                    .align(Alignment.CenterStart)
                    .offset(x = (trackOffset - 17).dp, y = (-55).dp)
                    .zIndex(1f)
                    .onGloballyPositioned { coords ->
                        trackWidth = coords.size.width / 5f
                    }
            ) {
                repeat(5) {
                    Image(
                        painter = painterResource(id = R.drawable.traintracks),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .height(70.dp)
                            .wrapContentWidth(unbounded = true)
                    )
                }
            }

            // Zug darüber
            Image(
                painter = painterResource(id = getIceDrawable(status.tzn)),
                contentDescription = null,
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .height(63.dp)
                    .wrapContentWidth(unbounded = true, align = Alignment.Start)
                    .align(Alignment.CenterStart)
                    .offset(x = (-250).dp, y = (-54).dp)
                    .zIndex(2f)
                    .graphicsLayer { clip = false }
            )

            // Titelkarte
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${status.trainType} ${status.trainNumber}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = getIceClass(status.tzn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${status.speed} km/h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}


@Composable
fun TravelSummaryCard(status: TrainStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${status.stops.firstOrNull()?.name ?: "—"} ➜ ${status.destination}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            val totalStops = status.stops.size
            val passedStops = status.stops.count { it.passed }
            val progressPercent = if (totalStops > 0)
                (passedStops.toFloat() / totalStops * 100).toInt() else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📏 ${status.distanceToDestination / 1000} km verbleibend",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$passedStops von $totalStops Halten",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "⏱️ Ankunft ${status.destinationEta}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val remainingMinutes = if (status.speed > 0)
                        (status.distanceToDestination / 1000f / status.speed * 60).toInt() else 0
                    val hours = remainingMinutes / 60
                    val minutes = remainingMinutes % 60
                    Text(
                        text = "ca. ${if (hours > 0) "${hours}h " else ""}${minutes}min verbleibend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (status.destinationDelay > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "+${status.destinationDelay} min",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(color = Green90, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "pünktlich",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Green40,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivityRow(status: TrainStatus, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🪑 Wagenklasse",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = when (status.wagonClass) {
                        "FIRST" -> "1"
                        "SECOND" -> "2"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = when (status.connectivity) {
                    "STRONG" -> if (isDarkTheme) Green20 else Green90
                    "WEAK" -> if (isDarkTheme) Orange20 else Orange90
                    "NO_CONNECTION" -> MaterialTheme.colorScheme.errorContainer
                    else -> if (isDarkTheme) Grey20 else Grey90
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📶 WLAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = when (status.connectivity) {
                        "STRONG" -> if (isDarkTheme) Green90 else Green40
                        "WEAK" -> if (isDarkTheme) Orange90 else Orange40
                        "NO_CONNECTION" -> MaterialTheme.colorScheme.error
                        else -> if (isDarkTheme) Grey90 else Grey40
                    }
                )
                Text(
                    text = when (status.connectivity) {
                        "STRONG" -> "Stark"
                        "WEAK" -> "Schwach"
                        "NO_INFO" -> "Keine Info"
                        "NO_CONNECTION" -> "Kein Signal"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status.connectivity) {
                        "STRONG" -> if (isDarkTheme) Green90 else Green40
                        "WEAK" -> if (isDarkTheme) Orange90 else Orange40
                        "NO_CONNECTION" -> MaterialTheme.colorScheme.error
                        else -> if (isDarkTheme) Grey90 else Grey40
                    }
                )
            }
        }
    }
}

@Composable
fun DelayReasonCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚠️")
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}



@Composable
fun MapScreen(status: TrainStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        MapCard(
            latitude = status.latitude,
            longitude = status.longitude
        )
    }
}



@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}


