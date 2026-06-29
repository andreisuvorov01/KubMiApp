package com.example.kubmi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = KubMiPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = KubMiTextPrimary,

    secondary = KubMiAccentRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = KubMiTextPrimary,

    background = KubMiBackground,
    onBackground = KubMiTextPrimary,

    surface = KubMiSurface,
    onSurface = KubMiTextPrimary,
    surfaceVariant = KubMiSurfaceVariant,
    onSurfaceVariant = KubMiTextSecondary,

    outline = KubMiOutline,
    outlineVariant = KubMiOutline
)

val DarkColorScheme = darkColorScheme(
    primary = KubMiPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF163257),
    onPrimaryContainer = Color.White,

    secondary = KubMiAccentRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A1F29),
    onSecondaryContainer = Color.White,

    background = Color(0xFF0E1116),
    onBackground = Color(0xFFF3F4F6),

    surface = Color(0xFF151A22),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1E2531),
    onSurfaceVariant = Color(0xFFC9D1DD),

    outline = Color(0xFF3A4456),
    outlineVariant = Color(0xFF2A3240)
)

@Composable
fun KubMiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}