package com.example.mediabrowser.domain.model

/**
 * App-wide user preferences, persisted via DataStore.
 */
data class AppSettings(
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val safeSearchEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val gridColumns: Int = 3,
    val imageQuality: ImageQuality = ImageQuality.MEDIUM,
    val cacheSizeLimitMb: Int = 250,
    val downloadOverWifiOnly: Boolean = false,

    // --- Layout customization ---
    val homeLayoutStyle: LayoutStyle = LayoutStyle.MASONRY,
    val favoritesLayoutStyle: LayoutStyle = LayoutStyle.MASONRY,
    val cardCornerRadiusDp: Int = 16,

    // --- Home feed type (Default trending vs personalized Poison Feed) ---
    val homeFeedType: FeedType = FeedType.DEFAULT,

    // --- Theme customization ---
    val accentColorHex: String = "#2DD4BF",
    val backgroundColorHex: String = "#000000",
    val surfaceColorHex: String = "#0E0F11",
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

/**
 * Which feed shows under the Popular/Trending/Series blocks on Home:
 *  - DEFAULT: the standard trending feed
 *  - POISON: the personalized, affinity-ranked recommendation feed
 */
enum class FeedType {
    DEFAULT,
    POISON
}

/**
 * Controls which image URL the grid loads, trading quality for speed:
 *  - LOW: tiny preview (fastest, lowest data)
 *  - MEDIUM: the resampled "sample" image (good-looking, fast — the sweet spot)
 *  - HIGH: the full original file (sharpest, slowest, most data)
 */
enum class ImageQuality {
    LOW,
    MEDIUM,
    HIGH
}