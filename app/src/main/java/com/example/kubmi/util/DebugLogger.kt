package com.example.kubmi.util

import android.util.Log

/**
 * Debug logger for kiosk mode debugging - uses Logcat
 */
object DebugLogger {
    private const val TAG = "KioskDebug"
    
    fun log(hypothesisId: String, location: String, message: String, data: Map<String, Any?> = emptyMap()) {
        val dataStr = data.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.d(TAG, "[$hypothesisId] $location: $message | $dataStr")
    }
    
    fun clear() {
        // Logcat doesn't need clearing
    }
}
