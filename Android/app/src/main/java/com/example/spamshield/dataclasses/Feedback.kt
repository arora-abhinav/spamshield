package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class FeedbackRequest(
    val prediction_id: Int,
    val actual: String,
    val message: String? = null
)

data class FeedbackResponse(
    @SerializedName("Result") val result: String
)
