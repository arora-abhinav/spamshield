package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class PredictionsResponse(
    @SerializedName("Predictions") val predictions: List<IndividualPrediction>
)

data class IndividualPrediction(
    @SerializedName("ID") val id: Int?,
    @SerializedName("Classification") val classification: String?,
    @SerializedName("Confidence") val confidence: Double?,
    @SerializedName("Timestamp") val timestamp: String?
)
