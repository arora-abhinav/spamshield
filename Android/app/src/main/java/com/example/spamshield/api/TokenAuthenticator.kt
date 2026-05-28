package com.example.spamshield.api

import android.content.Context
import com.example.spamshield.token.TokenManager
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://impartial-perception-production-44ad.up.railway.app/"
private const val RETRY_HEADER = "X-Retry-After-Refresh"

class TokenAuthenticator(private val context: Context) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(RETRY_HEADER) != null) return null

        val refreshToken = TokenManager.getRefreshToken(context) ?: run {
            TokenManager.clearAll(context)
            return null
        }

        return synchronized(this) {
            val currentToken = TokenManager.getAccessToken(context)
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentToken != null && currentToken != requestToken) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header(RETRY_HEADER, "true")
                    .build()
            } else {
                val refreshApi = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpClient.Builder().build())
                    .build()
                    .create(SpamShieldApi::class.java)

                val refreshResponse = refreshApi
                    .refreshSync("Bearer $refreshToken")
                    .execute()

                if (refreshResponse.isSuccessful) {
                    val body = refreshResponse.body()!!
                    TokenManager.saveNewTokens(context, body.accessToken, body.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.accessToken}")
                        .header(RETRY_HEADER, "true")
                        .build()
                } else {
                    TokenManager.clearAll(context)
                    null
                }
            }
        }
    }
}
