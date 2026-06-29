package com.example.kubmi.presentation.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.model.AuthState
import com.example.kubmi.util.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAuthViewModel @Inject constructor(
    private val securePreferences: SecurePreferences
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkPasswordStatus()
    }
    
    private fun checkPasswordStatus() {
        viewModelScope.launch {
            // Password is always set (default: kubmiadmin), so always require auth
            _authState.value = AuthState.RequiresAuth()
        }
    }
    
    /**
     * Set password for the first time and immediately authenticate
     */
    fun setPassword(password: String) {
        if (password.isBlank()) {
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                securePreferences.savePassword(password)
                // After setting password, user is immediately authenticated
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Initial
            }
        }
    }
    
    /**
     * Authenticate with existing password
     */
    fun authenticate(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val isAuthenticated = securePreferences.verifyPassword(password)
                _authState.value = if (isAuthenticated) {
                    AuthState.Authenticated
                } else {
                    // Stay on login screen with error message
                    AuthState.RequiresAuth(errorMessage = "Неверный пароль")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.RequiresAuth(errorMessage = "Ошибка проверки пароля")
            }
        }
    }
    
    /**
     * Reset password and go back to initial setup
     */
    fun resetPassword() {
        viewModelScope.launch {
            try {
                securePreferences.clearPassword()
                _authState.value = AuthState.Initial
            } catch (e: Exception) {
                // Ignore errors during reset
            }
        }
    }
    
    /**
     * Logout - go back to requiring authentication
     */
    fun logout() {
        _authState.value = AuthState.RequiresAuth()
    }
}