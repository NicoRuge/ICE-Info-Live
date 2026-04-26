package com.nruge.iceinfo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.nruge.iceinfo.model.*
import kotlinx.coroutines.*

class IceNotificationService : Service() {


    companion object {
        const val CHANNEL_ID = "ice_tracker_channel_v2"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.nruge.iceinfo.ACTION_STOP"
        const val ACTION_UPDATE_TARGET = "com.nruge.iceinfo.ACTION_UPDATE_TARGET"
        const val EXTRA_DEMO_SPEED = "extra_demo_speed"
        const val EXTRA_TARGET_EVA = "extra_target_eva"
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
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
        
        val status = sampleTrainStatus.let { 
            if (currentDemoSpeed != -1) it.copy(speed = currentDemoSpeed) else it 
        }.copy(targetStopEva = targetStopEva)
        
        val targetStop = status.stops.find { it.evaNr == targetStopEva }
        com.nruge.iceinfo.widget.WidgetUpdater.update(
            this, 
            status, 
            currentDemoSpeed != -1, 
            targetStop?.name
        )
        
        // Only show/update notification if we are already in foreground or explicitly starting
        // ACTION_UPDATE_TARGET should only update if the service is already "alive"
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
        startPolling(currentDemoSpeed)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

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

        val targetStop = status.stops.find { it.evaNr == targetStopEva } ?: status.stops.find { it.isNext }
        
        val displayStopName = targetStop?.name ?: status.nextStop
        val displayEta = targetStop?.scheduledArrival ?: status.eta
        val displayTrack = targetStop?.track ?: status.track
        val displayDelay = targetStop?.delayMinutes ?: status.delayMinutes

        val delayText = when {
            displayDelay > 0 -> "+$displayDelay min"
            else -> "pünktlich"
        }
        val trackText = if (displayTrack.isNotEmpty()) "Gleis $displayTrack" else ""

        // Progress berechnen basierend auf dem Ziel
        val progress = if (targetStop != null) {
            val currentPos = status.actualPosition
            val targetPos = targetStop.distanceFromStart
            val prevStop = status.stops.takeWhile { it.evaNr != targetStop.evaNr }.lastOrNull { it.passed } 
                ?: status.stops.firstOrNull()
            
            val startPos = prevStop?.distanceFromStart ?: 0
            if (targetPos > startPos) {
                ((currentPos - startPos).toFloat() / (targetPos - startPos)).coerceIn(0f, 1f)
            } else 0f
        } else 0f

        val remoteViews = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.tv_speed, "${status.speed} km/h")
            setTextViewText(R.id.tv_train_info, "${status.trainType} ${status.trainNumber}")
            setTextViewText(R.id.tv_next_stop, "→ $displayStopName")
            setTextViewText(R.id.tv_eta, "Ankunft: $displayEta $trackText")
            setTextViewText(R.id.tv_delay, delayText)
            if (displayDelay > 0) {
                setTextColor(R.id.tv_delay, Color.RED)
            } else {
                setTextColor(R.id.tv_delay, Color.parseColor("#388E3C"))
            }
            setImageViewBitmap(R.id.iv_tracks, createTrainTrackBitmap(progress))
        }

        val smallIcon = when {
            status.speed >= 250 -> R.drawable.ic_speed_300
            status.speed >= 150 -> R.drawable.ic_speed_200
            status.speed >= 50 -> R.drawable.ic_speed_100
            else -> R.drawable.ic_speed
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Beenden",
                stopIntent
            )

        return notificationBuilder.build()
    }

    private fun createTrainTrackBitmap(progress: Float): Bitmap {
        val width = 800
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 4f
        }

        // Tracks
        paint.color = Color.LTGRAY
        val trackY1 = height * 0.4f
        val trackY2 = height * 0.6f
        canvas.drawLine(0f, trackY1, width.toFloat(), trackY1, paint)
        canvas.drawLine(0f, trackY2, width.toFloat(), trackY2, paint)

        // Ties (Schwellen)
        for (i in 0..width step 40) {
            canvas.drawLine(i.toFloat(), trackY1 - 5, i.toFloat(), trackY2 + 5, paint)
        }

        // Train
        paint.color = Color.RED
        val trainWidth = 60f
        val trainHeight = 20f
        val trainX = (width - trainWidth) * progress
        val trainY = (height - trainHeight) / 2f
        canvas.drawRect(trainX, trainY, trainX + trainWidth, trainY + trainHeight, paint)

        // Train Front (rounded)
        canvas.drawCircle(trainX + trainWidth, trainY + trainHeight / 2, trainHeight / 2, paint)

        return bitmap
    }
}