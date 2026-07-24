package com.example.kubmi.presentation.screens.admin.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_hub_title)) },
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
            CategoryCard(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_category_kiosk),
                description = stringResource(R.string.settings_category_kiosk_desc),
                onClick = { navController.navigate(Screen.SettingsKioskMode.route) }
            )
            CategoryCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.settings_category_permissions),
                description = stringResource(R.string.settings_category_permissions_desc),
                onClick = { navController.navigate(Screen.SettingsPermissions.route) }
            )
            CategoryCard(
                icon = Icons.Default.Tv,
                title = stringResource(R.string.settings_category_screensaver),
                description = stringResource(R.string.settings_category_screensaver_desc),
                onClick = { navController.navigate(Screen.SettingsScreensaver.route) }
            )
            CategoryCard(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.settings_category_main_screen_text),
                description = stringResource(R.string.settings_category_main_screen_text_desc),
                onClick = { navController.navigate(Screen.SettingsBellSchedule.route) }
            )
            CategoryCard(
                icon = Icons.Default.Sync,
                title = stringResource(R.string.settings_category_data),
                description = stringResource(R.string.settings_category_data_desc),
                onClick = { navController.navigate(Screen.SettingsData.route) }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}