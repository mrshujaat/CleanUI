package com.example.mediabrowser.ui.theme

import androidx.compose.ui.graphics.Color

// Default palette — used as fallback values and as the seed for the
// Settings color pickers. Actual runtime colors come from AppSettings
// (user-customizable), resolved in Theme.kt / DynamicColors.kt.
val AccentPrimary = Color(0xFF2DD4BF)
val AccentPrimaryDark = Color(0xFF5EEAD4)
val AccentSecondary = Color(0xFF14B8A6)

// Pure black to match the redesign (PDF uses #000000 backgrounds).
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF0E0F11)
val SurfaceVariantDark = Color(0xFF8A8D91)
val OnSurfaceDark = Color(0xFFFFFFFF)
val OnSurfaceVariantDark = Color(0xFF8A8D91)

val BackgroundLight = Color(0xFFFAFAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEFEFF4)
val OnSurfaceLight = Color(0xFF1B1B1F)
val OnSurfaceVariantLight = Color(0xFF55555F)

val ErrorColor = Color(0xFFFF5C5C)
val SuccessColor = Color(0xFF2ECC71)

// --- Tag-category colors, matched EXACTLY to the redesign PDF ---
// Sampled directly from the artboard pills. These are the defaults and the
// seeds for the Settings color pickers.
val TagArtistDefault = Color(0xFFFDEA02)     // yellow
val TagCharacterDefault = Color(0xFF2C87C8)  // blue
val TagSeriesDefault = Color(0xFF9E1E62)     // magenta (series)
val TagCopyrightDefault = Color(0xFFBE1E2D)  // red (copyright)
val TagMetaDefault = Color(0xFFB35426)       // orange/brown
val TagGeneralDefault = Color(0xFF344154)    // slate

// Orange used for the primary (r34.app-style) search suggestions.
val SuggestionPrimaryOrange = Color(0xFFF16521)

// Purple used for selected/committed tag pills in the search bar.
val SelectedTagPurple = Color(0xFF6A2C90)