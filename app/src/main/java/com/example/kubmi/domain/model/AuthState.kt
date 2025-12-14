package com.example.kubmi.domain.model

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object PasswordSet : AuthState()
    data class Error(val message: String) : AuthState()
}