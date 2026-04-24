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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nruge.iceinfo.model.AppTheme
import com.nruge.iceinfo.ui.AppBottomBar
import com.nruge.iceinfo.ui.AppNavigation
import com.nruge.iceinfo.ui.AppTopBar
import com.nruge.iceinfo.ui.InfoDialog
import com.nruge.iceinfo.ui.MainViewModel
import com.nruge.iceinfo.ui.components.NoWifiScreen
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

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
            var demoSpeed by remember { mutableIntStateOf(114) }
            var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ICEInfoTheme(darkTheme = isDark) {
                val viewModel: MainViewModel = viewModel()
                val trainStatus by viewModel.trainStatus.collectAsState()
                val pois by viewModel.pois.collectAsState()
                val isMockMode by viewModel.isMockMode.collectAsState()
                val connections by viewModel.connections.collectAsState()

                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                var serviceRunning by remember { mutableStateOf(false) }
                var showInfo by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        AppTopBar(
                            isMockMode = isMockMode,
                            isConnected = trainStatus.isConnected,
                            serviceRunning = serviceRunning,
                            onToggleService = {
                                if (serviceRunning) {
                                    stopService(Intent(context, IceNotificationService::class.java))
                                    serviceRunning = false
                                } else {
                                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                        == PackageManager.PERMISSION_GRANTED) {
                                        context.startForegroundService(Intent(context, IceNotificationService::class.java))
                                        serviceRunning = true
                                    } else {
                                        requestPermissionLauncher.launch(
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        )
                                        serviceRunning = true
                                    }
                                }
                            },
                            onExitDemo = { viewModel.setMockMode(false) },
                            onThemeChange = { appTheme = it },
                            onShowInfo = { showInfo = true }
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
                            onDemoSpeedChange = { demoSpeed = it }
                        )
                    }
                }

                if (showInfo) {
                    InfoDialog(onDismiss = { showInfo = false })
                }
            }
        }
    }
}
