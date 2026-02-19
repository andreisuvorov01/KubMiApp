package com.example.kubmi.presentation.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.model.News
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _newsState = MutableStateFlow<List<News>>(emptyList())
    val newsState: StateFlow<List<News>> = _newsState.asStateFlow()

    val pdfSlidesState: Flow<List<News>> = newsRepository.getPdfSlides()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    init {
        loadNews()
        refreshNews()
    }

    private fun loadNews() {
        viewModelScope.launch {
            newsRepository.getAllNews()
                .map { newsList ->
                    newsList.sortedByDescending { it.timestamp }
                }
                .collect { news ->
                    _newsState.value = news
                }
        }
    }

    fun refreshNews() {
        if (_isLoading.value) return // Prevent multiple concurrent refreshes
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Only force network if we have no news at all
                val currentNews = _newsState.value
                val force = currentNews.isEmpty()
                newsRepository.refreshNews(forceNetwork = force)
            } catch (e: Exception) {
                Log.e("KubMI_NewsVM", "Error refreshing news", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun observeNewsById(id: String): Flow<News?> = newsRepository.observeNewsById(id)

    fun refreshNewsArticle(id: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null
            val started = System.currentTimeMillis()
            Log.i("KubMI_NewsVM", "refreshNewsArticle(): start id=$id thread=${Thread.currentThread().name}")
            try {
                withTimeout(35_000) {
                    newsRepository.refreshNewsArticle(id)
                }
            } catch (e: Exception) {
                _detailError.value = e.message ?: "Неизвестная ошибка"
                Log.e("KubMI_NewsVM", "refreshNewsArticle(): error id=$id", e)
            } finally {
                _detailLoading.value = false
                val dur = System.currentTimeMillis() - started
                Log.i("KubMI_NewsVM", "refreshNewsArticle(): end id=$id ms=$dur")
            }
        }
    }

    suspend fun getNewsById(id: String): News? = newsRepository.getNewsById(id)
}
