package com.naviaod.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.naviaod.app.NaviAodApplication
import com.naviaod.app.R
import com.naviaod.app.ui.MainActivity

class AodForegroundService : Service() {

    private lateinit var speedometerService: PassiveSpeedometerService

    override fun onCreate() {
        super.onCreate()
        speedometerService = PassiveSpeedometerService(this)
        speedometerService.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        speedometerService.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NaviAodApplication.CHANNEL_AOD_SERVICE)
            .setContentTitle("NaviAOD Active")
            .setContentText("OLED Always-On Display is ready for Google Maps")
            .setSmallIcon(R.drawable.ic_speedometer)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, AodForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AodForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
