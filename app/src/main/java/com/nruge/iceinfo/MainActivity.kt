package com.nruge.iceinfo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.ui.AppBottomBar
import com.nruge.iceinfo.ui.AppNavigation
import com.nruge.iceinfo.ui.AppTopBar
import com.nruge.iceinfo.ui.InfoDialog
import com.nruge.iceinfo.ui.OnboardingDialog
import com.nruge.iceinfo.ui.StopSelectionDialog
import com.nruge.iceinfo.ui.MainViewModel
import com.nruge.iceinfo.ui.SettingsSheet
import com.nruge.iceinfo.ui.components.NoWifiScreen
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.util.isWIFIonICE as checkWIFIonICE
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startForegroundService(Intent(this, IceNotificationService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
            val viewModel: MainViewModel = viewModel()
            val trainStatus: TrainStatus by viewModel.trainStatus.collectAsState()
            val pois: List<PoiItem> by viewModel.pois.collectAsState()
            val isMockMode: Boolean by viewModel.isMockMode.collectAsState()
            val demoSpeed: Int by viewModel.demoSpeed.collectAsState()
            val connections: List<ConnectingTrain> by viewModel.connections.collectAsState()
            val isWIFIonICEStatus: Boolean by viewModel.isWIFIonICE.collectAsState()

            var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ICEInfoTheme(darkTheme = isDark) {
                val view = LocalView.current
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    while (true) {
                        viewModel.updateWifiStatus(checkWIFIonICE(context))
                        delay(5000)
                    }
                }
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                val serviceRunning by IceNotificationService.isRunning.collectAsStateWithLifecycle()
                var showInfo by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var showDemoSpeed by remember { mutableStateOf(false) }
                
                val prefs = remember { context.getSharedPreferences("iceinfo_prefs", MODE_PRIVATE) }
                var showOnboarding by remember { 
                    mutableStateOf(!prefs.getBoolean("onboarding_shown", false)) 
                }

                LaunchedEffect(intent) {
                    // Action select target is now handled by dropdown in HomeScreen
                }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        AppTopBar(
                            isMockMode = isMockMode,
                            isConnected = trainStatus.isConnected,
                            serviceRunning = serviceRunning,
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
                            onExitDemo = { viewModel.setMockMode(false) },
                            onShowSettings = { showSettings = true },
                            onShowInfo = { showInfo = true },
                            scrollBehavior = scrollBehavior
                        )
                    },
                    bottomBar = {
                        AppBottomBar(
                            currentRoute = currentRoute,
                            enabled = trainStatus.isConnected || isMockMode,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    if (!trainStatus.isConnected && !isMockMode) {
                        NoWifiScreen(
                            modifier = Modifier.padding(innerPadding),
                            status = trainStatus,
                            isWIFIonICE = isWIFIonICEStatus,
                            onRetry = { viewModel.retryConnection() },
                            onMockMode = { viewModel.setMockMode(true) }
                        )
                    } else {
                        AppNavigation(
                            navController = navController,
                            innerPadding = innerPadding,
                            trainStatus = trainStatus,
                            pois = pois,
                            connections = connections,
                            isDarkTheme = isDark,
                            isMockMode = isMockMode,
                            demoSpeed = demoSpeed,
                            showDemoSpeed = showDemoSpeed,
                            onDemoSpeedChange = { 
                                viewModel.setDemoSpeed(it)
                                if (serviceRunning && isMockMode) {
                                    val intent = Intent(context, IceNotificationService::class.java).apply {
                                        putExtra(IceNotificationService.EXTRA_DEMO_SPEED, it)
                                    }
                                    context.startForegroundService(intent)
                                }
                            },
                            onTargetStopChange = { viewModel.setTargetStop(it) }
                        )
                    }
                }

                if (showInfo) {
                    InfoDialog(onDismiss = { showInfo = false })
                }

                if (showSettings) {
                    SettingsSheet(
                        appTheme = appTheme,
                        onThemeChange = { appTheme = it },
                        isMockMode = isMockMode,
                        showDemoSpeed = showDemoSpeed,
                        onToggleDemoSpeed = { showDemoSpeed = it },
                        onDismiss = { showSettings = false }
                    )
                }

                if (showOnboarding) {
                    OnboardingDialog(onDismiss = {
                        prefs.edit { putBoolean("onboarding_shown", true) }
                        showOnboarding = false
                    })
                }
            }
        }
    }
}
