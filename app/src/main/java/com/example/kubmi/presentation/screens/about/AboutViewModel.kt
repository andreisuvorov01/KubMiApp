package com.example.kubmi.presentation.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.repository.AboutRepository
import com.example.kubmi.domain.model.AboutContent
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
    
    private val _aboutContent = MutableStateFlow<AboutContent?>(null)
    val aboutContent: StateFlow<AboutContent?> = _aboutContent.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAboutContent()
    }
    
    private fun loadAboutContent() {
        viewModelScope.launch {
            aboutRepository.getAboutContent().collect { content ->
                _aboutContent.value = content
            }
        }
    }
    
    fun refreshContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val content = aboutRepository.refreshAboutContent()
                _aboutContent.value = content
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}