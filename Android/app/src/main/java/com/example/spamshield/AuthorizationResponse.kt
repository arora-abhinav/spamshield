package com.example.spamshield

data class AuthorizationResponse(
    val access_token: String,
    val refresh_token: String
)