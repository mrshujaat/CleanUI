package com.example.mediabrowser.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import coil.ImageLoader
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.local.dao.DownloadDao
import com.example.mediabrowser.data.local.dao.FavoriteArtistDao
import com.example.mediabrowser.data.local.dao.FavoriteDao
import com.example.mediabrowser.data.local.dao.FavoriteTagDao
import com.example.mediabrowser.data.local.entity.DownloadEntity
import com.example.mediabrowser.data.local.entity.FavoriteArtistEntity
import com.example.mediabrowser.data.local.entity.FavoriteTagEntity
// Explicitly import the distinct naming helpers to bypass compiler ambiguity
import com.example.mediabrowser.data.mapper.toDomain as toPostDomain
import com.example.mediabrowser.data.mapper.toDomain as toDownloadItemDomain
import com.example.mediabrowser.data.mapper.toFavoriteEntity
import com.example.mediabrowser.data.mapper.toFavoriteEntityFromDetail // Added for line 105 handling
import com.example.mediabrowser.data.paging.ArchivePagingSource
import com.example.mediabrowser.data.remote.MediaApiException
import com.example.mediabrowser.data.remote.ArchiveApi
import com.example.mediabrowser.domain.model.ContentRating
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.DownloadStatus
import com.example.mediabrowser.domain.model.FavoriteArtist
import com.example.mediabrowser.domain.model.FavoriteTag
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.domain.model.TagInfo
import com.example.mediabrowser.domain.model.TagSuggestion
import com.example.mediabrowser.download.DownloadScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_PAGE_SIZE = 20

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val archiveApi: ArchiveApi,
    private val favoriteDao: FavoriteDao,
    private val downloadDao: DownloadDao,
    private val imageLoader: ImageLoader,
    private val downloadScheduler: DownloadScheduler,
    private val preferencesDataStore: PreferencesDataStore,
    private val favoriteArtistDao: FavoriteArtistDao,
    private val favoriteTagDao: FavoriteTagDao
) : MediaRepository {

    override fun getPostsPaged(tags: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false,
                // FIXED: Setting initialLoadSize to match pageSize prevents the 
                // paging library from requesting a larger chunk initially that 
                // conflicts with your API's page-based indexing.
                initialLoadSize = DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = {
                ArchivePagingSource(
                    api = archiveApi,
                    query = tags,
                    pageSize = DEFAULT_PAGE_SIZE,
                    preferencesDataStore = preferencesDataStore,
                    favoriteIdsProvider = { observeFavoriteIds().first() }
                )
            }
        ).flow
    }

    override suspend fun getPostDetailsFromPost(post: Post): Result<PostDetail> {
        val isFav = favoriteDao.isFavorite(post.id)
        val artistName = post.tags.lastOrNull()
        val nonArtistTags = if (artistName != null) post.tags.dropLast(1) else post.tags

        return Result.success(
            PostDetail(
                id = post.id,
                fileUrl = post.fileUrl,
                previewUrl = post.previewUrl,
                fileType = post.fileType,
                fileSizeBytes = 0L,
                width = post.width,
                height = post.height,
                score = post.score,
                favoriteCount = 0,
                rating = ContentRating.entries.firstOrNull() ?: ContentRating.valueOf("GENERAL"),
                source = null,
                uploader = artistName,
                createdAt = System.currentTimeMillis(),
                tags = nonArtistTags.map { tagName -> TagInfo(name = tagName, category = TagCategory.GENERAL, postCount = 0) },
                isFavorite = isFav
            )
        )
    }

    override suspend fun searchTags(query: String): Result<List<TagSuggestion>> {
        if (query.isBlank()) return Result.success(emptyList())
        return Result.success(
            listOf(
                TagSuggestion(
                    name = query,
                    displayName = query.replaceFirstChar { it.uppercase() },
                    category = TagCategory.GENERAL,
                    postCount = 0
                )
            )
        )
    }

    override fun getFavoritesPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { favoriteDao.pagingSource() }
        ).flow.map { pagingData -> pagingData.map { entity -> entity.toPostDomain() } }
    }

    override fun observeFavoriteIds(): Flow<Set<Long>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    override suspend fun isFavorite(postId: Long): Boolean = favoriteDao.isFavorite(postId)

    override suspend fun toggleFavorite(post: Post) {
        if (favoriteDao.isFavorite(post.id)) {
            favoriteDao.deleteById(post.id)
        } else {
            favoriteDao.insert(post.toFavoriteEntity())
        }
    }

    override suspend fun toggleFavoriteDetail(detail: PostDetail) {
        if (favoriteDao.isFavorite(detail.id)) {
            favoriteDao.deleteById(detail.id)
        } else {
            favoriteDao.insert(detail.toFavoriteEntityFromDetail())
        }
    }

    override fun observeDownloads(): Flow<List<DownloadItem>> =
        downloadDao.observeAll().map { list -> list.map { it.toDownloadItemDomain() } }

    override suspend fun enqueueDownload(post: Post): Long {
        val fileExtension = if (post.fileType == MediaType.VIDEO) "mp4" else
            post.fileUrl.substringAfterLast('.', missingDelimiterValue = "jpg").substringBefore('?')
        val fileName = "media_${post.id}.$fileExtension"
        val entity = DownloadEntity(
            postId = post.id,
            fileUrl = post.fileUrl,
            fileName = fileName,
            localUri = null,
            mediaType = post.fileType.name,
            status = DownloadStatus.QUEUED.name,
            progress = 0,
            createdAt = System.currentTimeMillis(),
            width = post.width,
            height = post.height,
            score = post.score,
            tagsSnapshot = post.tags.joinToString(" ")
        )
        val downloadId = downloadDao.insert(entity)
        val wifiOnly = preferencesDataStore.settingsFlow.first().downloadOverWifiOnly

        downloadScheduler.enqueueDownload(
            downloadId = downloadId,
            fileUrl = post.fileUrl,
            fileName = fileName,
            mediaType = post.fileType,
            wifiOnly = wifiOnly
        )
        return downloadId
    }

    override suspend fun deleteDownload(id: Long) {
        downloadScheduler.cancelDownload(id)
        downloadDao.deleteById(id)
    }

    override suspend fun clearImageCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    override suspend fun getCacheSizeBytes(): Long {
        return imageLoader.diskCache?.size ?: 0L
    }

    override fun observeFavoriteArtists(): Flow<List<FavoriteArtist>> =
        favoriteArtistDao.observeAll().map { list ->
            list.map {
                FavoriteArtist(it.artistName, it.displayName, it.postCount, it.savedAt)
            }
        }

    override suspend fun isArtistFavorite(artistName: String): Boolean =
        favoriteArtistDao.isFavorite(artistName)

    override suspend fun toggleFavoriteArtist(artistName: String, displayName: String, postCount: Int) {
        if (favoriteArtistDao.isFavorite(artistName)) {
            favoriteArtistDao.deleteByName(artistName)
        } else {
            favoriteArtistDao.insert(
                FavoriteArtistEntity(artistName, displayName, postCount, System.currentTimeMillis())
            )
        }
    }

    override fun observeFavoriteTags(): Flow<List<FavoriteTag>> =
        favoriteTagDao.observeAll().map { list ->
            list.map {
                FavoriteTag(
                    it.tagName,
                    it.displayName,
                    TagCategory.entries.find { c -> c.name == it.category } ?: TagCategory.GENERAL,
                    it.postCount,
                    it.savedAt
                )
            }
        }

    override suspend fun isTagFavorite(tagName: String): Boolean =
        favoriteTagDao.isFavorite(tagName)

    override suspend fun toggleFavoriteTag(
        tagName: String,
        displayName: String,
        category: TagCategory,
        postCount: Int
    ) {
        if (favoriteTagDao.isFavorite(tagName)) {
            favoriteTagDao.deleteByName(tagName)
        } else {
            favoriteTagDao.insert(
                FavoriteTagEntity(tagName, displayName, category.name, postCount, System.currentTimeMillis())
            )
        }
    }

    private fun mapNetworkException(throwable: Throwable): Throwable = when (throwable) {
        is SocketTimeoutException -> MediaApiException.Timeout(throwable)
        is HttpException -> MediaApiException.ServerError(throwable.code(), throwable)
        is IOException -> MediaApiException.NoConnectivity(throwable)
        is MediaApiException -> throwable
        else -> MediaApiException.Unknown(throwable)
    }
}