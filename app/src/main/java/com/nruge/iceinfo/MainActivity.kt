package com.nruge.iceinfo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import com.nruge.iceinfo.ui.navigationItems
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.ui.AppNavigationBar
import com.nruge.iceinfo.ui.AppNavigation
import com.nruge.iceinfo.ui.AppTopBar
import com.nruge.iceinfo.ui.ChangelogDialog
import com.nruge.iceinfo.ui.WhatsNewDialog
import com.nruge.iceinfo.ui.CrashReportingConsentDialog
import com.nruge.iceinfo.ui.DebugDialog
import com.nruge.iceinfo.ui.InfoDialog
import com.nruge.iceinfo.ui.MainViewModel
import com.nruge.iceinfo.ui.OnboardingDialog
import com.nruge.iceinfo.ui.RecordJourneyDialog
import com.nruge.iceinfo.ui.SettingsScreen
import com.nruge.iceinfo.ui.StopSelectionDialog
import com.nruge.iceinfo.ui.components.NoWifiScreen

import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.util.isWIFIonICE as checkWIFIonICE
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val snackbarHostState = SnackbarHostState()

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            lifecycleScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = getString(R.string.update_downloaded),
                    actionLabel = getString(R.string.update_install),
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeUpdate()
                }
            }
        }
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startForegroundService(Intent(this, IceNotificationService::class.java))
        }
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.w("AppUpdate", "Update flow cancelled: ${result.resultCode}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            // Resume an IMMEDIATE update that was interrupted (e.g. user
            // backgrounded the app during the update flow). Without this,
            // a half-finished high-priority update would be silently
            // dropped on the floor.
            if (info.updateAvailability() ==
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
                return@addOnSuccessListener
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                lifecycleScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = getString(R.string.update_downloaded),
                        actionLabel = getString(R.string.update_install),
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        appUpdateManager.completeUpdate()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener

            // High-priority updates (set via Play Publishing API, priority >= 4)
            // are pushed as IMMEDIATE — the user must update before continuing.
            // Used for critical hotfixes. Everything else stays FLEXIBLE.
            val highPriority = info.updatePriority() >= 4
            val type = when {
                highPriority && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    AppUpdateType.IMMEDIATE
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                    AppUpdateType.FLEXIBLE
                else -> return@addOnSuccessListener
            }
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateResultLauncher,
                AppUpdateOptions.newBuilder(type).build()
            )
        }.addOnFailureListener {
            Log.w("AppUpdate", "Update check failed: ${it.message}")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)
        checkForUpdate()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        setContent {
            val viewModel: MainViewModel = viewModel()
            val trainStatus: TrainStatus by viewModel.trainStatus.collectAsStateWithLifecycle()
            val pois: List<PoiItem> by viewModel.pois.collectAsStateWithLifecycle()
            val isMockMode: Boolean by viewModel.isMockMode.collectAsStateWithLifecycle()
            val demoSpeed: Int by viewModel.demoSpeed.collectAsStateWithLifecycle()
            val reducedMotion: Boolean by viewModel.reducedMotion.collectAsStateWithLifecycle()
            val connections: List<ConnectingTrain> by viewModel.connections.collectAsStateWithLifecycle()
            val departures: List<Departure> by viewModel.departures.collectAsStateWithLifecycle()
            val weather by viewModel.weather.collectAsStateWithLifecycle()
            val isWIFIonICEStatus: Boolean by viewModel.isWIFIonICE.collectAsStateWithLifecycle()
            val isReconnecting: Boolean by viewModel.isReconnecting.collectAsStateWithLifecycle()
            val serviceStation by viewModel.serviceStation.collectAsStateWithLifecycle()
            val stationSearchResults by viewModel.stationSearchResults.collectAsStateWithLifecycle()
            val osmData by viewModel.osmData.collectAsStateWithLifecycle()
            val savedJourneys by viewModel.journeys.collectAsStateWithLifecycle()
            val showRecordingConsent by viewModel.showRecordingConsent.collectAsStateWithLifecycle()
            val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
            val liveRecording by viewModel.liveRecording.collectAsStateWithLifecycle()
            val menuItems by viewModel.menuCategories.collectAsStateWithLifecycle()
            val isMenuLoading by viewModel.isMenuLoading.collectAsStateWithLifecycle()
            val coaches by viewModel.coaches.collectAsStateWithLifecycle()
            val coachStopName by viewModel.coachStopName.collectAsStateWithLifecycle()
            val selectedCoach by viewModel.selectedCoach.collectAsStateWithLifecycle()
            val seatNumber by viewModel.seatNumber.collectAsStateWithLifecycle()

            val initialContext = LocalContext.current
            var appTheme by rememberSaveable {
                mutableStateOf(
                    runCatching {
                        AppTheme.valueOf(com.nruge.iceinfo.util.SettingsManager.getAppTheme(initialContext))
                    }.getOrDefault(AppTheme.SYSTEM)
                )
            }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ICEInfoTheme(darkTheme = isDark) {
                val view = LocalView.current
                val context = LocalContext.current

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        // App kommt in Vordergrund: sofort pollen und WiFi prüfen
                        viewModel.onForeground()
                        viewModel.updateWifiStatus(checkWIFIonICE(context))
                        while (true) {
                            delay(5000)
                            viewModel.updateWifiStatus(checkWIFIonICE(context))
                        }
                    }
                    // App geht in Hintergrund (Block wurde gecancelt)
                    viewModel.onBackground()
                }
                SideEffect {
                    val window = (view.context as Activity).window
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }

                val serviceRunning by IceNotificationService.isRunning.collectAsStateWithLifecycle()
                var showInfo by remember { mutableStateOf(false) }
                var showChangelog by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var crashReportingEnabled by remember {
                    mutableStateOf(com.nruge.iceinfo.util.SettingsManager.isCrashReportingEnabled(context))
                }
                val traewellingAccount by com.nruge.iceinfo.TraewellingAuth.account.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { com.nruge.iceinfo.TraewellingAuth.init(context) }
                var showDebug by remember { mutableStateOf(false) }
                var showDemoSpeed by remember { mutableStateOf(false) }
                
                var showOnboarding by remember {
                    mutableStateOf(!com.nruge.iceinfo.util.SettingsManager.isOnboardingShown(context))
                }
                var showCrashConsent by remember {
                    mutableStateOf(
                        com.nruge.iceinfo.util.SettingsManager.getCrashConsentVersion(context)
                            != com.nruge.iceinfo.BuildConfig.VERSION_CODE
                    )
                }

                val lastSeenVersion = com.nruge.iceinfo.util.SettingsManager.getLastSeenVersion(context)
                val currentVersion = com.nruge.iceinfo.BuildConfig.VERSION_CODE
                var showWhatsNew by remember {
                    mutableStateOf(lastSeenVersion > 0 && lastSeenVersion < currentVersion)
                }
                if (lastSeenVersion != currentVersion) {
                    com.nruge.iceinfo.util.SettingsManager.setLastSeenVersion(context, currentVersion)
                }

                LaunchedEffect(intent) {
                    // Action select target is now handled by dropdown in HomeScreen
                }
                val pagerState = rememberPagerState(pageCount = { navigationItems.size })
                val coroutineScope = rememberCoroutineScope()
                var showJourneys by remember { mutableStateOf(false) }
                var showConnections by remember { mutableStateOf(false) }
                val currentRoute = when {
                    showJourneys -> com.nruge.iceinfo.ui.Screen.Journeys.route
                    showConnections -> com.nruge.iceinfo.ui.Screen.Connections.route
                    else -> navigationItems[pagerState.currentPage].route
                }
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

                BackHandler(enabled = showJourneys || showConnections || showSettings) {
                    showJourneys = false
                    showConnections = false
                    showSettings = false
                }

                LaunchedEffect(currentRoute) {
                    scrollBehavior.state.contentOffset = 0f
                }

                var demoBackProgress by remember { mutableFloatStateOf(0f) }
                var demoBackInProgress by remember { mutableStateOf(false) }

                // NoWifiScreen auch zeigen, wenn WIFIonICE zwar verbunden ist, aber das
                // Bordportal nichts liefert (dann mit entsprechender Meldung). Während einer
                // laufenden Aufzeichnung bleibt die App in dem Fall sichtbar.
                val noWifiOverlayVisible = !trainStatus.isConnected && !isMockMode
                    && !isReconnecting
                    && !(isWIFIonICEStatus && isRecording)
                    && currentRoute != com.nruge.iceinfo.ui.Screen.Journeys.route

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(snackbarData = data)
                        }
                    },
                    topBar = {
                        AppTopBar(
                            isMockMode = isMockMode,
                            isConnected = trainStatus.isConnected,
                            isOnTrainWifi = isWIFIonICEStatus,
                            isReconnecting = isReconnecting,
                            serviceRunning = serviceRunning,
                            showPrideBadge = !trainStatus.isConnected && !isMockMode && !isWIFIonICEStatus,
                            isRecording = isRecording,
                            onSaveRecording = { viewModel.saveRecordingNow() },
                            onCancelRecording = { viewModel.cancelRecording() },
                            onToggleService = {
                                if (serviceRunning) {
                                    val stopIntent = Intent(context, IceNotificationService::class.java).apply {
                                        action = IceNotificationService.ACTION_STOP
                                    }
                                    context.startService(stopIntent)
                                } else {
                                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                        == PackageManager.PERMISSION_GRANTED) {
                                        val intent = Intent(context, IceNotificationService::class.java).apply {
                                            if (isMockMode) {
                                                putExtra(IceNotificationService.EXTRA_DEMO_SPEED, demoSpeed)
                                            }
                                            trainStatus.targetStopEva?.let {
                                                putExtra(IceNotificationService.EXTRA_TARGET_EVA, it)
                                            }
                                        }
                                        context.startForegroundService(intent)
                                    } else {
                                        requestPermissionLauncher.launch(
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                            },
                            onShareTrip = { com.nruge.iceinfo.util.shareTrainStatus(context, trainStatus) },
                            onExitDemo = { viewModel.setMockMode(false) },
                            onStartDemo = { viewModel.setMockMode(true) },
                            onShowSettings = { showSettings = true },
                            onShowInfo = { showInfo = true },
                            onShowChangelog = { showChangelog = true },
                            onShowJourneys = { showJourneys = true },
                            onShowConnections = { showConnections = true },
                            onNavigateBack = if (showJourneys || showConnections || showSettings) {
                                { showJourneys = false; showConnections = false; showSettings = false }
                            } else null,
                            // Journeys zeichnet die Linie selbst unter seinem Header
                            showScrollDivider = currentRoute != com.nruge.iceinfo.ui.Screen.Journeys.route,
                            scrollBehavior = scrollBehavior
                        )
                    },
                    bottomBar = {
                        if ((trainStatus.isConnected || isMockMode || isWIFIonICEStatus) && !demoBackInProgress
                            && !noWifiOverlayVisible
                            && !showJourneys && !showConnections && !showSettings) {
                            AppNavigationBar(
                                currentRoute = currentRoute,
                                enabled = true,
                                onNavigate = { route ->
                                    val index = navigationItems.indexOfFirst { it.route == route }
                                    if (index >= 0) coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    PredictiveBackHandler(enabled = isMockMode) { events ->
                        try {
                            events.collect { e ->
                                demoBackInProgress = true
                                demoBackProgress = e.progress
                            }
                            viewModel.setMockMode(false)
                        } catch (_: kotlinx.coroutines.CancellationException) {
                        } finally {
                            demoBackInProgress = false
                            demoBackProgress = 0f
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (demoBackInProgress) {
                            NoWifiScreen(
                                modifier = Modifier.padding(innerPadding),
                                status = trainStatus,
                                isWIFIonICE = isWIFIonICEStatus,
                                onRetry = {},
                                onMockMode = {}
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (demoBackInProgress) {
                                        val s = 1f - (demoBackProgress * 0.15f)
                                        scaleX = s
                                        scaleY = s
                                        alpha = 1f - demoBackProgress * 0.9f
                                    }
                                }
                        ) {
                            AppNavigation(
                                    pagerState = pagerState,
                                    isJourneysVisible = showJourneys,
                                    isConnectionsVisible = showConnections,
                                    innerPadding = innerPadding,
                                    trainStatus = trainStatus,
                                    pois = pois,
                                    connections = connections,
                                    departures = departures,
                                    weather = weather,
                                    osmData = osmData,
                                    isMockMode = isMockMode,
                                    demoSpeed = demoSpeed,
                                    showDemoSpeed = showDemoSpeed,
                                    reducedMotion = reducedMotion,
                                    onDemoSpeedChange = {
                                        viewModel.setDemoSpeed(it)
                                        if (serviceRunning && isMockMode) {
                                            val intent = Intent(context, IceNotificationService::class.java).apply {
                                                putExtra(IceNotificationService.EXTRA_DEMO_SPEED, it)
                                            }
                                            context.startForegroundService(intent)
                                        }
                                    },
                                    onTargetStopChange = { viewModel.setTargetStop(it) },
                                    coaches = coaches,
                                    coachStopName = coachStopName,
                                    selectedCoach = selectedCoach,
                                    seatNumber = seatNumber,
                                    onCoachChange = { viewModel.setCoach(it) },
                                    onSeatChange = { viewModel.setSeat(it) },
                                    serviceStation = serviceStation,
                                    stationSearchResults = stationSearchResults,
                                    onStationSearchQueryChange = { viewModel.searchStations(it) },
                                    onStationSelect = { viewModel.selectServiceStation(it) },
                                    onLoadTrainStation = { eva, name -> viewModel.loadServiceStationFromTrain(eva, name) },
                                    savedJourneys = savedJourneys,
                                    onDeleteJourney = { viewModel.deleteJourney(it) },
                                    onUpdateJourney = { viewModel.updateJourney(it) },
                                    isRecording = isRecording,
                                    liveRecording = liveRecording,
                                    onStartRecording = { viewModel.requestRecording() },
                                    onExportJourneys = { uri, cb -> viewModel.exportJourneys(uri, cb) },
                                    onImportJourneys = { uri, cb -> viewModel.importJourneys(uri, cb) },
                                    menuItems = menuItems,
                                    isMenuLoading = isMenuLoading,
                                    onLoadMenu = { viewModel.fetchMenuIfNeeded() }
                                )
                            // NoWifiScreen als Overlay wenn nicht verbunden und nicht auf Journeys-Screen
                            AnimatedVisibility(
                                visible = noWifiOverlayVisible,
                                enter = EnterTransition.None,
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                NoWifiScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(innerPadding),
                                    status = trainStatus,
                                    isWIFIonICE = isWIFIonICEStatus,
                                    onRetry = { viewModel.retryConnection() },
                                    onMockMode = { viewModel.setMockMode(true) }
                                )
                            }

                            // Popup: WIFIonICE verbunden, aber keine API-Verbindung — nur wenn
                            // der NoWifiScreen die Meldung nicht ohnehin schon zeigt
                            AnimatedVisibility(
                                visible = isWIFIonICEStatus && !trainStatus.isConnected && !isMockMode
                                    && !isRecording && !noWifiOverlayVisible,
                                enter = slideInVertically { it } + fadeIn(),
                                exit = slideOutVertically { it } + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        start = 16.dp, end = 16.dp,
                                        bottom = 16.dp + innerPadding.calculateBottomPadding()
                                    )
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    tonalElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiTetheringError,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.no_wifi_api_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { showDebug = true },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Text("Debug", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Overlay: Einstellungen — Vollbild-Seite über das Top-Bar-Menü
                            // (Zurück-Pfeil in der geteilten AppTopBar, siehe onNavigateBack).
                            AnimatedVisibility(
                                visible = showSettings,
                                enter = if (reducedMotion) fadeIn() else
                                    slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)),
                                exit = if (reducedMotion) fadeOut() else
                                    slideOutHorizontally(tween(250)) { it } + fadeOut(tween(150))
                            ) {
                                SettingsScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(innerPadding),
                                    appTheme = appTheme,
                                    onThemeChange = {
                                        appTheme = it
                                        com.nruge.iceinfo.util.SettingsManager.setAppTheme(context, it.name)
                                    },
                                    isMockMode = isMockMode,
                                    showDemoSpeed = showDemoSpeed,
                                    onToggleDemoSpeed = { showDemoSpeed = it },
                                    reducedMotion = reducedMotion,
                                    onToggleReducedMotion = { viewModel.setReducedMotion(it) },
                                    language = com.nruge.iceinfo.util.SettingsManager.getLanguage(context),
                                    onLanguageChange = {
                                        com.nruge.iceinfo.util.SettingsManager.setLanguage(context, it)
                                        showSettings = false
                                    },
                                    crashReportingEnabled = crashReportingEnabled,
                                    onToggleCrashReporting = { enabled ->
                                        crashReportingEnabled = enabled
                                        com.nruge.iceinfo.util.SettingsManager.setCrashReportingEnabled(context, enabled)
                                        runCatching {
                                            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                                                .isCrashlyticsCollectionEnabled = enabled
                                        }
                                    },
                                    onDebug = {
                                        showSettings = false
                                        showDebug = true
                                    },
                                    traewellingConnected = traewellingAccount != null,
                                    traewellingUsername = traewellingAccount?.username,
                                    onConnectTraewelling = {
                                        context.startActivity(
                                            com.nruge.iceinfo.TraewellingAuth.buildAuthorizeIntent(context)
                                        )
                                    },
                                    onDisconnectTraewelling = {
                                        com.nruge.iceinfo.TraewellingAuth.disconnect(context)
                                    }
                                )
                            }
                        }
                    }
                }

                if (showRecordingConsent) {
                    RecordJourneyDialog(
                        status = trainStatus,
                        isRecording = isRecording,
                        traewellingConnected = traewellingAccount != null,
                        onRecord = { recordGps -> viewModel.startRecording(recordGps) },
                        onCheckIn = { exitStop ->
                            coroutineScope.launch {
                                val msg = when (val r = com.nruge.iceinfo.TraewellingRepository
                                    .checkIn(context, trainStatus, exitStop)) {
                                    com.nruge.iceinfo.TraewellingRepository.CheckInResult.Success ->
                                        context.getString(R.string.checkin_success)
                                    com.nruge.iceinfo.TraewellingRepository.CheckInResult.NotConnected ->
                                        context.getString(R.string.checkin_not_connected)
                                    com.nruge.iceinfo.TraewellingRepository.CheckInResult.TrainNotFound ->
                                        context.getString(R.string.checkin_train_not_found)
                                    is com.nruge.iceinfo.TraewellingRepository.CheckInResult.Failure ->
                                        r.message ?: context.getString(R.string.checkin_failed)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onDismiss = { viewModel.declineRecording() }
                    )
                }

                if (showInfo) {
                    InfoDialog(onDismiss = { showInfo = false })
                }

                if (showChangelog) {
                    ChangelogDialog(onDismiss = { showChangelog = false })
                }

                if (showWhatsNew) {
                    WhatsNewDialog(onDismiss = { showWhatsNew = false })
                }

                if (showDebug) {
                    val simulateTrainWifi by viewModel.simulateWifiOnIce.collectAsStateWithLifecycle()
                    DebugDialog(
                        trainConnected = trainStatus.isConnected || isMockMode,
                        simulateTrainWifi = simulateTrainWifi,
                        onToggleSimulateTrainWifi = { viewModel.setSimulateWifiOnIce(it) },
                        onDismiss = { showDebug = false }
                    )
                }

                if (showOnboarding) {
                    OnboardingDialog(onDismiss = {
                        com.nruge.iceinfo.util.SettingsManager.setOnboardingShown(context)
                        showOnboarding = false
                    })
                } else if (showCrashConsent) {
                    CrashReportingConsentDialog(
                        onAccept = {
                            com.nruge.iceinfo.util.SettingsManager.setCrashReportingEnabled(context, true)
                            com.nruge.iceinfo.util.SettingsManager.setCrashConsentVersion(
                                context, com.nruge.iceinfo.BuildConfig.VERSION_CODE
                            )
                            runCatching {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                                    .isCrashlyticsCollectionEnabled = true
                            }
                            showCrashConsent = false
                        },
                        onDecline = {
                            com.nruge.iceinfo.util.SettingsManager.setCrashReportingEnabled(context, false)
                            com.nruge.iceinfo.util.SettingsManager.setCrashConsentVersion(
                                context, com.nruge.iceinfo.BuildConfig.VERSION_CODE
                            )
                            runCatching {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                                    .isCrashlyticsCollectionEnabled = false
                            }
                            showCrashConsent = false
                        }
                    )
                }
            }
        }
    }
}
