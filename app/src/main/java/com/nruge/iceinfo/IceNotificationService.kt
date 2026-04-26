package com.nruge.iceinfo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.nruge.iceinfo.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IceNotificationService : Service() {


    companion object {
        const val CHANNEL_ID = "ice_tracker_channel_v2"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.nruge.iceinfo.ACTION_STOP"
        const val ACTION_UPDATE_TARGET = "com.nruge.iceinfo.ACTION_UPDATE_TARGET"
        const val EXTRA_DEMO_SPEED = "extra_demo_speed"
        const val EXTRA_TARGET_EVA = "extra_target_eva"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var pollingJob: Job? = null
    private var currentDemoSpeed: Int = -1
    private var targetStopEva: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        targetStopEva = com.nruge.iceinfo.util.SettingsManager.getTargetStopEva(this)
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always promote to foreground immediately to satisfy the 5s contract,
        // even on ACTION_STOP — Android still expects startForeground() if the
        // service was started via startForegroundService().
        val initialStatus = buildCurrentStatus()
        runCatching {
            startForeground(NOTIFICATION_ID, buildNotification(initialStatus))
        }.onFailure { Log.e("IceService", "startForeground failed: ${it.message}") }

        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        val newTargetEva = intent?.getStringExtra(EXTRA_TARGET_EVA)
        if (newTargetEva != null) {
            targetStopEva = newTargetEva
            com.nruge.iceinfo.util.SettingsManager.setTargetStopEva(this, newTargetEva)
        }

        val demoSpeed = intent?.getIntExtra(EXTRA_DEMO_SPEED, -1) ?: -1
        if (demoSpeed != -1) {
            currentDemoSpeed = demoSpeed
            Log.d("IceService", "Demo speed updated: $currentDemoSpeed")
        }

        val status = buildCurrentStatus()
        val targetStop = status.stops.find { it.evaNr == targetStopEva }
        com.nruge.iceinfo.widget.WidgetUpdater.update(
            this,
            status,
            currentDemoSpeed != -1,
            targetStop?.name
        )
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
        startPolling(currentDemoSpeed)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        serviceScope.cancel()
        _isRunning.value = false
    }

    private fun stopSelfCleanly() {
        pollingJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildCurrentStatus(): TrainStatus =
        sampleTrainStatus
            .let { if (currentDemoSpeed != -1) it.copy(speed = currentDemoSpeed) else it }
            .copy(targetStopEva = targetStopEva)

    private fun startPolling(demoSpeed: Int = -1) {
        this.currentDemoSpeed = demoSpeed
        
        if (pollingJob?.isActive == true) return
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val status = if (currentDemoSpeed != -1) {
                        sampleTrainStatus.copy(speed = currentDemoSpeed)
                    } else {
                        TrainRepository.fetchTrainStatus()
                    }.copy(targetStopEva = targetStopEva)

                    val targetStop = status.stops.find { it.evaNr == targetStopEva }
                    com.nruge.iceinfo.widget.WidgetUpdater.update(
                        this@IceNotificationService, 
                        status, 
                        currentDemoSpeed != -1, 
                        targetStop?.name
                    )
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
                } catch (e: Exception) {
                    Log.e("IceService", "Fehler: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ICE Tracker",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Zeigt Geschwindigkeit und nächsten Halt"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }


    private fun buildNotification(status: TrainStatus): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, IceNotificationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        // Prefer user-selected target; fall back to next stop
        val target = status.stops.find { it.evaNr == targetStopEva }
            ?: status.stops.find { it.isNext }

        val finalDestination = status.stops.lastOrNull()

        val displayStopName = target?.name ?: status.nextStop
        val displayEta = target?.actualArrival?.takeIf { it.isNotEmpty() }
            ?: target?.scheduledArrival
            ?: status.eta
        val displayTrack = target?.track ?: status.track
        val displayDelay = target?.delayMinutes ?: status.delayMinutes

        // Distance + ETA in minutes from current speed
        val remainingMeters = ((target?.distanceFromStart ?: 0) - status.actualPosition).coerceAtLeast(0)
        val remainingKm = remainingMeters / 1000
        val remainingMin = if (status.speed > 0)
            ((remainingMeters / 1000.0) / status.speed * 60.0).toInt()
        else 0

        val distanceLine = buildString {
            if (remainingKm > 0) append("$remainingKm km")
            if (remainingKm > 0 && remainingMin > 0) append(" · ")
            if (remainingMin > 0) append("${remainingMin} min")
            if (remainingKm == 0 && remainingMin == 0) append("Ankunft …")
        }

        // Progress: travelled fraction of total route to target (start-of-route → target)
        val targetPos = target?.distanceFromStart ?: 0
        val progressPercent = if (targetPos > 0)
            ((status.actualPosition.toFloat() / targetPos) * 100f).toInt().coerceIn(0, 100)
        else 0

        val footerParts = buildList {
            if (displayTrack.isNotEmpty()) add("Gleis $displayTrack")
            if (finalDestination != null && finalDestination.evaNr != target?.evaNr) {
                add("Ziel: ${finalDestination.name} ${finalDestination.scheduledArrival}")
            }
        }

        val remoteViews = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.tv_train_info, "${status.trainType} ${status.trainNumber}")
            setTextViewText(R.id.tv_speed, "${status.speed} km/h")
            setTextViewText(R.id.tv_label,
                if (target?.evaNr == targetStopEva && targetStopEva != null) "DEIN AUSSTIEG"
                else "NÄCHSTER HALT"
            )
            setTextViewText(R.id.tv_destination, displayStopName)
            setTextViewText(R.id.tv_distance, distanceLine.ifEmpty { "—" })
            setTextViewText(R.id.tv_eta, "an. $displayEta")
            if (displayDelay > 0) {
                setTextViewText(R.id.tv_delay, "+$displayDelay")
                setTextColor(R.id.tv_delay, Color.parseColor("#D32F2F"))
                setViewVisibility(R.id.tv_delay, android.view.View.VISIBLE)
            } else {
                setTextViewText(R.id.tv_delay, "")
                setViewVisibility(R.id.tv_delay, android.view.View.GONE)
            }
            setProgressBar(R.id.pb_progress, 100, progressPercent, false)
            if (footerParts.isEmpty()) {
                setViewVisibility(R.id.tv_footer, android.view.View.GONE)
            } else {
                setTextViewText(R.id.tv_footer, footerParts.joinToString(" · "))
                setViewVisibility(R.id.tv_footer, android.view.View.VISIBLE)
            }
        }

        val smallIcon = when {
            status.speed >= 250 -> R.drawable.ic_speed_300
            status.speed >= 150 -> R.drawable.ic_speed_200
            status.speed >= 50 -> R.drawable.ic_speed_100
            else -> R.drawable.ic_speed
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Beenden",
                stopIntent
            )
            .build()
    }
}