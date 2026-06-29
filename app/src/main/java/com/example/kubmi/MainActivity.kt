package com.example.kubmi

import android.app.ActivityOptions
import android.content.Intent
import android.app.UiModeManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.compose.animation.ExperimentalAnimationApi
import com.example.kubmi.presentation.navigation.NavGraph
import com.example.kubmi.presentation.screens.news.NewsViewModel
import com.example.kubmi.service.KioskService
import com.example.kubmi.ui.theme.KubMiTheme
import com.example.kubmi.kiosk.AdvancedKioskManager
import com.example.kubmi.kiosk.GaokeViewKioskDetector
import com.example.kubmi.kiosk.KeyEventBlocker
import com.example.kubmi.util.KioskManager
import com.example.kubmi.util.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var advancedKioskManager: AdvancedKioskManager

    private val securePreferences by lazy { SecurePreferences(this) }
    
    private val launcherPrefs by lazy { getSharedPreferences("launcher_prefs", MODE_PRIVATE) }
    private val kioskPrefs by lazy { getSharedPreferences(KioskService.PREF_KIOSK_GUARD, MODE_PRIVATE) }
    private val returnHandler = Handler(Looper.getMainLooper())

    // Screensaver related variables
    private val screensaverHandler = Handler(Looper.getMainLooper())
    private val showScreensaver = mutableStateOf(false)
    private val screensaverRunnable = Runnable { showScreensaver.value = true }

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_LAUNCHER_HELP_SEEN = "launcher_help_seen"
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1000
    }

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup advanced kiosk mode
        advancedKioskManager.setupFullKioskMode()
        if (advancedKioskManager.isDeviceOwner() &&
            !GaokeViewKioskDetector.isGaokeViewKioskActive(this)) {
            advancedKioskManager.startLockTask(this)
            Log.d(TAG, "Device Owner mode - Lock Task started")
        }
        
        KioskManager.enableKioskMode(this)
        KioskService.start(this)

        checkOverlayPermission()
        startOverlayServiceIfAllowed()

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            KubMiTheme {
                val showLauncherPrompt = rememberSaveable { mutableStateOf(shouldShowLauncherHelp()) }
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    val navController = rememberAnimatedNavController()
                    val newsViewModel: NewsViewModel = hiltViewModel()
                    val sftpSlides by newsViewModel.sftpSlidesState.collectAsState()
                    val sftpReady by newsViewModel.sftpReadyState.collectAsState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(navController = navController)

                        if (showLauncherPrompt.value) {
                            LauncherSelectionDialog(
                                onOpenSettings = {
                                    openHomeSettings()
                                    markLauncherHelpShown()
                                    showLauncherPrompt.value = false
                                },
                                onDismiss = {
                                    markLauncherHelpShown()
                                    showLauncherPrompt.value = false
                                }
                            )
                        }

                        // Display screensaver only when SFTP slides are ready
                        if (showScreensaver.value && sftpReady) {
                            ScreensaverContent(imageUrls = sftpSlides)
                        }
                    }
                }
            }
        }
        resetScreensaverTimer()
    }

    private fun resetScreensaverTimer() {
        val delayMs = securePreferences.getScreensaverDelay() * 1000L
        screensaverHandler.removeCallbacks(screensaverRunnable)
        screensaverHandler.postDelayed(screensaverRunnable, delayMs)
        if (showScreensaver.value) showScreensaver.value = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (showScreensaver.value) {
            showScreensaver.value = false
            resetScreensaverTimer()
            return true
        }
        resetScreensaverTimer()
        return super.dispatchTouchEvent(ev)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetScreensaverTimer()
    }

    override fun onStart() {
        super.onStart()
        KioskService.start(this)
    }

    override fun onResume() {
        super.onResume()
        returnHandler.removeCallbacksAndMessages(null)
        KeyEventBlocker.paused = false
        KeyEventBlocker.foregroundReturnPaused = false
        KioskManager.enableKioskMode(this)
        resetScreensaverTimer()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (isBlockedKey(keyCode)) true else super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Use advanced key blocker
        if (event.action == KeyEvent.ACTION_DOWN && KeyEventBlocker.shouldBlockKey(event)) {
            Log.w(TAG, "Blocked key event: keyCode=${event.keyCode}, meta=${event.metaState}")
            return true
        }
        
        // Legacy blocker for backward compatibility
        if (event.action == KeyEvent.ACTION_DOWN && isBlockedKeyEvent(event)) {
            Log.w(TAG, "Blocked kiosk key event: keyCode=${event.keyCode}, meta=${event.metaState}")
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        // Disable hardware back to prevent leaving the kiosk flow.
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isTemporaryExitAllowed() && !isSystemLockTaskActive()) {
            bringAppToForeground()
        }
    }

    override fun onPause() {
        super.onPause()
        screensaverHandler.removeCallbacks(screensaverRunnable)
        scheduleReturnIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        screensaverHandler.removeCallbacks(screensaverRunnable)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskManager.enableKioskMode(this)
        } else {
            scheduleReturnIfNeeded()
        }
    }

    override fun onStop() {
        super.onStop()
        scheduleReturnIfNeeded()
    }

    private fun isBlockedKey(keyCode: Int): Boolean = isBlockedKeyCode(keyCode)

    private fun isBlockedKeyEvent(event: KeyEvent): Boolean {
        if (isBlockedKeyCode(event.keyCode)) return true
        val ctrl = event.isCtrlPressed
        val alt = event.isAltPressed
        val shift = event.isShiftPressed
        val meta = event.isMetaPressed
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> alt
            KeyEvent.KEYCODE_TAB -> alt || meta
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD -> alt
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_U -> ctrl
            KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J -> ctrl && shift
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_M -> meta
            else -> false
        }
    }

    private fun isBlockedKeyCode(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_F4, KeyEvent.KEYCODE_F11, KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_SYSRQ, KeyEvent.KEYCODE_WINDOW -> true
            else -> false
        }
    }

    private fun shouldShowLauncherHelp(): Boolean =
        !launcherPrefs.getBoolean(KEY_LAUNCHER_HELP_SEEN, false)

    private fun markLauncherHelpShown() {
        launcherPrefs.edit().putBoolean(KEY_LAUNCHER_HELP_SEEN, true).apply()
    }

    private fun openHomeSettings() {
        allowTemporaryExit()
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
        intents.firstOrNull { intent ->
            intent.resolveActivity(packageManager) != null
        }?.let { startActivity(it) }
    }

    private fun bringAppToForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        startActivity(intent, options.toBundle())
    }

    private fun scheduleReturnIfNeeded() {
        if (isTemporaryExitAllowed()) return
        if (KeyEventBlocker.foregroundReturnPaused) return
        // When the board's own kiosk (Lock Task) is active, it handles return — we don't.
        if (isSystemLockTaskActive()) return
        returnHandler.removeCallbacksAndMessages(null)
        returnHandler.postDelayed({ bringAppToForeground() }, 2000)
    }

    private fun isSystemLockTaskActive(): Boolean {
        return try {
            val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                am.isInLockTaskMode
            }
        } catch (_: Exception) { false }
    }

    private fun allowTemporaryExit() {
        kioskPrefs.edit()
            .putLong(
                KioskService.KEY_ALLOW_EXIT_UNTIL,
                System.currentTimeMillis() + KioskService.ALLOW_EXIT_WINDOW_MS
            )
            .apply()
    }

    private fun isTemporaryExitAllowed(): Boolean {
        val until = kioskPrefs.getLong(KioskService.KEY_ALLOW_EXIT_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    private fun startOverlayServiceIfAllowed() {
        if (isTvDevice()) return // Skip overlay service on TV
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                packageManager.hasSystemFeature("android.software.leanback") ||
                packageManager.hasSystemFeature("android.hardware.type.television")
    }

    private fun checkOverlayPermission() {
        if (isTvDevice()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            if (intent.resolveActivity(packageManager) != null) {
                allowTemporaryExit()
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOverlayServiceIfAllowed()
            }
        }
    }
}

@Composable
private fun LauncherSelectionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.launcher_help_title)) },
        text = { Text(text = stringResource(id = R.string.launcher_help_message, context.packageName)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(id = R.string.launcher_help_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.launcher_help_got_it))
            }
        }
    )
}
