package com.example.kubmi

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.kubmi.presentation.navigation.NavGraph
import com.example.kubmi.service.KioskService
import com.example.kubmi.ui.theme.KubMiTheme
import com.example.kubmi.util.KioskManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val launcherPrefs by lazy { getSharedPreferences("launcher_prefs", MODE_PRIVATE) }
    private val kioskPrefs by lazy { getSharedPreferences(KioskService.PREF_KIOSK_GUARD, MODE_PRIVATE) }
    private val returnHandler = Handler(Looper.getMainLooper())

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KioskManager.enableKioskMode(this)
        KioskService.start(this)
        
        setContent {
            KubMiTheme {
                val showLauncherPrompt = rememberSaveable { mutableStateOf(shouldShowLauncherHelp()) }
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    val navController = rememberNavController()
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
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        KioskService.start(this)
    }

    override fun onResume() {
        super.onResume()
        KioskManager.enableKioskMode(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (isBlockedKey(keyCode)) true else super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && isBlockedKey(event.keyCode)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        // Disable hardware back to prevent leaving the kiosk flow.
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isTemporaryExitAllowed()) {
            bringAppToForeground()
        }
    }

    override fun onPause() {
        super.onPause()
        scheduleReturnIfNeeded()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            scheduleReturnIfNeeded()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskManager.enableKioskMode(this)
        }
    }

    private fun isBlockedKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_SEARCH -> true
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
        startActivity(intent)
    }

    private fun scheduleReturnIfNeeded() {
        if (isTemporaryExitAllowed()) return
        returnHandler.removeCallbacksAndMessages(null)
        returnHandler.postDelayed({ bringAppToForeground() }, 600)
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

    companion object {
        private const val KEY_LAUNCHER_HELP_SEEN = "launcher_help_seen"
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
