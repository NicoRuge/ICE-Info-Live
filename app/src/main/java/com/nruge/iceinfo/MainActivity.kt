package com.nruge.iceinfo

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.background
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import android.net.Uri
import androidx.core.view.WindowCompat
import com.nruge.iceinfo.ui.theme.Green20
import com.nruge.iceinfo.ui.theme.Green40
import com.nruge.iceinfo.ui.theme.Green90
import com.nruge.iceinfo.ui.theme.Grey20
import com.nruge.iceinfo.ui.theme.Grey40
import com.nruge.iceinfo.ui.theme.Grey90
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.ui.theme.AppTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.SettingsSuggest
import com.nruge.iceinfo.ui.theme.Orange20
import com.nruge.iceinfo.ui.theme.Orange40
import com.nruge.iceinfo.ui.theme.Orange90
import kotlinx.coroutines.delay


// -------------------------------------------------------------------------
// Datenmodelle
// -------------------------------------------------------------------------

data class TrainStatus(
    val distanceLastToNext: Int = 0,
    val trainType: String,
    val trainNumber: String,
    val speed: Int,
    val nextStop: String,
    val destination: String,
    val eta: String,
    val delayMinutes: Int = 0,
    val track: String = "",
    val delayReason: String = "",
    val distanceToNext: Int = 0,
    val stops: List<TrainStop> = emptyList(),
    val wagonClass: String = "",
    val connectivity: String = "",
    val tzn: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceToDestination: Int = 0,
    val destinationEta: String = "",
    val destinationTrack: String = "",
    val destinationDelay: Int = 0,
    val isConnected: Boolean = true
)

data class TrainStop(
    val name: String,
    val scheduledArrival: String,
    val actualArrival: String,
    val delayMinutes: Int,
    val track: String,
    val passed: Boolean,
    val isNext: Boolean
)

// -------------------------------------------------------------------------
// Activity
// -------------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startForegroundService(Intent(this, IceNotificationService::class.java))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            var appTheme by rememberSaveable { mutableStateOf(AppTheme.SYSTEM) }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ICEInfoTheme(appTheme = appTheme) {
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }
                var trainStatus by remember { mutableStateOf(sampleTrainStatus.copy(isConnected = false)) }
                var serviceRunning by remember { mutableStateOf(false) }
                var isMockMode by remember { mutableStateOf(false) }
                var mapVisible by remember { mutableStateOf(false) }
                var isChecking by remember { mutableStateOf(false) }
                var pois by remember { mutableStateOf(samplePois) }

                LaunchedEffect(Unit) {
                    while (true) {
                        if (!isMockMode) {
                            trainStatus = TrainRepository.fetchTrainStatus()
                            pois = TrainRepository.fetchPois()
                        }
                        delay(3000)
                    }
                }

                val context = LocalContext.current

                LaunchedEffect(isMockMode) {
                    while (!isMockMode) {
                        trainStatus = TrainRepository.fetchTrainStatus()
                        pois = TrainRepository.fetchPois()
                        delay(3000)
                    }
                }

                LaunchedEffect(isChecking) {
                    if (isChecking) {
                        trainStatus = TrainRepository.fetchTrainStatus()
                        isChecking = false
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "ICEinfo Live",
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isMockMode) {
                                        Text(
                                            text = "Demo Modus",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            fontStyle = FontStyle.Italic
                                        )
                                    } else if (trainStatus.isConnected) {
                                        Text(
                                            text = "🟢 Live",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                if(isMockMode) {
                                    IconButton(onClick = { isMockMode = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Mock-Modus"
                                        )
                                    }
                                }
                            },
                                actions = {
                                var menuExpanded by remember { mutableStateOf(false) }

                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menü"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("System Standard") },
                                        onClick = {
                                            appTheme = AppTheme.SYSTEM
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.SettingsSuggest,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Helles Theme") },
                                        onClick = {
                                            appTheme = AppTheme.LIGHT
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.LightMode,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dunkles Theme") },
                                        onClick = {
                                            appTheme = AppTheme.DARK
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DarkMode,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = serviceRunning,
                                enabled = trainStatus.isConnected,
                                onClick = {
                                    if (serviceRunning) {
                                        stopService(Intent(context, IceNotificationService::class.java))
                                        serviceRunning = false
                                    } else {
                                        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            context.startForegroundService(Intent(context, IceNotificationService::class.java))
                                            serviceRunning = true
                                        } else {
                                            (context as MainActivity).requestPermissionLauncher.launch(
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                            serviceRunning = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (serviceRunning)
                                            Icons.Default.NotificationsOff
                                        else
                                            Icons.Default.Notifications,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(if (serviceRunning) "Stop" else "Notification")
                                }
                            )
                            NavigationBarItem(
                                selected = mapVisible,
                                enabled = trainStatus.isConnected,
                                onClick = { mapVisible = !mapVisible },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Karte") }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (!trainStatus.isConnected) {
                        NoWifiScreen(
                            modifier = Modifier.padding(innerPadding),
                            onRetry = {
                                isMockMode = false
                                isChecking = true
                            },
                            onMockMode = {
                                isMockMode = true
                                trainStatus = sampleTrainStatus.copy(isConnected = true)
                            }
                        )
                    } else {
                        MainScreen(
                            status = trainStatus,
                            isDarkTheme = isDark,
                            serviceRunning = serviceRunning,
                            mapVisible = mapVisible,
                            modifier = Modifier.padding(innerPadding),
                            pois = pois
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Hilfsfunktion ICE-Typ
// -------------------------------------------------------------------------

fun getIceClass(tzn: String): String {
    val number = tzn.removePrefix("ICE").toIntOrNull() ?: return ""
    return when (number) {
        in 1..59 -> "ICE 1"
        in 60..99 -> "ICE 2"
        in 101..159 -> "ICE 1"
        in 201..299 -> "ICE T (BR 415)"
        in 301..399 -> "ICE T (BR 411)"
        in 401..499 -> "ICE 3"
        in 701..799 -> "ICE 3neo"
        in 801..899 -> "ICE 3neo"
        in 901..999 -> "ICE 4"
        else -> ""
    }
}

// -------------------------------------------------------------------------
// Hauptscreen
// -------------------------------------------------------------------------

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
            Text(text = "🚄", fontSize = 64.sp)
            Text(
                text = "Kein ICE WLAN gefunden",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Verbinde dich mit dem WLAN im Zug\nund öffne die App erneut.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("\uD83D\uDD04 Verbinden")
            }
            Spacer(modifier = Modifier.height(50.dp))
            TextButton(
                onClick = { onMockMode() }
            ) {
                Text(
                    text = "Demo-Modus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus = sampleTrainStatus,
    pois: List<PoiItem> = emptyList(),
    serviceRunning: Boolean = false,
    mapVisible: Boolean = false,
    isDarkTheme: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Titelkarte
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
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



        // Reisezusammenfassung
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Von → Nach
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${status.stops.firstOrNull()?.name ?: "—"} ➜ ${status.destination}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Fortschritt
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


                // Ankunft + verbleibende Zeit
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
                        Surface(
                            color = Green90,
                            shape = MaterialTheme.shapes.small
                        ) {
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

        // Wagenklasse + WLAN nebeneinander
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
            // Wifi Card
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

        // Karte
        if (mapVisible) {
            MapCard(
                latitude = status.latitude,
                longitude = status.longitude
            )
        }

        // Streckenverlauf
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
        }

        // Verspätungsgrund
        if (status.delayReason.isNotEmpty()) {
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
                        text = status.delayReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        //POIs
        val displayPois = if (status.isConnected && pois.isNotEmpty()) pois else samplePois
        val context = LocalContext.current

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
}



// -------------------------------------------------------------------------
// Composables
// -------------------------------------------------------------------------

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

@Composable
fun TimelineStopRow(stop: TrainStop, isFirst: Boolean, isLast: Boolean) {
    val dotColor = when {
        stop.isNext -> MaterialTheme.colorScheme.primary
        stop.passed -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outline
    }
    val lineColor = if (stop.passed)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    val textColor = when {
        stop.passed -> MaterialTheme.colorScheme.outlineVariant
        stop.isNext -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Linke Seite: Linie + Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Linie oben
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isFirst) Color.Transparent else lineColor)
            )
            // Dot
            Box(
                modifier = Modifier
                    .size(if (stop.isNext) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            // Linie unten
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isLast) Color.Transparent else
                        if (stop.passed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Rechte Seite: Inhalt
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
                textDecoration = if (stop.passed) TextDecoration.LineThrough
                else TextDecoration.None,
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

// -------------------------------------------------------------------------
// Preview
// -------------------------------------------------------------------------

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
                    ),
                    modifier = Modifier.height(56.dp)
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