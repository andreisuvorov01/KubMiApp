package com.example.kubmi.domain.model

/**
 * Sealed class для представления состояния загрузки данных
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val exception: Exception? = null) : UiState<Nothing>()
    
    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(message: String, exception: Exception? = null) = Error(message, exception)
        val loading = Loading
    }
}