package com.example.spamshield

import com.google.gson.annotations.SerializedName

data class multiplePredictionsResponse(
    @SerializedName("Batch Predictions") val BatchPredictions: List<PredictionsResponse>
)


data class PredictionResponse(
    val Classification: String,
    val Confidence: String,
    @SerializedName("Prediction ID") val PredictionID: String,
    val Time: String
)