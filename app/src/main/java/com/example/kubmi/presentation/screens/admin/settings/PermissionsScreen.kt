package com.example.kubmi.presentation.screens.admin.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.kiosk.AdvancedKioskManager
import com.example.kubmi.presentation.screens.admin.settings.components.PermissionItemCard
import com.example.kubmi.util.KioskPermissionManager

enum class PermissionType(
    val titleRes: Int,
    val descRes: Int,
    val openAction: KioskPermissionManager.() -> Unit
) {
    LAUNCHER(
        R.string.kiosk_launcher_title,
        R.string.kiosk_launcher_description,
        KioskPermissionManager::openLauncherSettings
    ),
    ACCESSIBILITY(
        R.string.kiosk_accessibility_title,
        R.string.kiosk_accessibility_description,
        KioskPermissionManager::openAccessibilitySettings
    ),
    USAGE_STATS(
        R.string.kiosk_usage_stats_title,
        R.string.kiosk_usage_stats_description,
        KioskPermissionManager::openUsageStatsSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val kioskManager = remember(context) {
        AdvancedKioskManager(context.applicationContext)
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_screen_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.kiosk_permissions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val permStatus = uiState.permissionStatus
            PermissionType.entries.forEach { type ->
                val isEnabled = when (type) {
                    PermissionType.LAUNCHER -> permStatus?.isDefaultLauncher ?: false
                    PermissionType.ACCESSIBILITY -> permStatus?.isAccessibilityEnabled ?: false
                    PermissionType.USAGE_STATS -> permStatus?.isUsageStatsEnabled ?: false
                }
                PermissionItemCard(
                    title = stringResource(type.titleRes),
                    description = stringResource(type.descRes),
                    isEnabled = isEnabled,
                    onEnableClick = {
                        (context as? Activity)?.let {
                            kioskManager.beginAdminMaintenance(it, 10_000L)
                        }
                        viewModel.getPermissionManager().let { manager ->
                            type.openAction(manager)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.refreshPermissions() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
}
