package com.example.kubmi.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logFile = File(context.filesDir, "admin_actions.log")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    fun logAction(action: String, details: String = "") {
        val timestamp = dateFormat.format(Date())
        val logEntry = "$timestamp | ACTION: $action | DETAILS: $details\n"
        
        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry)
            }
        } catch (e: IOException) {
            Log.e("AdminLogger", "Failed to write to log file", e)
        }
    }
    
    fun getLogContent(): String {
        return try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "No log entries found"
            }
        } catch (e: IOException) {
            "Error reading log file: ${e.message}"
        }
    }
    
    fun clearLogs() {
        try {
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: IOException) {
            Log.e("AdminLogger", "Failed to clear log file", e)
        }
    }
    
    fun getLogSize(): Long {
        return if (logFile.exists()) logFile.length() else 0L
    }
}