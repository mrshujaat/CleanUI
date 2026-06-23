package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Dark background with a subtle glow radiating from the top, tinted by the
 * current accent color so the whole app's mood shifts with the user's
 * chosen theme color.
 */
fun Modifier.appBackgroundGradient(accentColor: Color): Modifier = this.background(
    brush = Brush.radialGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.18f),
            Color(0xFF0A1715),
            Color(0xFF050607)
        ),
        center = Offset(0.5f, 0f),
        radius = 1200f
    )
)
