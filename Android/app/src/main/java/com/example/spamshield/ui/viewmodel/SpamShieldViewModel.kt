package com.example.spamshield.ui.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spamshield.data.local.MessageEntity
import com.example.spamshield.data.repository.SpamShieldRepository
import com.example.spamshield.dataclasses.StatisticsResponse
import com.example.spamshield.token.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

@HiltViewModel
class SpamShieldViewModel @Inject constructor(
    private val repository: SpamShieldRepository
) : ViewModel() {

    val allMessages: StateFlow<List<MessageEntity>> = repository.getMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val spamMessages: StateFlow<List<MessageEntity>> = repository.getSpamMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todayMessages: StateFlow<List<MessageEntity>> = repository.getTodaysMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _statisticsState = MutableStateFlow<UiState<StatisticsResponse>>(UiState.Idle)
    val statisticsState: StateFlow<UiState<StatisticsResponse>> = _statisticsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _optedIn = MutableStateFlow(false)
    val optedIn: StateFlow<Boolean> = _optedIn.asStateFlow()

    fun registerIfNeeded() {
        viewModelScope.launch {
            try {
                repository.registerIfNeeded()
            } catch (e: Exception) {
                _errorMessage.value = "Registration failed: ${e.message}"
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _statisticsState.value = UiState.Loading
            try {
                repository.registerIfNeeded()
            } catch (e: Exception) {
                _statisticsState.value = UiState.Error("Registration failed: ${e.message}")
                return@launch
            }
            repository.fetchStatistics()
                .onSuccess { _statisticsState.value = UiState.Success(it) }
                .onFailure { _statisticsState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun submitFeedback(predictionId: Int, actual: String, messageText: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.submitFeedback(predictionId, actual, messageText)
            } catch (e: Exception) {
                _errorMessage.value = "Feedback failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setConsent(optIn: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.setConsent(optIn)
                    .onSuccess { _optedIn.value = optIn }
                    .onFailure { _errorMessage.value = "Could not update consent: ${it.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStoredSpam() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteStoredSpam()
                    .onFailure { _errorMessage.value = "Delete failed: ${it.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInboxMessages(context: Context) {
        if (TokenManager.hasSyncedInbox(context)) return
        viewModelScope.launch {
            try {
                repository.registerIfNeeded()
            } catch (e: Exception) {
                _errorMessage.value = "Registration failed: ${e.message}"
                return@launch
            }

            val senders = mutableListOf<String>()
            val bodies = mutableListOf<String>()

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                null, null,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                var count = 0
                while (it.moveToNext() && count < 100) {
                    val sender = it.getString(addressIdx) ?: "Unknown"
                    val body = it.getString(bodyIdx) ?: continue
                    senders.add(sender)
                    bodies.add(body)
                    count++
                }
            }

            if (bodies.isEmpty()) {
                TokenManager.setHasSyncedInbox(context, true)
                return@launch
            }

            repository.predictMultiple(senders, bodies)
                .onSuccess { TokenManager.setHasSyncedInbox(context, true) }
                .onFailure { _errorMessage.value = "Failed to classify inbox: ${it.message}" }
        }
    }

    fun loadOptedInState(context: Context) {
        _optedIn.value = TokenManager.isOptedIn(context)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
