package com.example.spamshield.dataclasses

import com.google.gson.annotations.SerializedName

data class AuthorizationResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("device_id") val deviceId: String?
)
