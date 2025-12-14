package com.example.kubmi.presentation.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.model.News
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _newsState = MutableStateFlow<List<News>>(emptyList())
    val newsState: StateFlow<List<News>> = _newsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadNews()
        refreshNews()
    }

    private fun loadNews() {
        viewModelScope.launch {
            newsRepository.getAllNews().collect { news ->
                _newsState.value = news
            }
        }
    }

    fun refreshNews() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                newsRepository.refreshNews()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getNewsById(id: String): News? = newsRepository.getNewsById(id)
}
