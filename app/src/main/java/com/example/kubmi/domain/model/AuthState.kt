package com.example.kubmi.domain.model

sealed class AuthState {
    /** No password set yet - show setup screen */
    object Initial : AuthState()
    
    /** Loading/checking state */
    object Loading : AuthState()
    
    /** Password exists but user not authenticated - show login screen */
    data class RequiresAuth(val errorMessage: String? = null) : AuthState()
    
    /** User is authenticated - show admin panel */
    object Authenticated : AuthState()
}