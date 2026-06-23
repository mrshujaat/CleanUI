package com.example.mediabrowser.domain.model

/**
 * App-wide user preferences, persisted via DataStore.
 */
data class AppSettings(
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val safeSearchEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val gridColumns: Int = 3,
    val cacheSizeLimitMb: Int = 250,
    val downloadOverWifiOnly: Boolean = false,

    // --- Layout customization ---
    val homeLayoutStyle: LayoutStyle = LayoutStyle.MASONRY,
    val favoritesLayoutStyle: LayoutStyle = LayoutStyle.MASONRY,
    val cardCornerRadiusDp: Int = 16,

    // --- Theme customization ---
    val accentColorHex: String = "#2DD4BF",
    val backgroundColorHex: String = "#050607",
    val surfaceColorHex: String = "#121315",
    val tagArtistColorHex: String = "#FF8A65",
    val tagCharacterColorHex: String = "#81C784",
    val tagCopyrightColorHex: String = "#BA68C8",
    val tagMetaColorHex: String = "#90A4AE",
    val tagGeneralColorHex: String = "#64B5F6",

    // --- Generic API configuration (provider-agnostic) ---
    val apiProviderName: String = "",
    val apiBaseUrl: String = "",
    val apiCredentialOne: String = "",   // e.g. API Key / Client ID, depending on provider
    val apiCredentialTwo: String = ""    // e.g. Client Secret, optional, depending on provider
)

enum class DarkModeOption {
    SYSTEM,
    LIGHT,
    DARK
}

enum class LayoutStyle {
    GRID,
    MASONRY
}
