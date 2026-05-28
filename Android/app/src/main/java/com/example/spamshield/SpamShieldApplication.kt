package com.example.spamshield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpamShieldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "spam_alerts",
            "Spam Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for detected spam messages"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
