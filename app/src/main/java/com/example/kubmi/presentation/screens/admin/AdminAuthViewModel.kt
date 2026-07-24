package com.example.kubmi.presentation.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.model.AuthState
import com.example.kubmi.util.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            _authState.value = if (securePreferences.isPasswordSet()) {
                AuthState.RequiresAuth()
            } else {
                AuthState.Initial()
            }
        }
    }
    
    /**
     * Set password for the first time and immediately authenticate
     */
    fun setPassword(password: String, confirmation: String) {
        if (password != confirmation) {
            _authState.value = AuthState.Initial("Пароли не совпадают")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                withContext(Dispatchers.Default) {
                    securePreferences.savePassword(password)
                }
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Initial(
                    e.message ?: "Не удалось сохранить пароль"
                )
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
                val isAuthenticated = withContext(Dispatchers.Default) {
                    securePreferences.verifyPassword(password)
                }
                _authState.value = if (isAuthenticated) {
                    AuthState.Authenticated
                } else {
                    val remainingSeconds =
                        (securePreferences.getRemainingLockoutMillis() + 999L) / 1000L
                    val error = if (remainingSeconds > 0L) {
                        "Слишком много попыток. Повторите через $remainingSeconds сек."
                    } else {
                        "Неверный пароль"
                    }
                    AuthState.RequiresAuth(errorMessage = error)
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
                _authState.value = AuthState.Initial()
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
