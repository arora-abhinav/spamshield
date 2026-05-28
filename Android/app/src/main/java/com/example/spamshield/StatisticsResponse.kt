package com.example.spamshield

//Used to have names with spaces in them
import com.google.gson.annotations.SerializedName

data class StatisticsResponse(
    @SerializedName("Average Confidence All") val averageConfidenceAll: Double?,
    @SerializedName("Average Confidence Spam") val averageConfidenceSpam: Double?,
    @SerializedName("Feedback Count") val feedbackCount: Int?,
    @SerializedName("Ham Count") val hamCount: Int?,
    @SerializedName("Month Spam Count") val monthSpamCount: Int?,
    @SerializedName("Spam Count") val spamCount: Int?,
    @SerializedName("Spam Percentage") val spamPercentage: Double?,
    @SerializedName("Today Spam Count") val todaySpamCount: Int?,
    @SerializedName("Total Messages") val totalMessages: Int?,
    @SerializedName("Week Spam Count") val weekSpamCount: Int?,
    @SerializedName("Weekly Spam Distribution") val weeklySpamDistribution: List<Any>?
)