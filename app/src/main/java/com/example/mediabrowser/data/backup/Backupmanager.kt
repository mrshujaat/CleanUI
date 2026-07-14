package com.example.mediabrowser.data.backup

import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.local.dao.AffinityDao
import com.example.mediabrowser.data.local.dao.FavoriteArtistDao
import com.example.mediabrowser.data.local.dao.FavoriteDao
import com.example.mediabrowser.data.local.dao.FavoriteTagDao
import com.example.mediabrowser.data.local.dao.TagBatchDao
import com.example.mediabrowser.data.local.entity.ArtistAffinityEntity
import com.example.mediabrowser.data.local.entity.FavoriteArtistEntity
import com.example.mediabrowser.data.local.entity.FavoriteEntity
import com.example.mediabrowser.data.local.entity.FavoriteTagEntity
import com.example.mediabrowser.data.local.entity.TagAffinityEntity
import com.example.mediabrowser.data.local.entity.TagBatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports every piece of user state to a single JSON document and restores it.
 * This is what lets a user reinstall and carry on: favourites, favourite artists
 * & tags, "My Poison" batches, the learned affinity scores, and all app settings.
 *
 * All work runs on Dispatchers.IO. Restore is additive-with-replace (REPLACE
 * conflict strategy on inserts), so importing a backup over an existing install
 * merges cleanly rather than throwing on duplicates.
 */
@Singleton
class BackupManager @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val favoriteArtistDao: FavoriteArtistDao,
    private val favoriteTagDao: FavoriteTagDao,
    private val tagBatchDao: TagBatchDao,
    private val affinityDao: AffinityDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true   // forward-compatible: older backups missing new fields still load
        encodeDefaults = true
    }

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val settings = preferencesDataStore.settingsFlow.first()
        val backup = BackupData(
            version = BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            favorites = favoriteDao.observeAll().first().map { it.toDto() },
            favoriteArtists = favoriteArtistDao.observeAll().first().map { it.toDto() },
            favoriteTags = favoriteTagDao.observeAll().first().map { it.toDto() },
            batches = tagBatchDao.observeAll().first().map { it.toDto() },
            tagAffinities = affinityDao.allTags().map { it.toDto() },
            artistAffinities = affinityDao.allArtists().map { it.toDto() },
            settings = settings.toDto()
        )
        json.encodeToString(backup)
    }

    /**
     * Restore from a previously exported JSON. Returns a short human-readable
     * summary of what was imported, or throws if the document can't be parsed.
     */
    suspend fun importFromJson(text: String): RestoreSummary = withContext(Dispatchers.IO) {
        val backup = json.decodeFromString<BackupData>(text)

        backup.favorites.forEach { favoriteDao.insert(it.toEntity()) }
        backup.favoriteArtists.forEach { favoriteArtistDao.insert(it.toEntity()) }
        backup.favoriteTags.forEach { favoriteTagDao.insert(it.toEntity()) }
        // Batches use autoGenerate ids; insert with id=0 so Room assigns fresh ones
        // and we don't collide with existing local batches.
        backup.batches.forEach { tagBatchDao.insert(it.toEntity().copy(id = 0)) }
        backup.tagAffinities.forEach { affinityDao.upsertTag(it.toEntity()) }
        backup.artistAffinities.forEach { affinityDao.upsertArtist(it.toEntity()) }
        backup.settings?.let { applySettings(it) }

        RestoreSummary(
            favorites = backup.favorites.size,
            artists = backup.favoriteArtists.size,
            tags = backup.favoriteTags.size,
            batches = backup.batches.size,
            affinities = backup.tagAffinities.size + backup.artistAffinities.size
        )
    }

    private suspend fun applySettings(s: SettingsDto) {
        preferencesDataStore.setSafeSearch(s.safeSearchEnabled)
        preferencesDataStore.setAutoPlayVideos(s.autoPlayVideos)
        preferencesDataStore.setNotificationsEnabled(s.notificationsEnabled)
        preferencesDataStore.setGridColumns(s.gridColumns)
        preferencesDataStore.setDownloadOverWifiOnly(s.downloadOverWifiOnly)
        preferencesDataStore.setCardCornerRadius(s.cardCornerRadiusDp)
        preferencesDataStore.setAccentColor(s.accentColorHex)
        runCatching {
            preferencesDataStore.setImageQuality(
                com.example.mediabrowser.domain.model.ImageQuality.valueOf(s.imageQuality)
            )
        }
        runCatching {
            preferencesDataStore.setHomeLayoutStyle(
                com.example.mediabrowser.domain.model.LayoutStyle.valueOf(s.homeLayoutStyle)
            )
        }
        runCatching {
            preferencesDataStore.setFavoritesLayoutStyle(
                com.example.mediabrowser.domain.model.LayoutStyle.valueOf(s.favoritesLayoutStyle)
            )
        }
        runCatching {
            preferencesDataStore.setHomeFeedType(
                com.example.mediabrowser.domain.model.FeedType.valueOf(s.homeFeedType)
            )
        }
        // API credentials are restored too so the user doesn't have to re-enter them.
        preferencesDataStore.setApiCredentialOne(s.apiCredentialOne)
        preferencesDataStore.setApiCredentialTwo(s.apiCredentialTwo)
        preferencesDataStore.setSiteCredentials(s.siteCredentials)
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}

data class RestoreSummary(
    val favorites: Int,
    val artists: Int,
    val tags: Int,
    val batches: Int,
    val affinities: Int
)

// --- Serializable DTOs (decoupled from Room entities so schema tweaks don't
//     silently break old backup files) ---

@Serializable
private data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val favorites: List<FavoriteDto> = emptyList(),
    val favoriteArtists: List<FavoriteArtistDto> = emptyList(),
    val favoriteTags: List<FavoriteTagDto> = emptyList(),
    val batches: List<BatchDto> = emptyList(),
    val tagAffinities: List<TagAffinityDto> = emptyList(),
    val artistAffinities: List<ArtistAffinityDto> = emptyList(),
    val settings: SettingsDto? = null
)

@Serializable
private data class FavoriteDto(
    val postId: Long, val thumbnailUrl: String, val previewUrl: String,
    val fileUrl: String, val fileType: String, val width: Int, val height: Int,
    val score: Int, val tags: String, val savedAt: Long
)

@Serializable
private data class FavoriteArtistDto(
    val artistName: String, val displayName: String, val postCount: Int, val savedAt: Long
)

@Serializable
private data class FavoriteTagDto(
    val tagName: String, val displayName: String, val category: String,
    val postCount: Int, val savedAt: Long
)

@Serializable
private data class BatchDto(
    val id: Long, val name: String, val tags: String, val createdAt: Long, val updatedAt: Long
)

@Serializable
private data class TagAffinityDto(val tag: String, val score: Double, val lastUpdated: Long)

@Serializable
private data class ArtistAffinityDto(val artist: String, val score: Double, val lastUpdated: Long)

@Serializable
private data class SettingsDto(
    val safeSearchEnabled: Boolean = true,
    val autoPlayVideos: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val gridColumns: Int = 3,
    val downloadOverWifiOnly: Boolean = false,
    val cardCornerRadiusDp: Int = 16,
    val accentColorHex: String = "#2DD4BF",
    val imageQuality: String = "MEDIUM",
    val homeLayoutStyle: String = "MASONRY",
    val favoritesLayoutStyle: String = "MASONRY",
    val homeFeedType: String = "DEFAULT",
    val apiCredentialOne: String = "",
    val apiCredentialTwo: String = "",
    val siteCredentials: String = ""
)

// --- Mappers ---

private fun FavoriteEntity.toDto() = FavoriteDto(postId, thumbnailUrl, previewUrl, fileUrl, fileType, width, height, score, tags, savedAt)
private fun FavoriteDto.toEntity() = FavoriteEntity(postId, thumbnailUrl, previewUrl, fileUrl, fileType, width, height, score, tags, savedAt)

private fun FavoriteArtistEntity.toDto() = FavoriteArtistDto(artistName, displayName, postCount, savedAt)
private fun FavoriteArtistDto.toEntity() = FavoriteArtistEntity(artistName, displayName, postCount, savedAt)

private fun FavoriteTagEntity.toDto() = FavoriteTagDto(tagName, displayName, category, postCount, savedAt)
private fun FavoriteTagDto.toEntity() = FavoriteTagEntity(tagName, displayName, category, postCount, savedAt)

private fun TagBatchEntity.toDto() = BatchDto(id, name, tags, createdAt, updatedAt)
private fun BatchDto.toEntity() = TagBatchEntity(id, name, tags, createdAt, updatedAt)

private fun TagAffinityEntity.toDto() = TagAffinityDto(tag, score, lastUpdated)
private fun TagAffinityDto.toEntity() = TagAffinityEntity(tag, score, lastUpdated)

private fun ArtistAffinityEntity.toDto() = ArtistAffinityDto(artist, score, lastUpdated)
private fun ArtistAffinityDto.toEntity() = ArtistAffinityEntity(artist, score, lastUpdated)

private fun com.example.mediabrowser.domain.model.AppSettings.toDto() = SettingsDto(
    safeSearchEnabled = safeSearchEnabled,
    autoPlayVideos = autoPlayVideos,
    notificationsEnabled = notificationsEnabled,
    gridColumns = gridColumns,
    downloadOverWifiOnly = downloadOverWifiOnly,
    cardCornerRadiusDp = cardCornerRadiusDp,
    accentColorHex = accentColorHex,
    imageQuality = imageQuality.name,
    homeLayoutStyle = homeLayoutStyle.name,
    favoritesLayoutStyle = favoritesLayoutStyle.name,
    homeFeedType = homeFeedType.name,
    apiCredentialOne = apiCredentialOne,
    apiCredentialTwo = apiCredentialTwo,
    siteCredentials = siteCredentials
)