package com.example.kubmi.presentation.screens.admin

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.kiosk.AdvancedKioskManager
import com.example.kubmi.util.KioskPermissionManager
import com.example.kubmi.util.KioskPermissionManager.ProtectionLevel
import com.example.kubmi.util.SecurePreferences
import com.example.kubmi.util.ScreensaverDelays

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskSettingsScreen(
    navController: NavController,
    permissionManager: KioskPermissionManager,
    kioskManager: AdvancedKioskManager,
    securePreferences: SecurePreferences
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(permissionManager.getPermissionStatus()) }
    var kioskCheckResult by remember { mutableStateOf<AdvancedKioskManager.KioskCheckResult?>(null) }
    var showCheckResultDialog by remember { mutableStateOf(false) }
    var screensaverDelay by remember { mutableStateOf(securePreferences.getScreensaverDelay()) }
    var showDelayDropdown by remember { mutableStateOf(false) }
    
    // Refresh permission status when screen is resumed
    LaunchedEffect(Unit) {
        permissionStatus = permissionManager.getPermissionStatus()
        kioskCheckResult = kioskManager.checkKioskFunctionality()
    }
    
    // Refresh status periodically while on this screen
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            permissionStatus = permissionManager.getPermissionStatus()
            kioskCheckResult = kioskManager.checkKioskFunctionality()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kiosk_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
            
            // Kiosk Status Check Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Kiosk Functionality Check",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                kioskCheckResult = kioskManager.checkKioskFunctionality()
                                showCheckResultDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Run Diagnostics")
                        }
                        
                        kioskCheckResult?.let { result ->
                            if (result.isFullyProtected) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Protected",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    kioskCheckResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Protection: ${result.protectionPercentage}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // Screensaver Delay Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Screensaver Delay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time before screensaver activates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box {
                        OutlinedButton(
                            onClick = { showDelayDropdown = !showDelayDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ScreensaverDelays.DELAY_OPTIONS
                                    .find { it.first == screensaverDelay }
                                    ?.second ?: "5 minutes",
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDelayDropdown,
                            onDismissRequest = { showDelayDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScreensaverDelays.DELAY_OPTIONS.forEach { (delaySeconds, delayText) ->
                                DropdownMenuItem(
                                    text = { Text(delayText) },
                                    onClick = {
                                        screensaverDelay = delaySeconds
                                        securePreferences.saveScreensaverDelay(delaySeconds)
                                        showDelayDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
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
                    (context as? Activity)?.let {
                        kioskManager.beginAdminMaintenance(it, 10_000L)
                    }
                    permissionManager.openLauncherSettings()
                }
            )
            
            // Accessibility Service
            PermissionItem(
                title = stringResource(R.string.kiosk_accessibility_title),
                description = stringResource(R.string.kiosk_accessibility_description),
                isEnabled = permissionStatus.isAccessibilityEnabled,
                onEnableClick = {
                    (context as? Activity)?.let {
                        kioskManager.beginAdminMaintenance(it, 10_000L)
                    }
                    permissionManager.openAccessibilitySettings()
                }
            )
            
            // Usage Stats Permission
            PermissionItem(
                title = stringResource(R.string.kiosk_usage_stats_title),
                description = stringResource(R.string.kiosk_usage_stats_description),
                isEnabled = permissionStatus.isUsageStatsEnabled,
                onEnableClick = {
                    (context as? Activity)?.let {
                        kioskManager.beginAdminMaintenance(it, 10_000L)
                    }
                    permissionManager.openUsageStatsSettings()
                }
            )
            
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
                onClick = { 
                    permissionStatus = permissionManager.getPermissionStatus()
                    kioskCheckResult = kioskManager.checkKioskFunctionality()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
    
    // Kiosk Check Result Dialog
    if (showCheckResultDialog && kioskCheckResult != null) {
        KioskCheckResultDialog(
            result = kioskCheckResult!!,
            onDismiss = { showCheckResultDialog = false }
        )
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

@Composable
private fun KioskCheckResultDialog(
    result: AdvancedKioskManager.KioskCheckResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (result.isFullyProtected) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                Text("Kiosk Status Report")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Protection score
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Protection Level",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${result.protectionPercentage}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { result.protectionPercentage.toFloat() / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                
                // Key features status
                Text(
                    text = "Component Status",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                
                StatusItem(
                    title = "Device Owner",
                    isEnabled = result.isDeviceOwner,
                    icon = if (result.isDeviceOwner) Icons.Default.Check else Icons.Default.Close
                )
                
                StatusItem(
                    title = "Default Launcher",
                    isEnabled = result.isDefaultLauncher,
                    icon = if (result.isDefaultLauncher) Icons.Default.Check else Icons.Default.Close
                )
                
                StatusItem(
                    title = "Lock Task Mode",
                    isEnabled = result.isLockTaskActive,
                    icon = if (result.isLockTaskActive) Icons.Default.Check else Icons.Default.Close
                )
                
                StatusItem(
                    title = "Accessibility Service",
                    isEnabled = result.isAccessibilityEnabled,
                    icon = if (result.isAccessibilityEnabled) Icons.Default.Check else Icons.Default.Close
                )
                
                StatusItem(
                    title = "Foreground Service",
                    isEnabled = result.isRunningInForeground,
                    icon = if (result.isRunningInForeground) Icons.Default.Check else Icons.Default.Close
                )
                
                // Errors section
                if (result.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Errors",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    result.errors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
                
                // Warnings section
                if (result.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Warnings",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    result.warnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                
                // Device info
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device Info",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Android ${result.androidVersion}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StatusItem(
    title: String,
    isEnabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isEnabled) "OK" else "MISSING",
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800),
            fontWeight = FontWeight.Bold
        )
    }
}
