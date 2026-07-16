package com.nruge.iceinfo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import android.os.Build
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
        const val CHANNEL_ID = "ice_tracker_channel_v3"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.nruge.iceinfo.ACTION_STOP"
        const val ACTION_UPDATE_TARGET = "com.nruge.iceinfo.ACTION_UPDATE_TARGET"
        const val EXTRA_DEMO_SPEED = "extra_demo_speed"
        const val EXTRA_TARGET_EVA = "extra_target_eva"

        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val MAX_BACKOFF_STEPS = 4
        // Nach so vielen aufeinanderfolgenden Fehlversuchen (~70 s Backoff) gilt das
        // Zug-WLAN als verlassen und der Service beendet sich samt Notification.
        private const val STOP_AFTER_FAILURES = 4

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var pollingJob: Job? = null
    private var currentDemoSpeed: Int = -1
    private var targetStopEva: String? = null
    private var lastKnownStatus: TrainStatus? = null
    private var chipShowDelay: Boolean = false
    private var liveUpdateApisUnavailable: Boolean = false

    private val connectingStatus: TrainStatus by lazy {
        TrainStatus(
            trainType = "—", trainNumber = "—", speed = 0,
            nextStop = getString(R.string.notif_connecting), destination = "", eta = "",
            isConnected = false
        )
    }

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

        if (intent?.action == ACTION_UPDATE_TARGET) {
            val newTargetEva = intent.getStringExtra(EXTRA_TARGET_EVA)
            if (newTargetEva != null) {
                targetStopEva = newTargetEva
                com.nruge.iceinfo.util.SettingsManager.setTargetStopEva(this, newTargetEva)
            }
            if (pollingJob?.isActive == true) {
                val status = buildCurrentStatus()
                notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
            } else {
                stopSelfCleanly()
                return START_NOT_STICKY
            }
            return START_STICKY
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
        pollingJob?.cancel()
        serviceScope.cancel()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        _isRunning.value = false
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        pollingJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildCurrentStatus(): TrainStatus {
        val base = when {
            currentDemoSpeed != -1 -> sampleTrainStatus.copy(speed = currentDemoSpeed, isConnected = true)
            else -> lastKnownStatus ?: connectingStatus
        }
        return base.copy(targetStopEva = targetStopEva)
    }

    private fun startPolling(demoSpeed: Int = -1) {
        this.currentDemoSpeed = demoSpeed

        if (pollingJob?.isActive == true) return
        pollingJob = serviceScope.launch {
            var failureCount = 0
            while (isActive) {
                val isDemo = currentDemoSpeed != -1
                val success = try {
                    val status = if (isDemo) {
                        sampleTrainStatus.copy(speed = currentDemoSpeed)
                    } else {
                        TrainRepository.fetchTrainStatus()
                    }.copy(targetStopEva = targetStopEva)

                    // Fallback-/Beispieldaten bei Verbindungsverlust nie anzeigen —
                    // Notification und Widget behalten den letzten echten Stand.
                    if (isDemo || status.isConnected) {
                        lastKnownStatus = status
                        val targetStop = status.stops.find { it.evaNr == targetStopEva }
                        com.nruge.iceinfo.widget.WidgetUpdater.update(
                            this@IceNotificationService,
                            status,
                            isDemo,
                            targetStop?.name
                        )
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
                    }
                    status.isConnected
                } catch (e: Exception) {
                    Log.e("IceService", "Fehler: ${e.message}")
                    false
                }

                if (success || isDemo) {
                    failureCount = 0
                    delay(POLL_INTERVAL_MS)
                } else {
                    failureCount++
                    if (failureCount >= STOP_AFTER_FAILURES) {
                        stopSelfCleanly()
                        return@launch
                    }
                    val backoff = POLL_INTERVAL_MS * (1L shl failureCount.coerceAtMost(MAX_BACKOFF_STEPS))
                    delay(backoff.coerceAtMost(MAX_BACKOFF_MS))
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }


    private fun buildNotification(status: TrainStatus): Notification {
        // Die Live-Update-APIs (ProgressStyle, setRequestPromotedOngoing, setShortCriticalText)
        // sind in Android 16 hinter dem AConfig-Flag "api_rich_ongoing" versteckt. Auf Builds
        // mit deaktiviertem Flag fehlen die Methoden trotz SDK_INT >= 36 im Framework
        // (NoSuchMethodError), daher reicht der SDK-Check allein nicht.
        if (Build.VERSION.SDK_INT >= 36 && !liveUpdateApisUnavailable) {
            try {
                return buildLiveUpdateNotification(status)
            } catch (e: LinkageError) {
                liveUpdateApisUnavailable = true
            }
        }
        return buildLegacyNotification(status)
    }

    private fun buildLegacyNotification(status: TrainStatus): Notification {
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
            if (remainingKm == 0 && remainingMin == 0) append(getString(R.string.notif_arriving))
        }

        // Progress: travelled fraction of total route to target (start-of-route → target)
        val targetPos = target?.distanceFromStart ?: 0
        val progressPercent = if (targetPos > 0)
            ((status.actualPosition.toFloat() / targetPos) * 100f).toInt().coerceIn(0, 100)
        else 0

        val footerParts = buildList {
            if (displayTrack.isNotEmpty()) add(getString(R.string.track_full, displayTrack))
            if (finalDestination != null && finalDestination.evaNr != target?.evaNr) {
                add(getString(
                    R.string.notif_destination_format,
                    finalDestination.name,
                    finalDestination.scheduledArrival
                ))
            }
        }

        val remoteViews = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.tv_train_info, "${status.trainType} ${status.trainNumber}")
            setTextViewText(R.id.tv_speed, "${status.speed} km/h")
            setTextViewText(R.id.tv_label,
                if (target?.evaNr == targetStopEva && targetStopEva != null)
                    getString(R.string.notif_label_target_stop)
                else
                    getString(R.string.notif_label_next_stop)
            )
            setTextViewText(R.id.tv_destination, displayStopName)
            setTextViewText(R.id.tv_distance, distanceLine.ifEmpty { getString(R.string.notif_no_eta) })
            setTextViewText(R.id.tv_eta, getString(R.string.notif_arrival_prefix, displayEta))
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
                getString(R.string.notif_action_stop),
                stopIntent
            )
            .build()
    }

    private fun rasterizeTrackerIcon(): Bitmap {
        val size = (resources.displayMetrics.density * 24).toInt().coerceAtLeast(48)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_train_tracker)!!
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    @androidx.annotation.RequiresApi(36)
    private fun buildLiveUpdateNotification(status: TrainStatus): Notification {
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

        val userTarget = targetStopEva?.let { eva -> status.stops.find { it.evaNr == eva } }
        val nextStop = status.stops.find { it.isNext }
        val finalDestination = status.stops.lastOrNull()

        // Bar ends at user-chosen exit stop if set, otherwise at final destination.
        val routeTarget = userTarget ?: finalDestination
        // Title/ETA prefer user target, fall back to next stop.
        val displayTarget = userTarget ?: nextStop
        val displayStopName = displayTarget?.name ?: status.nextStop
        val displayEta = displayTarget?.actualArrival?.takeIf { it.isNotEmpty() }
            ?: displayTarget?.scheduledArrival
            ?: status.eta
        val displayDelay = displayTarget?.delayMinutes ?: status.delayMinutes

        // Visual window: from last passed stop (or route start) to chosen target.
        val lastPassedDistance = status.stops.asSequence()
            .filter { it.passed }
            .maxOfOrNull { it.distanceFromStart }
            ?: 0
        val absoluteEnd = (routeTarget?.distanceFromStart ?: 1).coerceAtLeast(1)
        val windowStart = lastPassedDistance.coerceAtMost(absoluteEnd - 1)

        // Next up to 3 upcoming stops within the window.
        val upcomingStops = status.stops.asSequence()
            .filter { !it.passed && !it.isCancelled && it.distanceFromStart in (windowStart + 1)..absoluteEnd }
            .take(3)
            .toList()

        // Equal-spaced dots: divide bar into (upcomingStops.size + 1) slices.
        // Tracker position is mapped piecewise so it stays monotonic and reaches
        // each dot when the train actually passes that stop in real distance.
        val barLength = 1000
        val sliceCount = upcomingStops.size + 1
        val sliceWidth = barLength / sliceCount

        // Anchor distances define each slice's real-world endpoints.
        val anchorDistances = buildList {
            add(windowStart)
            upcomingStops.forEach { add(it.distanceFromStart) }
            add(absoluteEnd)
        }
        val realPos = status.actualPosition.coerceIn(windowStart, absoluteEnd)
        val currentPos = run {
            val segmentIndex = anchorDistances.zipWithNext().indexOfFirst { (a, b) ->
                realPos in a..b
            }.coerceAtLeast(0)
            val segStart = anchorDistances[segmentIndex]
            val segEnd = anchorDistances[segmentIndex + 1]
            val frac = if (segEnd > segStart) (realPos - segStart).toFloat() / (segEnd - segStart) else 0f
            ((segmentIndex + frac) * sliceWidth).toInt().coerceIn(0, barLength)
        }

        val pointColor = Color.parseColor("#1976D2")
        val points = upcomingStops.mapIndexed { idx, _ ->
            Notification.ProgressStyle.Point((idx + 1) * sliceWidth).setColor(pointColor)
        }

        val segment = Notification.ProgressStyle.Segment(barLength)
            .setColor(Color.parseColor("#B0BEC5"))

        val trackerIcon = Icon.createWithBitmap(rasterizeTrackerIcon())

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(currentPos)
            .setProgressSegments(listOf(segment))
            .setProgressPoints(points)
            .setProgressTrackerIcon(trackerIcon)

        // Chip rotates between speed and delay when delayed; otherwise just speed.
        val speedText = "${status.speed}"
        val showingDelay: Boolean
        val chipText: String
        if (displayDelay > 0) {
            chipShowDelay = !chipShowDelay
            showingDelay = chipShowDelay
            chipText = if (chipShowDelay) "+$displayDelay" else speedText
        } else {
            chipShowDelay = false
            showingDelay = false
            chipText = speedText
        }

        val smallIcon = if (showingDelay) R.drawable.ic_chip_delay else R.drawable.ic_chip_speed

        val contentTitle = "${status.trainType} ${status.trainNumber}".trim()
        val contentText = buildString {
            append(displayStopName)
            if (displayEta.isNotEmpty()) append(" · ").append(displayEta)
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSubText("${status.speed} km/h")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(progressStyle)
            .setShortCriticalText(chipText)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    getString(R.string.notif_action_stop),
                    stopIntent
                ).build()
            )

        return builder.build()
    }
}