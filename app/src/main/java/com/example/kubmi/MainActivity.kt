package com.example.kubmi

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.kubmi.presentation.navigation.NavGraph
import com.example.kubmi.ui.theme.KubMiTheme
import com.example.kubmi.util.KioskManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KioskManager.enableKioskMode(this)
        
        setContent {
            KubMiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KioskManager.enableKioskMode(this)
    }

    override fun onPause() {
        super.onPause()
        KioskManager.restoreFocus(this)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (KioskManager.isBlockedKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KioskManager.isBlockedKey(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (KioskManager.handleTouchEvent(ev)) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        KioskManager.restoreFocus(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            KioskManager.enableKioskMode(this)
        } else {
            KioskManager.restoreFocus(this)
        }
    }
}