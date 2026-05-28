package com.example.spamshield

import com.google.gson.annotations.SerializedName

data class OptedInResponse(
    @SerializedName("Opted In") val optedIn: String
)