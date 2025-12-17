package com.example.kubmi.presentation.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.model.AboutPageContent
import com.example.kubmi.domain.repository.AboutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val aboutRepository: AboutRepository
) : ViewModel() {

    private val _aboutContent = MutableStateFlow<AboutPageContent?>(null)
    val aboutContent: StateFlow<AboutPageContent?> = _aboutContent.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAboutContent()
    }

    private fun loadAboutContent() {
        viewModelScope.launch {
            try {
                aboutRepository.getAboutContent().collect { content ->
                    _aboutContent.value = content
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun refreshContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { aboutRepository.refreshAboutContent() }
                .onSuccess {
                    _aboutContent.value = it
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }
}