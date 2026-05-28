package com.example.spamshield.api

import com.example.spamshield.dataclasses.AuthorizationResponse
import com.example.spamshield.dataclasses.BatchPredictionsResponse
import com.example.spamshield.dataclasses.FeedbackRequest
import com.example.spamshield.dataclasses.FeedbackResponse
import com.example.spamshield.dataclasses.HealthResponse
import com.example.spamshield.dataclasses.OptOutResponse
import com.example.spamshield.dataclasses.OptedInResponse
import com.example.spamshield.dataclasses.PredictionResponse
import com.example.spamshield.dataclasses.PredictionsResponse
import com.example.spamshield.dataclasses.StatisticsResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SpamShieldApi {

    @GET("register")
    suspend fun register(): Response<AuthorizationResponse>

    @GET("refresh")
    fun refreshSync(@Header("Authorization") bearerToken: String): Call<AuthorizationResponse>

    @POST("predict")
    suspend fun predict(@Body message: RequestBody): Response<PredictionResponse>

    @POST("predict-multiple")
    suspend fun predictMultiple(@Body messages: List<String>): Response<BatchPredictionsResponse>

    @GET("predictions_today/{today}")
    suspend fun getPredictions(@Path("today") today: Boolean): Response<PredictionsResponse>

    @GET("statistics")
    suspend fun getStatistics(): Response<StatisticsResponse>

    @POST("feedback")
    suspend fun feedback(@Body request: FeedbackRequest): Response<FeedbackResponse>

    @POST("allow_messages/{opt_in}")
    suspend fun allowMessages(@Path("opt_in") optIn: Boolean): Response<OptedInResponse>

    @DELETE("opt_out")
    suspend fun optOut(): Response<OptOutResponse>

    @DELETE("delete_stored_spam")
    suspend fun deleteStoredSpam(): Response<OptOutResponse>

    @GET("health")
    suspend fun health(): Response<HealthResponse>
}
