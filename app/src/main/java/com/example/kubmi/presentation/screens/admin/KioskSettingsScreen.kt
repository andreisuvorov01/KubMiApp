package com.example.kubmi.presentation.screens.admin

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import android.app.UiModeManager
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.service.KioskService
import com.example.kubmi.util.KioskPermissionManager
import com.example.kubmi.util.KioskPermissionManager.ProtectionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskSettingsScreen(
    navController: NavController,
    permissionManager: KioskPermissionManager
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(permissionManager.getPermissionStatus()) }
    var isLockTaskActive by remember { mutableStateOf(false) }

    // Check if this is a TV device
    val isTvDevice by remember {
        mutableStateOf(
            try {
                val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            } catch (e: Exception) {
                false
            }
        )
    }

    // Refresh permission status when screen is resumed
    LaunchedEffect(Unit) {
        permissionStatus = permissionManager.getPermissionStatus()

        // Only check lock task status on non-TV devices
        if (!isTvDevice) {
            isLockTaskActive = com.example.kubmi.util.KioskManager.isLockTaskActive(context as Activity)
        }
    }

    // Refresh status periodically while on this screen
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200)
            permissionStatus = permissionManager.getPermissionStatus()

            // Only check lock task status on non-TV devices
            if (!isTvDevice) {
                isLockTaskActive = com.example.kubmi.util.KioskManager.isLockTaskActive(context as Activity)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kiosk_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Protection Level Card
            ProtectionLevelCard(permissionStatus.overallProtectionLevel)
            
            // Permissions Section
            Text(
                text = stringResource(R.string.kiosk_permissions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Default Launcher
            PermissionItem(
                title = stringResource(R.string.kiosk_launcher_title),
                description = stringResource(R.string.kiosk_launcher_description),
                isEnabled = permissionStatus.isDefaultLauncher,
                onEnableClick = {
                    // Only allow SHORT exit for settings (10 seconds)
                    allowShortExit(context)
                    permissionManager.openLauncherSettings()
                }
            )
            
            // Accessibility Service
            PermissionItem(
                title = stringResource(R.string.kiosk_accessibility_title),
                description = stringResource(R.string.kiosk_accessibility_description),
                isEnabled = permissionStatus.isAccessibilityEnabled,
                onEnableClick = {
                    allowShortExit(context)
                    permissionManager.openAccessibilitySettings()
                }
            )
            
            // Device Owner status
            PermissionItem(
                title = stringResource(R.string.kiosk_device_owner_title),
                description = stringResource(R.string.kiosk_device_owner_description),
                isEnabled = permissionStatus.isDeviceOwner,
                onEnableClick = {
                    // Cannot enable device owner from app, show info toast
                    android.widget.Toast.makeText(context, "Включается через ADB: adb shell dpm set-device-owner com.example.kubmi/.receiver.DeviceAdminReceiver", android.widget.Toast.LENGTH_LONG).show()
                }
            )
            
            // Usage Stats Permission
            PermissionItem(
                title = stringResource(R.string.kiosk_usage_stats_title),
                description = stringResource(R.string.kiosk_usage_stats_description),
                isEnabled = permissionStatus.isUsageStatsEnabled,
                onEnableClick = {
                    allowShortExit(context)
                    permissionManager.openUsageStatsSettings()
                }
            )

            // Screen Pinning Status - Only show on non-TV devices
            if (!isTvDevice) {
                PermissionItem(
                    title = stringResource(R.string.kiosk_screen_pinning_title),
                    description = stringResource(R.string.kiosk_screen_pinning_description),
                    isEnabled = isLockTaskActive,
                    onEnableClick = {
                        // Enable screen pinning without exit window
                        com.example.kubmi.util.KioskManager.enableKioskMode(context as Activity)
                        com.example.kubmi.util.KioskManager.startLockTask(context as Activity)
                        com.example.kubmi.service.KioskService.start(context)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.kiosk_screen_pinning_enabled),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        // Update status immediately
                        isLockTaskActive = true
                    },
                    onDisableClick = {
                        // Allow temporary exit when disabling screen pinning
                        allowShortExit(context)
                        com.example.kubmi.util.KioskManager.disableKioskMode(context as Activity)
                        com.example.kubmi.util.KioskManager.stopLockTask(context as Activity)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.kiosk_screen_pinning_disabled),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        // Update status immediately
                        isLockTaskActive = false
                    }
                )
            } else {
                // Show info card for TV devices explaining why Screen Pinning is not available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.kiosk_tv_mode_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.kiosk_tv_mode_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.kiosk_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.kiosk_info_description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Refresh button
            OutlinedButton(
                onClick = { permissionStatus = permissionManager.getPermissionStatus() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
}

@Composable
private fun ProtectionLevelCard(level: ProtectionLevel) {
    val (color, levelText) = when (level) {
        ProtectionLevel.NONE -> Color.Red to stringResource(R.string.protection_level_none)
        ProtectionLevel.BASIC -> Color(0xFFFF9800) to stringResource(R.string.protection_level_basic)
        ProtectionLevel.MEDIUM -> Color(0xFFFFEB3B) to stringResource(R.string.protection_level_medium)
        ProtectionLevel.HIGH -> Color(0xFF8BC34A) to stringResource(R.string.protection_level_high)
        ProtectionLevel.MAXIMUM -> Color(0xFF4CAF50) to stringResource(R.string.protection_level_maximum)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.protection_level_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                val protectionPercent = when (level) {
                    ProtectionLevel.NONE -> "0"
                    ProtectionLevel.BASIC -> "25"
                    ProtectionLevel.MEDIUM -> "50"
                    ProtectionLevel.HIGH -> "75"
                    ProtectionLevel.MAXIMUM -> "100"
                }
                Text(
                    text = protectionPercent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onEnableClick: () -> Unit,
    onDisableClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isEnabled) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action button
            if (isEnabled && onDisableClick != null) {
                TextButton(onClick = onDisableClick) {
                    Text(stringResource(R.string.disable))
                }
            } else if (!isEnabled) {
                Button(
                    onClick = onEnableClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.enable))
                }
            }
        }
    }
}

private fun allowTemporaryExit(context: Context) {
    val prefs = context.getSharedPreferences(KioskService.PREF_KIOSK_GUARD, Context.MODE_PRIVATE)
    prefs.edit()
        .putLong(
            KioskService.KEY_ALLOW_EXIT_UNTIL,
            System.currentTimeMillis() + KioskService.ADMIN_EXIT_WINDOW_MS
        )
        .apply()
}

/**
 * Allow only a short exit window (30 seconds) for opening settings screens
 * This prevents the full 2-minute window from being activated on every button click
 */
private fun allowShortExit(context: Context) {
    val prefs = context.getSharedPreferences(KioskService.PREF_KIOSK_GUARD, Context.MODE_PRIVATE)
    prefs.edit()
        .putLong(
            KioskService.KEY_ALLOW_EXIT_UNTIL,
            System.currentTimeMillis() + 30_000L // 30 seconds
        )
        .apply()
    
    // Disable kiosk mode and stop lock task when exiting to system settings
    (context as? Activity)?.let { activity ->
        com.example.kubmi.util.KioskManager.disableKioskMode(activity)
        com.example.kubmi.util.KioskManager.stopLockTask(activity)
    }
    
    android.widget.Toast.makeText(
        context, 
        context.getString(R.string.kiosk_temporary_exit_allowed), 
        android.widget.Toast.LENGTH_SHORT
    ).show()
}
