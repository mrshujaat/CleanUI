package com.example.mediabrowser.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.DarkModeOption
import com.example.mediabrowser.domain.model.LayoutStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val repository: MediaRepository,
    private val backupManager: com.example.mediabrowser.data.backup.BackupManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch { _cacheSizeBytes.value = repository.getCacheSizeBytes() }
    }

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { preferencesDataStore.setDarkMode(option) }
    }

    fun setSafeSearch(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setSafeSearch(enabled) }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setDataSaver(enabled) }
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setAutoPlayVideos(enabled) }
    }

    fun setHomeFeedType(type: com.example.mediabrowser.domain.model.FeedType) {
        viewModelScope.launch { preferencesDataStore.setHomeFeedType(type) }
    }

    // --- Backup / Restore ---

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun clearBackupStatus() { _backupStatus.value = null }

    /** Produce the backup JSON, then hand it to [onReady] to write to the chosen file. */
    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonText = backupManager.exportToJson()
                onReady(jsonText)
                _backupStatus.value = "Backup exported successfully"
            } catch (e: Exception) {
                _backupStatus.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importBackup(text: String) {
        viewModelScope.launch {
            try {
                val summary = backupManager.importFromJson(text)
                _backupStatus.value =
                    "Restored ${summary.favorites} favourites, ${summary.batches} batches, ${summary.tags} tags"
            } catch (e: Exception) {
                _backupStatus.value = "Restore failed: ${e.message}"
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotificationsEnabled(enabled) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { preferencesDataStore.setGridColumns(columns) }
    }

    fun setImageQuality(quality: com.example.mediabrowser.domain.model.ImageQuality) {
        viewModelScope.launch { preferencesDataStore.setImageQuality(quality) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setDownloadOverWifiOnly(enabled) }
    }

    fun setHomeLayoutStyle(style: LayoutStyle) {
        viewModelScope.launch { preferencesDataStore.setHomeLayoutStyle(style) }
    }

    fun setFavoritesLayoutStyle(style: LayoutStyle) {
        viewModelScope.launch { preferencesDataStore.setFavoritesLayoutStyle(style) }
    }

    fun setCardCornerRadius(dp: Int) {
        viewModelScope.launch { preferencesDataStore.setCardCornerRadius(dp) }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setAccentColor(hex) }
    }

    fun setBackgroundColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setBackgroundColor(hex) }
    }

    fun setSurfaceColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setSurfaceColor(hex) }
    }

    fun setTagArtistColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setTagArtistColor(hex) }
    }

    fun setTagCharacterColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setTagCharacterColor(hex) }
    }

    fun setTagCopyrightColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setTagCopyrightColor(hex) }
    }

    fun setTagMetaColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setTagMetaColor(hex) }
    }

    fun setTagGeneralColor(hex: String) {
        viewModelScope.launch { preferencesDataStore.setTagGeneralColor(hex) }
    }

    fun setApiProviderName(name: String) {
        viewModelScope.launch { preferencesDataStore.setApiProviderName(name) }
    }

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch { preferencesDataStore.setApiBaseUrl(url) }
    }

    fun setApiCredentialOne(value: String) {
        // Trim whitespace/newlines — pasted API keys often carry a trailing newline,
        // which the server rejects as "Missing authentication".
        viewModelScope.launch { preferencesDataStore.setApiCredentialOne(value.trim()) }
    }

    fun setApiCredentialTwo(value: String) {
        viewModelScope.launch { preferencesDataStore.setApiCredentialTwo(value.trim()) }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearImageCache()
            refreshCacheSize()
        }
    }

    /** Theme presets applied in one tap from the Themes section. */
    enum class ThemePreset { DARK, LIGHT }

    fun applyThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            when (preset) {
                ThemePreset.DARK -> {
                    preferencesDataStore.setDarkMode(DarkModeOption.DARK)
                    preferencesDataStore.setBackgroundColor("#000000")
                    preferencesDataStore.setSurfaceColor("#0E0F11")
                    preferencesDataStore.setAccentColor("#2DD4BF")
                }
                ThemePreset.LIGHT -> {
                    preferencesDataStore.setDarkMode(DarkModeOption.LIGHT)
                    preferencesDataStore.setBackgroundColor("#FAFAFC")
                    preferencesDataStore.setSurfaceColor("#FFFFFF")
                    preferencesDataStore.setAccentColor("#2DD4BF")
                }
            }
        }
    }
}