package com.example.kubmi.presentation.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.model.News
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
    private val newsRepository: NewsRepository
) : ViewModel() {

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

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    /**
     * Refreshes news data from remote source.
     * Should be called when user explicitly requests data refresh.
     */
    fun refreshNews() {
        if (_refreshState.value is RefreshState.Loading) return // Избегаем дублирующих запросов
        
        viewModelScope.launch {
            _refreshState.value = RefreshState.Loading
            try {
                newsRepository.refreshNews()
                _refreshState.value = RefreshState.Success
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error(e.message ?: "Неизвестная ошибка")
                Timber.e(e, "Error refreshing news")
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