package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class WeeklyDistributionItem(
    @SerializedName("Count") val count: Int,
    @SerializedName("Date") val date: String
)
