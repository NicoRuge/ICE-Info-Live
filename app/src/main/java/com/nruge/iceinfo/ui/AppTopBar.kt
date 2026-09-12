package com.nruge.iceinfo.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.util.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    isMockMode: Boolean,
    isConnected: Boolean,
    isOnTrainWifi: Boolean,
    isReconnecting: Boolean = false,
    serviceRunning: Boolean,
    showPrideBadge: Boolean = false,
    isRecording: Boolean = false,
    onSaveRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onToggleService: () -> Unit,
    onShareTrip: (() -> Unit)? = null,
    onExitDemo: () -> Unit,
    onStartDemo: () -> Unit,
    onShowSettings: () -> Unit,
    onShowInfo: () -> Unit,
    onShowChangelog: () -> Unit,
    onShowJourneys: () -> Unit,
    onShowConnections: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    showScrollDivider: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val apiUnreachable = isOnTrainWifi && !isConnected && !isMockMode
    val barContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val barContentColor = MaterialTheme.colorScheme.onSurface
    val scrolledFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
    var showPrideDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var prideFlagHidden by remember {
        mutableStateOf(SettingsManager.isPrideFlagHidden(context))
    }
    val coroutineScope = rememberCoroutineScope()

    if (showPrideDialog) {
        PrideDialog(onDismiss = { showPrideDialog = false })
    }

    Column {
        CenterAlignedTopAppBar(
        title = {
            when {
                isMockMode     -> ConnectionStatusBadge(state = ConnectionState.DEMO)
                isReconnecting -> ConnectionStatusBadge(state = ConnectionState.RECONNECTING)
                apiUnreachable -> ConnectionStatusBadge(state = ConnectionState.OFFLINE)
                isConnected    -> ConnectionStatusBadge(state = ConnectionState.LIVE)
                else           -> {}
            }
        },
        navigationIcon = {
            when {
                onNavigateBack != null -> IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                isMockMode -> IconButton(onClick = onExitDemo) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.demo_end))
                }
                else -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "ICE",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFCC0000)
                    )
                    Text(
                        text = "info",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(3.dp))
                    var holdProgress by remember { mutableFloatStateOf(0f) }
                    Image(
                        painter = painterResource(R.drawable.progressive_pride),
                        contentDescription = "Pride Flag",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 20.dp, height = 14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .graphicsLayer {
                                alpha = if (prideFlagHidden) holdProgress.coerceAtLeast(0.08f)
                                        else 1f - holdProgress * 0.6f
                            }
                            .pointerInput(prideFlagHidden) {
                                detectTapGestures(
                                    onTap = { if (!prideFlagHidden) showPrideDialog = true },
                                    onPress = {
                                        val job = coroutineScope.launch {
                                            repeat(50) { i ->
                                                holdProgress = (i + 1) / 50f
                                                delay(100L)
                                            }
                                            val newHidden = !prideFlagHidden
                                            prideFlagHidden = newHidden
                                            SettingsManager.setPrideFlagHidden(context, newHidden)
                                            holdProgress = 0f
                                        }
                                        tryAwaitRelease()
                                        job.cancel()
                                        holdProgress = 0f
                                    }
                                )
                            }
                    )
                }
            }
        },
        actions = {
            var menuExpanded by remember { mutableStateOf(false) }

            if (isRecording) {
                RecordingAction(onSave = onSaveRecording, onCancel = onCancelRecording)
            }
            if (isConnected || isMockMode) {
                IconButton(onClick = onToggleService) {
                    Icon(
                        imageVector = if (serviceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications_cd)
                    )
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_cd))
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                if ((isConnected || isMockMode) && onShareTrip != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_trip_cd)) },
                        onClick = { onShareTrip(); menuExpanded = false },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                }
                if (!isMockMode) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.demo_mode)) },
                        onClick = { onStartDemo(); menuExpanded = false },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_journeys_menu)) },
                    onClick = { onShowJourneys(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.History, null) }
                )
                // Anschlüsse: wieder als Bottom-Tab, daher hier kein Menüeintrag mehr
                // (Overlay-Mechanik in MainActivity bleibt für einfaches Zurückwechseln erhalten).
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_settings)) },
                    onClick = { onShowSettings(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_changelog)) },
                    onClick = { onShowChangelog(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.NewReleases, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_info)) },
                    onClick = { onShowInfo(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = barContainerColor,
            scrolledContainerColor = barContainerColor,
            titleContentColor = barContentColor,
            navigationIconContentColor = barContentColor,
            actionIconContentColor = barContentColor
        ),
        scrollBehavior = scrollBehavior
    )
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (showScrollDivider) scrolledFraction else 0f)
    )
    } // Column
}

/** Pulsierender Aufnahme-Punkt im Header; Tap öffnet Speichern/Abbrechen. */
@Composable
private fun RecordingAction(onSave: () -> Unit, onCancel: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "recordPulse"
    )

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            icon = {
                Icon(
                    Icons.Default.StopCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.recording_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.recording_cancel_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { showCancelConfirm = false; onCancel() }) {
                    Text(
                        stringResource(R.string.recording_cancel_confirm_discard),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.recording_cancel_confirm_keep))
                }
            }
        )
    }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = stringResource(R.string.recording_active_cd),
                tint = MaterialTheme.colorScheme.error.copy(alpha = alpha),
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recording_save_now)) },
                onClick = { menuExpanded = false; onSave() },
                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recording_cancel)) },
                onClick = { menuExpanded = false; showCancelConfirm = true },
                leadingIcon = {
                    Icon(
                        Icons.Default.StopCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

private enum class ConnectionState { LIVE, DEMO, RECONNECTING, OFFLINE }

@Composable
private fun ConnectionStatusBadge(state: ConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "connection")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        when (state) {
            ConnectionState.LIVE -> {
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        tween(1200), RepeatMode.Reverse
                    ), label = "livePulse"
                )
                val dotColor = Color(0xFF4CAF50)
                Canvas(modifier = Modifier.size(11.dp)) {
                    drawCircle(color = dotColor.copy(alpha = alpha))
                }
                Text(
                    text = "Live",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ConnectionState.DEMO -> {
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        tween(1400), RepeatMode.Reverse
                    ), label = "demoPulse"
                )
                val dotColor = Color(0xFFAB47BC)
                Canvas(modifier = Modifier.size(11.dp)) {
                    drawCircle(color = dotColor.copy(alpha = alpha))
                }
                Text(
                    text = stringResource(R.string.status_demo),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAB47BC)
                )
            }
            ConnectionState.RECONNECTING -> {
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        tween(1000, easing = LinearEasing), RepeatMode.Restart
                    ), label = "spin"
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                        .size(11.dp)
                        .rotate(rotation),
                    tint = Color(0xFFFFA726)
                )
                Text(
                    text = stringResource(R.string.status_reconnecting),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFA726)
                )
            }
            ConnectionState.OFFLINE -> {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.status_api_unreachable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
        },
        title = {
            Text(
                text = "Für Vielfalt und Toleranz, gegen Hass und Hetze",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Diese App ist für alle Menschen - unabhängig von Herkunft, Identität oder wen sie lieben. Aber nicht für dich, wenn du damit ein Problem hast. Gute Reise!",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Slay ✨")
            }
        }
    )
}

@Composable
fun AppNavigationBar(
    currentRoute: String?,
    enabled: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        NavigationBar {
            navigationItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { if (!isSelected) onNavigate(screen.route) },
                    enabled = enabled,
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = stringResource(screen.labelRes)
                        )
                    },
                    label = { Text(stringResource(screen.labelRes)) }
                )
            }
        }
    }
}
