package com.example.kubmi.kiosk

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object GaokeViewKioskDetector {

    private const val TAG = "GaokeViewDetector"

    private val GAOKE_PACKAGES = listOf(
        "com.gaokeview.launcher",
        "com.gaokeview.settings",
        "com.gaokeview.toolbar",
        "com.gaokeview.floatmenu",
        "com.gaokeview.sidebar",
        "com.gaokeview.whiteboard",
        "com.gaoke.whiteboard",
        "com.gaoke.toolbar",
        "com.gaoke.launcher"
    )

    data class GaokeViewStatus(
        val isDeviceOwner: Boolean,
        val isLockTaskActive: Boolean,
        val isDefaultLauncher: Boolean,
        val installedPackages: List<String>,
        val isKioskModeActive: Boolean
    )

    fun detect(context: Context): GaokeViewStatus {
        val deviceOwner = isGaokeViewDeviceOwner(context)
        val lockTask = isLockTaskActive(context)
        val launcher = isGaokeViewDefaultLauncher(context)
        val installed = getInstalledGaokePackages(context)
        val active = deviceOwner || lockTask || launcher || installed.isNotEmpty()

        return GaokeViewStatus(
            isDeviceOwner = deviceOwner,
            isLockTaskActive = lockTask,
            isDefaultLauncher = launcher,
            installedPackages = installed,
            isKioskModeActive = active
        )
    }

    fun isGaokeViewKioskActive(context: Context): Boolean {
        return detect(context).isKioskModeActive
    }

    private fun isGaokeViewDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            GAOKE_PACKAGES.any { pkg ->
                try {
                    dpm.isDeviceOwnerApp(pkg)
                } catch (_: Exception) { false }
            }
        } catch (_: Exception) { false }
    }

    private fun isLockTaskActive(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                am.isInLockTaskMode
            }
        } catch (_: Exception) { false }
    }

    private fun isGaokeViewDefaultLauncher(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            val pkg = resolveInfo?.activityInfo?.packageName ?: return false
            GAOKE_PACKAGES.contains(pkg)
        } catch (_: Exception) { false }
    }

    private fun getInstalledGaokePackages(context: Context): List<String> {
        return GAOKE_PACKAGES.filter { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) { false }
        }
    }
}