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
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkPasswordStatus()
    }
    
    private fun checkPasswordStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val isPasswordSet = securePreferences.isPasswordSet()
                _authState.value = if (isPasswordSet) {
                    AuthState.PasswordSet
                } else {
                    AuthState.Initial
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to check password status: ${e.message}")
            }
        }
    }
    
    fun setPassword(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                securePreferences.savePassword(password)
                _authState.value = AuthState.PasswordSet
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to set password: ${e.message}")
            }
        }
    }
    
    fun authenticate(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val isAuthenticated = securePreferences.verifyPassword(password)
                _authState.value = if (isAuthenticated) {
                    AuthState.Authenticated
                } else {
                    AuthState.Error("Invalid password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            }
        }
    }
    
    fun resetPassword() {
        viewModelScope.launch {
            try {
                securePreferences.clearPassword()
                _authState.value = AuthState.Initial
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to reset password: ${e.message}")
            }
        }
    }
}