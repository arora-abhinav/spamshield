package com.example.spamshield

import android.R
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SpamshieldAPI {
    @GET("/statistics")
    fun get_statistics(): Response<StatisticsResponse>

    @GET("/predictions_today/")
    fun get_predictions(@Path("today") today: R.bool): Response<PredictionsResponse>

    @GET("/register")
    fun register(): Response<AuthorizationResponse>

    @GET("/refresh")
    fun refresh(): Response<AuthorizationResponse>

    @GET("/health")
    fun health(): Response<HealthResponse>

    @POST("/allow_messages/")
    fun allow_messages(@Path("opt_in") opt_in: R.bool): Response<OptedInResponse>

    @POST(value = "/predict")
    fun predict(@Body message: String): Response<PredictionsResponse>

    @POST(value = "/predict-multiple")
    fun predict_multiple(@Body message: List<String>): Response<multiplePredictionsResponse>

    @POST("/feedback")
    fun feedback(@Body giveFeedback: FeedbackRequest): Response<FeedbackResponse>

    @DELETE("/opt_out")
    fun opt_out(): Response<OptOutResponse>

    @DELETE("/delete_stored_spam")
    //Note: OptOutResponse has the same fields required for the delete_stored_spam endpoint
    fun delete_stored_spam(): Response<OptOutResponse>

}