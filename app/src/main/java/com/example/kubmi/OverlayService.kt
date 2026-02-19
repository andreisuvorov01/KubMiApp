package com.example.kubmi

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.app.UiModeManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var mWindowManager: WindowManager? = null
    private var mOverlayView: View? = null
    private val monitorHandler = Handler(Looper.getMainLooper())
    private val monitorRunnable = Runnable { checkAndRestoreAppFocus() }
    private var isTvDevice = false

    companion object {
        const val CHANNEL_ID = "OverlayServiceChannel"
        const val NOTIFICATION_ID = 1
        private const val MONITOR_INTERVAL_MS = 3000L // Check every 3 seconds
        private const val TAG = "OverlayService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Check if this is a TV device
        isTvDevice = isTvDevice()

        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = if (isTvDevice) "TV Kiosk Protection Active" else "Kiosk Protection Active"
        val notificationText = if (isTvDevice) "Monitoring TV kiosk mode" else "Ensuring focus retention."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Inflate the overlay layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mOverlayView = inflater.inflate(R.layout.overlay_layout, null)

        // Set up the WindowManager parameters for a full-screen invisible overlay
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Configure overlay parameters based on device type
        val overlayFlags = if (isTvDevice) {
            // For TV: More restrictive flags to prevent accidental interactions
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            // For mobile/tablet: Standard flags
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        // We use MATCH_PARENT for both width and height to cover the screen.
        // FLAG_NOT_FOCUSABLE: This window won't get focus, so the activity beneath can get focus.
        // FLAG_LAYOUT_IN_SCREEN: Allow the window to be laid out within the entire screen.
        // FLAG_LAYOUT_NO_LIMITS: Allow the window to extend outside the screen (if needed).
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            overlayFlags,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        try {
            mWindowManager?.addView(mOverlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view", e)
        }

        // Hide the text view as we want the overlay to be invisible focus guard
        val overlayTextView = mOverlayView?.findViewById<TextView>(R.id.overlay_text_view)
        overlayTextView?.visibility = View.GONE

        // Set background to transparent
        mOverlayView?.setBackgroundColor(0x00000000)

        // Start monitoring app focus (more frequent on TV devices)
        val monitorInterval = if (isTvDevice) MONITOR_INTERVAL_MS else MONITOR_INTERVAL_MS * 2
        monitorHandler.postDelayed(monitorRunnable, monitorInterval)

        Log.d(TAG, "OverlayService started successfully. TV device: $isTvDevice, Monitor interval: $monitorInterval ms")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Kiosk Overlay Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun isTvDevice(): Boolean {
        return try {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        } catch (e: Exception) {
            false
        }
    }

    private fun isAppInForeground(): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // Method 1: Check running app processes
            val appProcesses = activityManager.runningAppProcesses
            if (appProcesses != null) {
                for (appProcess in appProcesses) {
                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                        appProcess.processName == packageName) {
                        return true
                    }
                }
            }

            // Method 2: Check running tasks (for TV compatibility)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val runningTasks = activityManager.appTasks
                for (task in runningTasks) {
                    try {
                        val taskInfo = task.taskInfo
                        if (taskInfo.baseActivity?.packageName == packageName) {
                            return true
                        }
                    } catch (e: Exception) {
                        // Continue checking
                    }
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is in foreground", e)
            false
        }
    }

    private fun bringAppToForeground() {
        try {
            if (isTvDevice) {
                bringAppToForegroundForTV()
            } else {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
            Log.d(TAG, "Brought app to foreground (TV: $isTvDevice)")
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing app to foreground", e)
        }
    }

    private fun checkAndRestoreAppFocus() {
        val appInForeground = isAppInForeground()

        if (!appInForeground) {
            Log.w(TAG, "App not in foreground, restoring focus")
            bringAppToForeground()

            // For TV devices, add additional delay before next check
            if (isTvDevice) {
                monitorHandler.postDelayed(monitorRunnable, MONITOR_INTERVAL_MS * 2)
                return
            }
        }

        // Schedule next check
        val nextCheckDelay = if (isTvDevice) MONITOR_INTERVAL_MS else MONITOR_INTERVAL_MS * 2
        monitorHandler.postDelayed(monitorRunnable, nextCheckDelay)
    }

    /**
     * Enhanced method to bring app to foreground specifically for TV devices
     */
    private fun bringAppToForegroundForTV() {
        try {
            // First, try to bring existing activity to front
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)

            // For TV, also try to simulate home button press to ensure we're on top
            Thread.sleep(500) // Small delay

            // Additional TV-specific intent
            val tvIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(tvIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error bringing app to foreground on TV", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop monitoring
        monitorHandler.removeCallbacks(monitorRunnable)

        if (mOverlayView != null) {
            try {
                mWindowManager?.removeView(mOverlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            mOverlayView = null
        }

        Log.d(TAG, "OverlayService destroyed")
    }
}
