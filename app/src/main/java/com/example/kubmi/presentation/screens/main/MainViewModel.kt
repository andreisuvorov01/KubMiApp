package com.example.kubmi.presentation.screens.main

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.model.News
import com.example.kubmi.util.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import java.io.File
import org.json.JSONObject

/**
 * ViewModel for the main screen.
 *
 * Manages UI state for news display and handles data loading.
 * Uses StateFlow for reactive UI updates with proper lifecycle management.
 *
 * @property newsRepository Repository for news data operations
 *
 * @see [ViewModel documentation](https://developer.android.com/topic/libraries/architecture/viewmodel)
 * @see [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    // #region agent log
    private fun agentLog(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "run1"
    ) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to runId,
                "hypothesisId" to hypothesisId,
                "location" to location,
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            File("d:\\AndroidProject\\.cursor\\debug.log").appendText(
                JSONObject(payload).toString() + "\n"
            )
        } catch (_: Exception) {
            // keep ViewModel safe
        }
    }
    // #endregion

    /**
     * UI state for news list.
     * Uses StateFlow to emit news data reactively.
     * Automatically stops collecting when UI is not active (WhileSubscribed).
     */
    val news: StateFlow<List<News>> = newsRepository.getAllNews()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = emptyList()
        )

    private val _bellScheduleText = MutableStateFlow(securePreferences.getBellScheduleText())
    val bellScheduleText: StateFlow<String> = _bellScheduleText.asStateFlow()

    private val _mainScreenTitle = MutableStateFlow(securePreferences.getMainScreenTitle())
    val mainScreenTitle: StateFlow<String> = _mainScreenTitle.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "bell_schedule_text" -> {
                _bellScheduleText.value = prefs.getString(key, "") ?: ""
            }
            "main_screen_title" -> {
                _mainScreenTitle.value = prefs.getString(key, "") ?: ""
            }
        }
    }

    init {
        securePreferences.registerPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        super.onCleared()
        securePreferences.unregisterPreferenceChangeListener(prefsListener)
    }

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    /**
     * Refreshes news data from remote source.
     * Should be called when user explicitly requests data refresh.
     */
    fun refreshNews() {
        if (_refreshState.value is RefreshState.Loading) return // Избегаем дублирующих запросов
        
        viewModelScope.launch {
            agentLog(
                hypothesisId = "H1",
                location = "MainViewModel:refreshNews",
                message = "Refresh start",
                data = mapOf("currentState" to _refreshState.value::class.java.simpleName)
            )
            _refreshState.value = RefreshState.Loading
            try {
                newsRepository.refreshNews()
                _refreshState.value = RefreshState.Success
                agentLog(
                    hypothesisId = "H1",
                    location = "MainViewModel:refreshNews",
                    message = "Refresh success",
                    data = mapOf("newsCount" to news.value.size)
                )
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error(e.message ?: "Неизвестная ошибка")
                Timber.e(e, "Error refreshing news")
                agentLog(
                    hypothesisId = "H1",
                    location = "MainViewModel:refreshNews",
                    message = "Refresh error",
                    data = mapOf("error" to (e.message ?: e::class.java.simpleName))
                )
            }
        }
    }
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Loading : RefreshState()
    object Success : RefreshState()
    data class Error(val message: String) : RefreshState()
}