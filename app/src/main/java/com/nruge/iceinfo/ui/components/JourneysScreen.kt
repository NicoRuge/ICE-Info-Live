package com.nruge.iceinfo.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.LiveRecordingState
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.BuildConfig
import com.nruge.iceinfo.StatsRepository
import com.nruge.iceinfo.util.GpxExporter
import com.nruge.iceinfo.util.SettingsManager
import com.nruge.iceinfo.util.IceUtils
import com.nruge.iceinfo.util.openInAppBrowser
import com.nruge.iceinfo.util.shareLink
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun JourneysScreen(
    journeys: List<SavedJourney>,
    onDeleteJourney: (String) -> Unit,
    onUpdateJourney: (SavedJourney) -> Unit = {},
    isConnected: Boolean = false,
    isRecording: Boolean = false,
    liveRecording: LiveRecordingState? = null,
    onStartRecording: () -> Unit = {},
    onExportJourneys: (Uri, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onImportJourneys: (Uri, (Int) -> Unit) -> Unit = { _, _ -> },
    // false im Pager-Tab: dort liegt der FAB fix über dem Pager (AppNavigation);
    // true im "Meine Fahrten"-Overlay, das nicht mitwischt
    showRecordFab: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportJourneys(uri) { ok ->
            Toast.makeText(
                context,
                resources.getString(if (ok) R.string.journeys_export_done else R.string.journeys_export_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportJourneys(uri) { added ->
            val message = when {
                added < 0 -> resources.getString(R.string.journeys_import_invalid)
                added == 0 -> resources.getString(R.string.journeys_import_none)
                else -> resources.getQuantityString(R.plurals.journeys_imported, added, added)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    // JSON-Dateien werden je nach Quelle als text/* oder octet-stream gemeldet,
    // deshalb keine Mime-Einschränkung im Picker.
    val launchImport = { importLauncher.launch(arrayOf("*/*")) }
    val launchExport = {
        exportLauncher.launch("ICE-Info_Fahrten_${LocalDate.now()}.json")
    }

    val hasTracks = journeys.any { it.recordedGps && it.trackPoints.size >= 2 }
    var showAllTracks by remember { mutableStateOf(false) }
    if (showAllTracks) {
        AllTracksSheet(
            journeys = journeys,
            onDismiss = { showAllTracks = false }
        )
    }
    var showStats by remember { mutableStateOf(false) }
    if (showStats) {
        JourneyStatsSheet(
            journeys = journeys,
            onDismiss = { showStats = false }
        )
    }

    // Einmaliger Hinweis auf das Fahrten-Teilen — nur wenn mind. eine Fahrt existiert
    var showStatsHint by remember { mutableStateOf(false) }
    LaunchedEffect(journeys.isNotEmpty()) {
        if (journeys.isNotEmpty() && !SettingsManager.isStatsHintShown(context)) {
            showStatsHint = true
        }
    }
    if (showStatsHint) {
        val dismissHint = {
            showStatsHint = false
            SettingsManager.setStatsHintShown(context)
        }
        AlertDialog(
            onDismissRequest = dismissHint,
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
            title = { Text(stringResource(R.string.stats_hint_title)) },
            text = { Text(stringResource(R.string.stats_hint_text)) },
            confirmButton = {
                TextButton(onClick = dismissHint) {
                    Text(stringResource(R.string.stats_hint_confirm))
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = journeys.isEmpty() && liveRecording == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            JourneysEmptyState(
                onImport = launchImport,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = journeys.isNotEmpty() || liveRecording != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                JourneysHeader(
                    journeyCount = journeys.size,
                    hasTracks = hasTracks,
                    hasJourneys = journeys.isNotEmpty(),
                    onShowAllTracks = { showAllTracks = true },
                    onShowStats = { showStats = true },
                    onImport = launchImport,
                    onExport = launchExport
                )
                // Scroll-Trennlinie an der echten Abrisskante (unter den Header-Buttons);
                // die TopBar-Variante ist für diesen Screen deaktiviert (MainActivity).
                val listState = rememberLazyListState()
                val dividerAlpha by animateFloatAsState(
                    targetValue = if (listState.canScrollBackward) 1f else 0f,
                    label = "journeysScrollDivider"
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        bottom = if (isConnected) 88.dp else 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (liveRecording != null) {
                        item(key = "live") {
                            LiveJourneyCard(live = liveRecording)
                        }
                    }
                    items(journeys, key = { it.id }) { journey ->
                        JourneyCard(
                            journey = journey,
                            onDelete = { onDeleteJourney(journey.id) },
                            onUpdate = onUpdateJourney
                        )
                    }
                }
            }
        }

        // FAB: nur sichtbar wenn verbunden und noch nicht aufzeichnend
        if (showRecordFab && isConnected && !isRecording) {
            ExtendedFloatingActionButton(
                onClick = onStartRecording,
                icon = {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                },
                text = { Text(stringResource(R.string.record_fab)) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JourneysHeader(
    journeyCount: Int,
    hasTracks: Boolean,
    hasJourneys: Boolean,
    onShowAllTracks: () -> Unit,
    onShowStats: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.journeys_count, journeyCount, journeyCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HeaderActionButton(
                icon = Icons.Default.BarChart,
                label = stringResource(R.string.journeys_stats),
                enabled = hasJourneys,
                onClick = onShowStats
            )
            HeaderActionButton(
                icon = Icons.Default.Map,
                label = stringResource(R.string.journeys_map_all_short),
                enabled = hasTracks,
                onClick = onShowAllTracks
            )
            HeaderActionButton(
                icon = Icons.Default.FileDownload,
                label = stringResource(R.string.journeys_import),
                onClick = onImport
            )
            HeaderActionButton(
                icon = Icons.Default.FileUpload,
                label = stringResource(R.string.journeys_export),
                onClick = onExport
            )
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun JourneysEmptyState(
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.HistoryToggleOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.journeys_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.journeys_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onImport) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.journeys_import))
        }
    }
}

@Composable
private fun JourneyCard(
    journey: SavedJourney,
    onDelete: () -> Unit,
    onUpdate: (SavedJourney) -> Unit = {}
) {
    var showDetail by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTrackMap by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showShareStatsDialog by remember { mutableStateOf(false) }
    var isSharingStats by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val linkChooser = stringResource(R.string.journeys_share_link_chooser)

    if (showDetail) {
        JourneyDetailSheet(
            journey = journey,
            onDismiss = { showDetail = false },
            onEdit = { showDetail = false; showEditDialog = true },
            onDelete = { showDetail = false; showDeleteDialog = true },
            onShowTrack = { showTrackMap = true },
            onShareGpx = { GpxExporter.shareGpx(context, journey) },
            onShareStats = { showShareStatsDialog = true },
            onOpenLink = { openInAppBrowser(context, StatsRepository.journeyUrl(journey.sharedHash)) },
            onShareLink = { shareLink(context, StatsRepository.journeyUrl(journey.sharedHash), linkChooser) }
        )
    }

    if (showShareStatsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSharingStats) showShareStatsDialog = false },
            title = { Text(stringResource(R.string.journeys_share_stats_title)) },
            text = {
                val installId = remember { SettingsManager.getInstallId(context) }
                // Vollständige Liste aller an die API übertragenen Felder
                val dataBlock = buildList {
                    add("${journey.trainType} ${journey.trainNumber} · ${journey.date}")
                    add("${journey.originStation} → ${journey.destinationStation}")
                    add(stringResource(R.string.journeys_share_stats_times, journey.departureTime, journey.arrivalTime))
                    add(stringResource(R.string.journeys_share_stats_delay, journey.delayMinutes))
                    // Endbahnhof-Verspätung nur zeigen, wenn der Nutzer vorher ausstieg
                    // (sonst ist sie identisch mit der Ausstiegsverspätung oben).
                    if (journey.finalDelayIsPrognosis && journey.finalStation.isNotBlank()) {
                        add(stringResource(R.string.journeys_share_stats_final, journey.finalStation, journey.finalDelayMinutes))
                    }
                    add(stringResource(R.string.journeys_share_stats_metrics, journey.durationMinutes, journey.distanceKm, journey.stopsCount))
                    add(
                        stringResource(
                            R.string.journeys_share_stats_vehicle,
                            journey.series.ifBlank { "–" },
                            journey.tzn.ifBlank { "–" }
                        )
                    )
                    if (journey.stops.isNotEmpty()) {
                        add(stringResource(R.string.journeys_share_stats_course, journey.stops.size))
                    }
                    add(stringResource(R.string.journeys_share_stats_meta, BuildConfig.VERSION_NAME, installId))
                }.joinToString("\n")
                Text(stringResource(R.string.journeys_share_stats_text, dataBlock))
            },
            confirmButton = {
                TextButton(
                    enabled = !isSharingStats,
                    onClick = {
                        scope.launch {
                            isSharingStats = true
                            val hash = StatsRepository.shareJourney(context, journey)
                            isSharingStats = false
                            showShareStatsDialog = false
                            if (hash != null) onUpdate(journey.copy(shared = true, sharedHash = hash))
                            Toast.makeText(
                                context,
                                if (hash != null) R.string.journeys_share_stats_done else R.string.journeys_share_stats_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(if (isSharingStats) R.string.journeys_share_stats_sending else R.string.journeys_share_stats_confirm))
                }
            },
            dismissButton = {
                TextButton(enabled = !isSharingStats, onClick = { showShareStatsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDialog) {
        EditJourneyDialog(
            journey = journey,
            onConfirm = { updated -> showEditDialog = false; onUpdate(updated) },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showTrackMap && journey.recordedGps && journey.trackPoints.isNotEmpty()) {
        JourneyTrackSheet(
            journey = journey,
            onDismiss = { showTrackMap = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.journeys_delete_title)) },
            text = { Text("${journey.trainType} ${journey.trainNumber} · ${journey.date}\n${journey.originStation} → ${journey.destinationStation}") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Surface(
        onClick = { showDetail = true },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: train + date + status glyphs
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = journey.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (journey.recordedGps && journey.trackPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = stringResource(R.string.journeys_open_track_cd),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (journey.shared) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = stringResource(R.string.journeys_shared_stats_cd),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Vom Nutzer vergebener Name
                if (journey.name.isNotBlank()) {
                    Text(
                        text = journey.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Route: Origin → Destination
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = journey.originStation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = journey.destinationStation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Times row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeLabel(label = stringResource(R.string.time_dep_short), time = journey.departureTime)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    TimeLabel(label = stringResource(R.string.time_arr_short), time = journey.arrivalTime)
                    if (journey.delayMinutes > 0) {
                        DelayBadge(delayMinutes = journey.delayMinutes)
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.journeys_on_time),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatDuration(journey.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun JourneyDetailSheet(
    journey: SavedJourney,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowTrack: () -> Unit,
    onShareGpx: () -> Unit,
    onShareStats: () -> Unit,
    onOpenLink: () -> Unit,
    onShareLink: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: train + date + status glyphs
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = journey.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                if (journey.shared) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = stringResource(R.string.journeys_shared_stats_cd),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Vom Nutzer vergebener Name
            if (journey.name.isNotBlank()) {
                Text(
                    text = journey.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Route: Origin → Destination
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = journey.originStation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = journey.destinationStation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Times row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeLabel(label = stringResource(R.string.time_dep_short), time = journey.departureTime)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                TimeLabel(label = stringResource(R.string.time_arr_short), time = journey.arrivalTime)
                if (journey.delayMinutes > 0) {
                    DelayBadge(delayMinutes = journey.delayMinutes)
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.journeys_on_time),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDuration(journey.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Stats row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (journey.distanceKm > 0) {
                    StatChip(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = "${journey.distanceKm} km"
                    )
                }
                if (journey.avgSpeedKmh > 0) {
                    StatChip(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = "Ø ${journey.avgSpeedKmh} km/h"
                    )
                }
                if (journey.topSpeedKmh > 0) {
                    StatChip(
                        icon = null,
                        label = "↑ ${journey.topSpeedKmh} km/h"
                    )
                }
                // Baureihe / Taufname (automatisch erfasst)
                val iceClass = IceUtils.getIceClassFromSeries(journey.series, journey.tzn.ifBlank { null })
                val tzName = journey.tzn.takeIf { it.isNotBlank() }
                    ?.let { IceUtils.getTzName(it)?.name }
                if (iceClass.isNotBlank() || tzName != null) {
                    StatChip(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = listOfNotNull(iceClass.ifBlank { null }, tzName?.let { "„$it“" })
                            .joinToString(" · ")
                    )
                }
            }

            // Vom Nutzer gepflegte Zusatzinfos
            val hasMeta = journey.purpose.isNotBlank() || journey.ticketType.isNotBlank() ||
                journey.price.isNotBlank() || journey.seat.isNotBlank()
            if (hasMeta) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (journey.purpose.isNotBlank()) {
                        StatChip(
                            icon = { Icon(Icons.AutoMirrored.Filled.Label, null, Modifier.size(14.dp)) },
                            label = journey.purpose
                        )
                    }
                    if (journey.ticketType.isNotBlank()) {
                        StatChip(
                            icon = { Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(14.dp)) },
                            label = journey.ticketType
                        )
                    }
                    if (journey.price.isNotBlank()) {
                        StatChip(
                            icon = { Icon(Icons.Default.Euro, null, Modifier.size(14.dp)) },
                            label = journey.price
                        )
                    }
                    if (journey.seat.isNotBlank()) {
                        StatChip(
                            icon = { Icon(Icons.Default.AirlineSeatReclineNormal, null, Modifier.size(14.dp)) },
                            label = journey.seat
                        )
                    }
                }
            }
            if (journey.notes.isNotBlank()) {
                Text(
                    text = journey.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Aktionen
            Column {
                if (journey.recordedGps && journey.trackPoints.isNotEmpty()) {
                    DetailActionRow(
                        icon = Icons.Default.GpsFixed,
                        label = stringResource(R.string.journeys_open_track_cd),
                        onClick = onShowTrack
                    )
                    DetailActionRow(
                        icon = Icons.Default.Share,
                        label = stringResource(R.string.journeys_menu_share_gpx),
                        onClick = onShareGpx
                    )
                }
                if (journey.shared && journey.sharedHash.isNotBlank()) {
                    DetailActionRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = stringResource(R.string.journeys_menu_open_link),
                        onClick = onOpenLink
                    )
                    DetailActionRow(
                        icon = Icons.Default.Link,
                        label = stringResource(R.string.journeys_menu_share_link),
                        onClick = onShareLink
                    )
                } else {
                    DetailActionRow(
                        icon = Icons.Default.CloudUpload,
                        label = stringResource(R.string.journeys_menu_upload),
                        onClick = onShareStats
                    )
                }
                DetailActionRow(
                    icon = Icons.Default.Edit,
                    label = stringResource(R.string.journeys_edit),
                    onClick = onEdit
                )
                DetailActionRow(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.delete),
                    onClick = onDelete,
                    destructive = true
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DetailActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditJourneyDialog(
    journey: SavedJourney,
    onConfirm: (SavedJourney) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(journey.name) }
    var purpose by remember { mutableStateOf(journey.purpose) }
    var ticketType by remember { mutableStateOf(journey.ticketType) }
    var price by remember { mutableStateOf(journey.price) }
    var seat by remember { mutableStateOf(journey.seat) }
    var notes by remember { mutableStateOf(journey.notes) }
    var series by remember { mutableStateOf(journey.series) }
    var seriesMenuExpanded by remember { mutableStateOf(false) }

    // Anzeige-Label der aktuell gewählten Baureihe (fällt auf die Tz-Ableitung zurück)
    val seriesLabel = IceUtils.getIceClassFromSeries(series, journey.tzn.ifBlank { null })
        .ifBlank { stringResource(R.string.journeys_series_none) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.journeys_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.journeys_name_label)) },
                    placeholder = { Text(stringResource(R.string.journeys_name_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.journeys_purpose_label)) },
                    placeholder = { Text(stringResource(R.string.journeys_purpose_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ticketType,
                    onValueChange = { ticketType = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.journeys_ticket_label)) },
                    placeholder = { Text(stringResource(R.string.journeys_ticket_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = seriesMenuExpanded,
                    onExpandedChange = { seriesMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = seriesLabel,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.journeys_series_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = seriesMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = seriesMenuExpanded,
                        onDismissRequest = { seriesMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.journeys_series_none)) },
                            onClick = { series = ""; seriesMenuExpanded = false }
                        )
                        IceUtils.allSeries().forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.journeys_series_item, label, code)) },
                                onClick = { series = code; seriesMenuExpanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.journeys_price_label)) },
                        placeholder = { Text(stringResource(R.string.journeys_price_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seat,
                        onValueChange = { seat = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.journeys_seat_label)) },
                        placeholder = { Text(stringResource(R.string.journeys_seat_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(stringResource(R.string.journeys_notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    journey.copy(
                        name = name.trim(),
                        purpose = purpose.trim(),
                        ticketType = ticketType.trim(),
                        price = price.trim(),
                        seat = seat.trim(),
                        notes = notes.trim(),
                        series = series
                    )
                )
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TimeLabel(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatChip(
    icon: (@Composable () -> Unit)?,
    label: String
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}min" else "${m}min"
}

private fun formatElapsed(startMs: Long, nowMs: Long): String {
    val totalSec = ((nowMs - startMs) / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m"
    else "${m.toString().padStart(2, '0')}m ${s.toString().padStart(2, '0')}s"
}

@Composable
private fun LiveJourneyCard(live: LiveRecordingState) {
    // Sekundengenauer Ticker
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    // Pulsierender Dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsierender LIVE-Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = dotAlpha),
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "REC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${live.trainType} ${live.trainNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = live.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Route
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = live.originStation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = live.destinationStation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            // Live-Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveStatChip(
                    label = stringResource(R.string.live_elapsed),
                    value = formatElapsed(live.startMs, nowMs),
                    modifier = Modifier.weight(1f)
                )
                LiveStatChip(
                    label = stringResource(R.string.live_speed_current),
                    value = "${live.currentSpeedKmh} km/h",
                    modifier = Modifier.weight(1f)
                )
                LiveStatChip(
                    label = stringResource(R.string.live_speed_top),
                    value = "${live.topSpeedKmh} km/h",
                    modifier = Modifier.weight(1f)
                )
            }
            if (live.recordGps && live.trackPointCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.live_gps_points, live.trackPointCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
