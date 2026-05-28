package com.example.spamshield.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val predictionId: Int,
    val messageText: String,
    val classification: String,
    val confidence: Double,
    val timestamp: String,
    val sender: String,
    val feedbackGiven: Boolean = false,
    val userCorrection: String? = null
)
