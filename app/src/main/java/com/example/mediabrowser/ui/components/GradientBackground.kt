package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Flat app background. The accent-tinted top glow was removed per the design —
 * the app is now a consistent solid color (pure black in the default dark theme).
 * The accentColor parameter is kept so existing call sites don't need to change.
 */
fun Modifier.appBackgroundGradient(
    @Suppress("UNUSED_PARAMETER") accentColor: Color,
    backgroundColor: Color = Color(0xFF000000)
): Modifier = this.background(backgroundColor)