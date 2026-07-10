package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

@android.annotation.SuppressLint("NotificationPermission")
class SafeJourneyService : Service() {

    private var destination: String = "Unknown Destination"
    private var etaMinutes: Int = 0
    private var checkInSeconds: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_JOURNEY

        if (intent != null) {
            destination = intent.getStringExtra(EXTRA_DESTINATION) ?: destination
            etaMinutes = intent.getIntExtra(EXTRA_ETA, etaMinutes)
            checkInSeconds = intent.getIntExtra(EXTRA_NEXT_CHECK_IN, checkInSeconds)
        }

        when (action) {
            ACTION_START_JOURNEY -> {
                isServiceRunning = true
                val notification = createNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE_STATUS -> {
                if (isServiceRunning) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, createNotification())
                }
            }
            ACTION_STOP_JOURNEY -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "safe_journey_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Safe Shield Live Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active safe journey shield and check-in tracking"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedCheckIn = if (checkInSeconds >= 60) {
            "${checkInSeconds / 60}m ${checkInSeconds % 60}s"
        } else {
            "${checkInSeconds}s"
        }

        val contentText = "Destination: $destination • ETA: ${etaMinutes}m\nNext check-in in $formattedCheckIn"

        val iconRes = if (applicationInfo.icon != 0) applicationInfo.icon else android.R.drawable.ic_menu_compass

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safe Shield Tracking Active")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 9120

        const val ACTION_START_JOURNEY = "com.example.service.ACTION_START_JOURNEY"
        const val ACTION_UPDATE_STATUS = "com.example.service.ACTION_UPDATE_STATUS"
        const val ACTION_STOP_JOURNEY = "com.example.service.ACTION_STOP_JOURNEY"

        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_ETA = "eta"
        const val EXTRA_NEXT_CHECK_IN = "next_check_in"

        @Volatile
        var isServiceRunning = false
            private set
    }
}
