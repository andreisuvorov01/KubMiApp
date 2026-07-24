package com.example.kubmi.util

import android.util.Log

/**
 * Debug logger for kiosk mode debugging - uses Logcat
 */
object DebugLogger {
    private const val TAG = "KioskDebug"
    
    fun log(hypothesisId: String, location: String, message: String, data: Map<String, Any?> = emptyMap()) {
        // logging disabled
    }
    
    fun clear() {
        // Logcat doesn't need clearing
    }
}
