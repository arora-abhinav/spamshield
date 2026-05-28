package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class OptOutResponse(
    @SerializedName("Result") val result: String
)
