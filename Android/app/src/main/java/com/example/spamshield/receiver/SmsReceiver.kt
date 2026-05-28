package com.example.spamshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.spamshield.service.SmsProcessingService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val grouped = mutableMapOf<String, StringBuilder>()
        messages.forEach { sms ->
            grouped.getOrPut(sms.originatingAddress ?: "Unknown") { StringBuilder() }
                .append(sms.messageBody)
        }

        grouped.forEach { (sender, body) ->
            val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
                putExtra("message", body.toString())
                putExtra("sender", sender)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
