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

    private val _historyState = MutableStateFlow<UiState<List<com.example.spamshield.data.local.MessageEntity>>>(UiState.Idle)
    val historyState: StateFlow<UiState<List<com.example.spamshield.data.local.MessageEntity>>> = _historyState.asStateFlow()

    private val _optedIn = MutableStateFlow(false)
    val optedIn: StateFlow<Boolean> = _optedIn.asStateFlow()

    private val _previousMsgConsent = MutableStateFlow(false)
    val previousMsgConsent: StateFlow<Boolean> = _previousMsgConsent.asStateFlow()

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
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val cached = repository.statistics.value
        if (cached != null && repository.statisticsCacheDate == today) {
            _statisticsState.value = UiState.Success(cached)
            return
        }
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

    fun refreshStatisticsInBackground() {
        viewModelScope.launch {
            repository.fetchStatistics()
                .onSuccess { _statisticsState.value = UiState.Success(it) }
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

    fun loadHistory() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val cached = repository.historyCache.value
        if (cached != null && repository.historyCacheDate == today) {
            _historyState.value = UiState.Success(cached)
            return
        }
        if (_historyState.value is UiState.Loading) return
        viewModelScope.launch {
            _historyState.value = UiState.Loading
            repository.fetchAndMergeHistory()
                .onSuccess { _historyState.value = UiState.Success(it) }
                .onFailure { _historyState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun loadPreviousMsgConsent(context: Context) {
        _previousMsgConsent.value = TokenManager.getPreviousMsgConsent(context)
    }

    fun setPreviousMsgConsent(context: Context, granted: Boolean) {
        TokenManager.setPreviousMsgConsent(context, granted)
        _previousMsgConsent.value = granted
        if (granted) TokenManager.setHasSyncedInbox(context, false)
    }

    fun loadInboxMessages(context: Context) {
        android.util.Log.d("SpamShield", "loadInboxMessages called, hasSynced=${TokenManager.hasSyncedInbox(context)}")
        if (TokenManager.hasSyncedInbox(context)) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                try {
                    repository.registerIfNeeded()
                    android.util.Log.d("SpamShield", "loadInboxMessages: registration OK")
                } catch (e: Exception) {
                    android.util.Log.e("SpamShield", "loadInboxMessages: registration failed: ${e.message}")
                    _errorMessage.value = "Registration failed: ${e.message}"
                    return@launch
                }

                val senders = mutableListOf<String>()
                val bodies = mutableListOf<String>()
                val timestamps = mutableListOf<String>()

                var rawCount = 0
                var skipped = 0
                try {
                    val cursor = context.contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                        null, null,
                        "${Telephony.Sms.DATE} DESC"
                    )
                    if (cursor == null) {
                        _errorMessage.value = "Could not access SMS inbox — please verify SMS permission is granted."
                        return@launch
                    }
                    cursor.use {
                        rawCount = it.count
                        android.util.Log.d("SpamShield", "loadInboxMessages: inbox cursor count=$rawCount")
                        val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                        val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                        val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        var count = 0
                        while (it.moveToNext() && count < 100) {
                            val sender = it.getString(addressIdx) ?: "Unknown"
                            val body = it.getString(bodyIdx)
                            if (body == null) { skipped++; continue }
                            val dateMs = it.getLong(dateIdx)
                            senders.add(sender)
                            bodies.add(body)
                            timestamps.add(sdf.format(java.util.Date(dateMs)))
                            count++
                        }
                    }
                    android.util.Log.d("SpamShield", "loadInboxMessages: added ${bodies.size}, skipped $skipped of $rawCount")
                } catch (e: Exception) {
                    android.util.Log.e("SpamShield", "loadInboxMessages: cursor error: ${e.message}")
                    _errorMessage.value = "Could not read SMS messages: ${e.message}"
                    return@launch
                }

                if (bodies.isEmpty()) {
                    val msg = if (rawCount == 0) {
                        "SMS inbox returned 0 messages. If you use Google Messages or another RCS app, those messages are not traditional SMS and cannot be read by third-party apps."
                    } else {
                        "Found $rawCount messages but all $skipped had no text (may be picture/MMS messages)."
                    }
                    android.util.Log.w("SpamShield", "loadInboxMessages: $msg")
                    _errorMessage.value = msg
                    return@launch
                }

                android.util.Log.d("SpamShield", "loadInboxMessages: calling predictMultiple with ${bodies.size} messages")
                repository.predictMultiple(senders, bodies, timestamps)
                    .onSuccess {
                        android.util.Log.d("SpamShield", "loadInboxMessages: predictMultiple succeeded")
                        TokenManager.setHasSyncedInbox(context, true)
                        refreshStatisticsInBackground()
                    }
                    .onFailure {
                        android.util.Log.e("SpamShield", "loadInboxMessages: predictMultiple failed: ${it.message}")
                        _errorMessage.value = "Failed to classify inbox: ${it.message}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOptedInState(context: Context) {
        _optedIn.value = TokenManager.isOptedIn(context)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
