package com.example.spamshield.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import com.example.spamshield.MainActivity
import com.example.spamshield.R
import com.example.spamshield.data.repository.SpamShieldRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsProcessingService : LifecycleService() {

    @Inject lateinit var repository: SpamShieldRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(PROCESSING_NOTIFICATION_ID, buildProcessingNotification())

        val message = intent?.getStringExtra("message")
        val sender = intent?.getStringExtra("sender") ?: "Unknown"

        if (message == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                repository.predictOne(message, sender).onSuccess { entity ->
                    if (entity.classification == "spam") {
                        showSpamNotification(sender, entity.confidence)
                    }
                }
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildProcessingNotification(): Notification {
        return NotificationCompat.Builder(this, "spam_alerts")
            .setContentTitle("SpamShield")
            .setContentText("Checking message…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showSpamNotification(sender: String, confidence: Double) {
        val confidencePct = (confidence * 100).toInt()
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "spam_alerts")
            .setContentTitle("⚠️ Spam Detected")
            .setContentText("Message from $sender classified as spam ($confidencePct% confidence)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val PROCESSING_NOTIFICATION_ID = 1001
    }
}
