package com.example.mediabrowser.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.DarkModeOption
import com.example.mediabrowser.domain.model.LayoutStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Wraps Jetpack DataStore Preferences to expose [AppSettings] as a Flow and
 * provide typed setters. Keeps every screen from having to know about raw
 * Preferences keys.
 */
@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val SAFE_SEARCH = booleanPreferencesKey("safe_search_enabled")
        val DATA_SAVER = booleanPreferencesKey("data_saver_enabled")
        val AUTOPLAY_VIDEOS = booleanPreferencesKey("autoplay_videos")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val CACHE_LIMIT_MB = intPreferencesKey("cache_size_limit_mb")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("download_over_wifi_only")

        val HOME_LAYOUT_STYLE = stringPreferencesKey("home_layout_style")
        val FAVORITES_LAYOUT_STYLE = stringPreferencesKey("favorites_layout_style")
        val HOME_FEED_TYPE = stringPreferencesKey("home_feed_type")
        val CARD_CORNER_RADIUS = intPreferencesKey("card_corner_radius_dp")

        val ACCENT_COLOR = stringPreferencesKey("accent_color_hex")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color_hex")
        val SURFACE_COLOR = stringPreferencesKey("surface_color_hex")
        val TAG_ARTIST_COLOR = stringPreferencesKey("tag_artist_color_hex")
        val TAG_CHARACTER_COLOR = stringPreferencesKey("tag_character_color_hex")
        val TAG_COPYRIGHT_COLOR = stringPreferencesKey("tag_copyright_color_hex")
        val TAG_META_COLOR = stringPreferencesKey("tag_meta_color_hex")
        val TAG_GENERAL_COLOR = stringPreferencesKey("tag_general_color_hex")

        val API_PROVIDER_NAME = stringPreferencesKey("api_provider_name")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_CREDENTIAL_ONE = stringPreferencesKey("api_credential_one")
        val API_CREDENTIAL_TWO = stringPreferencesKey("api_credential_two")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkMode = DarkModeOption.entries.find { it.name == prefs[Keys.DARK_MODE] }
                ?: DarkModeOption.SYSTEM,
            safeSearchEnabled = prefs[Keys.SAFE_SEARCH] ?: true,
            dataSaverEnabled = prefs[Keys.DATA_SAVER] ?: false,
            autoPlayVideos = prefs[Keys.AUTOPLAY_VIDEOS] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            gridColumns = prefs[Keys.GRID_COLUMNS] ?: 3,
            cacheSizeLimitMb = prefs[Keys.CACHE_LIMIT_MB] ?: 250,
            downloadOverWifiOnly = prefs[Keys.WIFI_ONLY_DOWNLOADS] ?: false,

            homeLayoutStyle = LayoutStyle.entries.find { it.name == prefs[Keys.HOME_LAYOUT_STYLE] }
                ?: LayoutStyle.MASONRY,
            favoritesLayoutStyle = LayoutStyle.entries.find { it.name == prefs[Keys.FAVORITES_LAYOUT_STYLE] }
                ?: LayoutStyle.MASONRY,
            homeFeedType = com.example.mediabrowser.domain.model.FeedType.entries
                .find { it.name == prefs[Keys.HOME_FEED_TYPE] }
                ?: com.example.mediabrowser.domain.model.FeedType.DEFAULT,
            cardCornerRadiusDp = prefs[Keys.CARD_CORNER_RADIUS] ?: 16,

            imageQuality = com.example.mediabrowser.domain.model.ImageQuality.entries
                .find { it.name == prefs[Keys.IMAGE_QUALITY] }
                ?: com.example.mediabrowser.domain.model.ImageQuality.MEDIUM,

            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#2DD4BF",
            backgroundColorHex = prefs[Keys.BACKGROUND_COLOR] ?: "#050607",
            surfaceColorHex = prefs[Keys.SURFACE_COLOR] ?: "#121315",
            tagArtistColorHex = prefs[Keys.TAG_ARTIST_COLOR] ?: "#FF8A65",
            tagCharacterColorHex = prefs[Keys.TAG_CHARACTER_COLOR] ?: "#81C784",
            tagCopyrightColorHex = prefs[Keys.TAG_COPYRIGHT_COLOR] ?: "#BA68C8",
            tagMetaColorHex = prefs[Keys.TAG_META_COLOR] ?: "#90A4AE",
            tagGeneralColorHex = prefs[Keys.TAG_GENERAL_COLOR] ?: "#64B5F6",

            apiProviderName = prefs[Keys.API_PROVIDER_NAME] ?: "",
            apiBaseUrl = prefs[Keys.API_BASE_URL] ?: "",
            apiCredentialOne = prefs[Keys.API_CREDENTIAL_ONE] ?: "",
            apiCredentialTwo = prefs[Keys.API_CREDENTIAL_TWO] ?: ""
        )
    }

    suspend fun setDarkMode(option: DarkModeOption) {
        context.dataStore.edit { it[Keys.DARK_MODE] = option.name }
    }

    suspend fun setSafeSearch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAFE_SEARCH] = enabled }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun setAutoPlayVideos(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_VIDEOS] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns }
    }

    suspend fun setCacheSizeLimitMb(mb: Int) {
        context.dataStore.edit { it[Keys.CACHE_LIMIT_MB] = mb }
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setHomeLayoutStyle(style: LayoutStyle) {
        context.dataStore.edit { it[Keys.HOME_LAYOUT_STYLE] = style.name }
    }

    suspend fun setHomeFeedType(type: com.example.mediabrowser.domain.model.FeedType) {
        context.dataStore.edit { it[Keys.HOME_FEED_TYPE] = type.name }
    }

    suspend fun setImageQuality(quality: com.example.mediabrowser.domain.model.ImageQuality) {
        context.dataStore.edit { it[Keys.IMAGE_QUALITY] = quality.name }
    }

    suspend fun setFavoritesLayoutStyle(style: LayoutStyle) {
        context.dataStore.edit { it[Keys.FAVORITES_LAYOUT_STYLE] = style.name }
    }

    suspend fun setCardCornerRadius(dp: Int) {
        context.dataStore.edit { it[Keys.CARD_CORNER_RADIUS] = dp }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = hex }
    }

    suspend fun setBackgroundColor(hex: String) {
        context.dataStore.edit { it[Keys.BACKGROUND_COLOR] = hex }
    }

    suspend fun setSurfaceColor(hex: String) {
        context.dataStore.edit { it[Keys.SURFACE_COLOR] = hex }
    }

    suspend fun setTagArtistColor(hex: String) {
        context.dataStore.edit { it[Keys.TAG_ARTIST_COLOR] = hex }
    }

    suspend fun setTagCharacterColor(hex: String) {
        context.dataStore.edit { it[Keys.TAG_CHARACTER_COLOR] = hex }
    }

    suspend fun setTagCopyrightColor(hex: String) {
        context.dataStore.edit { it[Keys.TAG_COPYRIGHT_COLOR] = hex }
    }

    suspend fun setTagMetaColor(hex: String) {
        context.dataStore.edit { it[Keys.TAG_META_COLOR] = hex }
    }

    suspend fun setTagGeneralColor(hex: String) {
        context.dataStore.edit { it[Keys.TAG_GENERAL_COLOR] = hex }
    }

    suspend fun setApiProviderName(name: String) {
        context.dataStore.edit { it[Keys.API_PROVIDER_NAME] = name }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.API_BASE_URL] = url }
    }

    suspend fun setApiCredentialOne(value: String) {
        context.dataStore.edit { it[Keys.API_CREDENTIAL_ONE] = value }
    }

    suspend fun setApiCredentialTwo(value: String) {
        context.dataStore.edit { it[Keys.API_CREDENTIAL_TWO] = value }
    }
}