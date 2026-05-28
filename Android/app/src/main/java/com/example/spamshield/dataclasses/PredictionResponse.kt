package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("Classification") val classification: String,
    @SerializedName("Confidence") val confidence: Double,
    @SerializedName("Prediction ID") val predictionId: Int,
    @SerializedName("Time") val time: String
)

data class BatchPredictionsResponse(
    @SerializedName("Batch Predictions") val batchPredictions: List<PredictionResponse>
)
