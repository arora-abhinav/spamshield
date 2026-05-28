package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class OptedInResponse(
    @SerializedName("Opted in") val optedIn: Boolean
)
