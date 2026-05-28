package com.example.spamshield

data class PredictionsResponse(
    val predictions: List<IndividualPrediction>
)
data class IndividualPrediction(
    val classification: String?,
    val confidence: Float?,
    val timestamp: String?

)