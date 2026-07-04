package com.example.mediabrowser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mediabrowser.R

/**
 * Manjari font family. Maps the three shipped weights to Compose FontWeights.
 * Manjari only provides Thin / Regular / Bold, so SemiBold and Medium requests
 * fall back to Bold and Regular respectively (Compose picks the nearest weight).
 *
 * If you only added one Manjari file, keep just the Regular line below and delete
 * the Thin/Bold lines — the family will still work, using Regular for everything.
 */
val Manjari = FontFamily(
    Font(R.font.manjari_thin, FontWeight.Thin),
    Font(R.font.manjari_regular, FontWeight.Normal),
    Font(R.font.manjari_bold, FontWeight.Bold),
)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manjari, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)