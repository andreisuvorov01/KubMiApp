package com.example.kubmi.presentation.screens.admin.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kubmi.R
import com.example.kubmi.util.KioskPermissionManager.ProtectionLevel

@Composable
fun ProtectionLevelCard(
    level: ProtectionLevel,
    modifier: Modifier = Modifier
) {
    val (color, levelText) = when (level) {
        ProtectionLevel.NONE -> Color.Red to stringResource(R.string.protection_level_none)
        ProtectionLevel.BASIC -> Color(0xFFFF9800) to stringResource(R.string.protection_level_basic)
        ProtectionLevel.MEDIUM -> Color(0xFFFFEB3B) to stringResource(R.string.protection_level_medium)
        ProtectionLevel.HIGH -> Color(0xFF8BC34A) to stringResource(R.string.protection_level_high)
        ProtectionLevel.MAXIMUM -> Color(0xFF4CAF50) to stringResource(R.string.protection_level_maximum)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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