package com.nruge.iceinfo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.nruge.iceinfo.model.*
import kotlinx.coroutines.*

class IceNotificationService : Service() {


    companion object {
        const val CHANNEL_ID = "ice_tracker_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.nruge.iceinfo.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    // Stop-Button in der Notification
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP) stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        registerReceiver(stopReceiver, IntentFilter(ACTION_STOP), RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(sampleTrainStatus))
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(stopReceiver)
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val status = TrainRepository.fetchTrainStatus()
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
            NotificationManager.IMPORTANCE_LOW
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
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val delayText = when {
            status.delayMinutes > 0 -> "+${status.delayMinutes} min"
            else -> "pünktlich"
        }
        val trackText = if (status.track.isNotEmpty()) "Gleis ${status.track}" else ""

        // Progress berechnen
        val progress = if (status.distanceLastToNext > 0) {
            val covered = status.distanceLastToNext - status.distanceToNext
            (covered.toFloat() / status.distanceLastToNext).coerceIn(0f, 1f)
        } else 0f

        val remoteViews = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.tv_speed, "${status.speed} km/h")
            setTextViewText(R.id.tv_train_info, "${status.trainType} ${status.trainNumber}")
            setTextViewText(R.id.tv_next_stop, "→ ${status.nextStop}")
            setTextViewText(R.id.tv_eta, "Ankunft: ${status.eta} $trackText")
            setTextViewText(R.id.tv_delay, delayText)
            if (status.delayMinutes > 0) {
                setTextColor(R.id.tv_delay, Color.RED)
            } else {
                setTextColor(R.id.tv_delay, Color.parseColor("#388E3C"))
            }
            setImageViewBitmap(R.id.iv_tracks, createTrainTrackBitmap(progress))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Beenden",
                stopIntent
            )
            .build()
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