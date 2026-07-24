package com.example.kubmi.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.kubmi.MainActivity
import com.example.kubmi.R
import com.example.kubmi.service.KioskService

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.device_admin_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.device_admin_disabled), Toast.LENGTH_SHORT).show()
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        Log.i(TAG, "Lock Task entered by $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        Log.w(TAG, "Lock Task exited")
        if (!KioskService.isTemporaryExitAllowed(context)) {
            KioskService.start(context)
            runCatching {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }.onFailure {
                Log.e(TAG, "Could not restore kiosk after unexpected Lock Task exit", it)
            }
        }
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
}





