package com.example.kubmi.ui.theme

import androidx.compose.ui.graphics.Color

// Website-inspired palette (panel page: white surfaces, blue link color + blue/red CTA buttons, dark text)
// Panel page link color (computed): rgb(0, 65, 150) -> #004196
val KubMiPrimaryBlue = Color(0xFF004196)          // primary + link blue

// CTA button blue (looks brighter than link blue on the panel page)
val KubMiCtaBlue = Color(0xFF1976D2)              // CTA blue (approx)
val KubMiCtaBlueDark = Color(0xFF0D47A1)          // CTA gradient end

val KubMiAccentRed = Color(0xFFE95466)            // CTA red/pink (approx)
val KubMiAccentRedDark = Color(0xFFD84353)        // gradient end

val KubMiTextPrimary = Color(0xFF111827)          // near-black
val KubMiTextSecondary = Color(0xFF4B5563)        // gray

val KubMiBackground = Color(0xFFFFFFFF)           // panel page background is white (patterned)
val KubMiSurface = Color(0xFFFFFFFF)              // cards
val KubMiSurfaceVariant = Color(0xFFF6F7F9)       // subtle sections
val KubMiOutline = Color(0xFFE5E7EB)              // borders

// Back-compat names referenced across codebase
val KubMiDarkBlue = KubMiCtaBlueDark
val KubMiBlue = KubMiPrimaryBlue
val KubMiAccent = KubMiPrimaryBlue
val KubMiLight = KubMiAccentRed

val BackgroundLight = KubMiBackground
val HeaderDark = KubMiTextPrimary
val TableRowEven = KubMiSurface
val TableRowOdd = KubMiSurfaceVariant
val TableBorder = KubMiOutline
val LinkBlue = KubMiPrimaryBlue
val ButtonGradientStart = KubMiCtaBlue
val ButtonGradientEnd = KubMiCtaBlueDark