package com.example.mediabrowser.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.DarkModeOption

/**
 * Holds the resolved tag-category colors for the current session, derived
 * from [AppSettings]. Read via [LocalTagColors] anywhere a tag chip needs
 * to pick its color, so customizing colors in Settings updates every
 * screen immediately without restarting the app.
 */
data class TagColorPalette(
    val artist: androidx.compose.ui.graphics.Color,
    val character: androidx.compose.ui.graphics.Color,
    val copyright: androidx.compose.ui.graphics.Color,
    val meta: androidx.compose.ui.graphics.Color,
    val general: androidx.compose.ui.graphics.Color
)

val LocalTagColors = staticCompositionLocalOf {
    TagColorPalette(
        artist = TagArtistDefault,
        character = TagCharacterDefault,
        copyright = TagCopyrightDefault,
        meta = TagMetaDefault,
        general = TagGeneralDefault
    )
}

/**
 * App theme wrapper. Resolves dark/light state, accent/background colors,
 * and tag-category colors all from [settings], so any change made in the
 * Settings screen applies live across the whole app on next recomposition
 * (no restart needed).
 */
@Composable
fun MediaBrowserTheme(
    settings: AppSettings,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Light mode is not implemented yet — the app's custom colors all assume a
    // dark background. Force dark regardless of the system setting or the saved
    // preference, otherwise a phone in system-light renders white-on-white.
    val useDarkTheme = true

    val accent = parseHexColor(settings.accentColorHex, AccentPrimaryDark)
    val background = parseHexColor(settings.backgroundColorHex, BackgroundDark)
    val surface = parseHexColor(settings.surfaceColorHex, SurfaceDark)

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> darkColorScheme(
            primary = accent,
            secondary = AccentSecondary,
            background = background,
            surface = surface,
            surfaceVariant = SurfaceVariantDark,
            onBackground = OnSurfaceDark,
            onSurface = OnSurfaceDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            error = ErrorColor
        )
        else -> lightColorScheme(
            primary = accent,
            secondary = AccentSecondary,
            background = BackgroundLight,
            surface = SurfaceLight,
            surfaceVariant = SurfaceVariantLight,
            onBackground = OnSurfaceLight,
            onSurface = OnSurfaceLight,
            onSurfaceVariant = OnSurfaceVariantLight,
            error = ErrorColor
        )
    }

    val tagColors = TagColorPalette(
        artist = parseHexColor(settings.tagArtistColorHex, TagArtistDefault),
        character = parseHexColor(settings.tagCharacterColorHex, TagCharacterDefault),
        copyright = parseHexColor(settings.tagCopyrightColorHex, TagCopyrightDefault),
        meta = parseHexColor(settings.tagMetaColorHex, TagMetaDefault),
        general = parseHexColor(settings.tagGeneralColorHex, TagGeneralDefault)
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalTagColors provides tagColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography
        ) {
            // Provided INSIDE MaterialTheme so it overrides MaterialTheme's own
            // default (bodyLarge) text style. This makes Manjari the baseline for
            // every Text — including inline-styled ones that only set size/weight/
            // color — since they inherit this and override just what they specify.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides
                    androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = Manjari)
            ) {
                content()
            }
        }
    }
}