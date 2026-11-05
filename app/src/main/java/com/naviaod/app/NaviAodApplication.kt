package com.naviaod.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NaviAodApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_AOD_SERVICE,
                "NaviAOD Active Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps NaviAOD responsive while navigation is active"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_AOD_SERVICE = "naviaod_service_channel"
        lateinit var instance: NaviAodApplication
            private set
    }
}
