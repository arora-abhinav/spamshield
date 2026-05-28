package com.example.spamshield

data class FeedbackRequest(
    val prediction_id: String,
    val actual: String,
    val message: String?
)
data class FeedbackResponse(
    val Feedback: String
)
