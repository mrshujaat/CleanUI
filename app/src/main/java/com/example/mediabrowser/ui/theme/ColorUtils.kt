package com.example.mediabrowser.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a "#RRGGBB" hex string into a Compose [Color], falling back to
 * [fallback] if the string is malformed. Used to turn user-selected theme
 * colors (stored as plain hex strings in AppSettings/DataStore) into
 * actual Color values at render time.
 */
fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        if (cleaned.length != 6) return fallback
        val colorLong = cleaned.toLong(16)
        Color(
            red = ((colorLong shr 16) and 0xFF) / 255f,
            green = ((colorLong shr 8) and 0xFF) / 255f,
            blue = (colorLong and 0xFF) / 255f,
            alpha = 1f
        )
    } catch (e: Exception) {
        fallback
    }
}

/** Converts a Compose [Color] back into a "#RRGGBB" hex string for storage. */
fun Color.toHexString(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
