package com.example.spamshield.data.repository

import android.content.Context
import com.example.spamshield.api.SpamShieldApi
import com.example.spamshield.data.local.MessageDao
import com.example.spamshield.data.local.MessageEntity
import com.example.spamshield.dataclasses.BatchPredictionsResponse
import com.example.spamshield.dataclasses.FeedbackRequest
import com.example.spamshield.dataclasses.OptOutResponse
import com.example.spamshield.dataclasses.PredictionsResponse
import com.example.spamshield.dataclasses.StatisticsResponse
import com.example.spamshield.token.TokenManager
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpamShieldRepository @Inject constructor(
    private val api: SpamShieldApi,
    private val dao: MessageDao,
    @ApplicationContext private val context: Context
) {

    suspend fun registerIfNeeded() {
        if (TokenManager.isRegistered(context)) return
        val response = api.register()
        if (response.isSuccessful) {
            val body = response.body()!!
            val deviceId = body.deviceId ?: extractDeviceIdFromJwt(body.accessToken)
            TokenManager.saveTokens(context, body.accessToken, body.refreshToken, deviceId)
            TokenManager.setRegistered(context, true)
        } else {
            throw Exception("Registration failed: ${response.code()}")
        }
    }

    private fun extractDeviceIdFromJwt(token: String): String? {
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            JSONObject(String(decoded, Charsets.UTF_8)).getString("device_id")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun predictOne(message: String, sender: String): Result<MessageEntity> {
        return try {
            val body = message.toRequestBody("text/plain".toMediaType())
            val response = api.predict(body)
            if (response.isSuccessful) {
                val prediction = response.body()!!
                val entity = MessageEntity(
                    predictionId = prediction.predictionId,
                    messageText = message,
                    classification = prediction.classification.lowercase(),
                    confidence = prediction.confidence,
                    timestamp = prediction.time,
                    sender = sender
                )
                dao.insert(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception("Prediction failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun predictMultiple(messages: List<String>): Result<BatchPredictionsResponse> {
        return try {
            val response = api.predictMultiple(messages)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Batch prediction failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(): Flow<List<MessageEntity>> = dao.getAllMessages()

    fun getSpamMessages(): Flow<List<MessageEntity>> = dao.getSpamMessages()

    fun getTodaysMessages(): Flow<List<MessageEntity>> = dao.getTodaysMessages()

    suspend fun fetchHistory(today: Boolean = false): Result<PredictionsResponse> {
        return try {
            val response = api.getPredictions(today)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Fetch history failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStatistics(): Result<StatisticsResponse> {
        return try {
            val response = api.getStatistics()
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Fetch statistics failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(predictionId: Int, actual: String, messageText: String?) {
        val messageToSend = if (TokenManager.isOptedIn(context) && actual == "spam") messageText else null
        val response = api.feedback(FeedbackRequest(predictionId, actual, messageToSend))
        if (response.isSuccessful) {
            dao.getByPredictionId(predictionId)?.let { entity ->
                dao.update(entity.copy(feedbackGiven = true, userCorrection = actual))
            }
        }
    }

    suspend fun setConsent(optIn: Boolean): Result<Unit> {
        return try {
            val response = api.allowMessages(optIn)
            if (response.isSuccessful) {
                TokenManager.setOptedIn(context, optIn)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Set consent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun optOut(): Result<OptOutResponse> {
        return try {
            val response = api.optOut()
            if (response.isSuccessful) {
                TokenManager.setOptedIn(context, false)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Opt out failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStoredSpam(): Result<OptOutResponse> {
        return try {
            val response = api.deleteStoredSpam()
            if (response.isSuccessful) {
                dao.deleteAll()
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Delete stored spam failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
