package com.example.mediabrowser.data.repository

import androidx.paging.PagingData
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.FavoriteArtist
import com.example.mediabrowser.domain.model.FavoriteTag
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.domain.model.TagSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all media data — combines the remote API and
 * local Room database. ViewModels depend only on this interface, never on
 * Retrofit or Room directly.
 */
interface MediaRepository {

    fun getPostsPaged(tags: String = ""): Flow<PagingData<Post>>

    /** Builds full detail for a post the caller already has in hand (from a grid/feed). */
    suspend fun getPostDetailsFromPost(post: Post): Result<PostDetail>

    suspend fun searchTags(query: String): Result<List<TagSuggestion>>

    fun getFavoritesPaged(): Flow<PagingData<Post>>

    fun observeFavoriteIds(): Flow<Set<Long>>

    suspend fun isFavorite(postId: Long): Boolean

    suspend fun toggleFavorite(post: Post)

    suspend fun toggleFavoriteDetail(detail: PostDetail)

    fun observeDownloads(): Flow<List<DownloadItem>>

    suspend fun enqueueDownload(post: Post): Long

    suspend fun deleteDownload(id: Long)

    suspend fun clearImageCache()

    suspend fun getCacheSizeBytes(): Long

    // --- Favorite artists ---

    fun observeFavoriteArtists(): Flow<List<FavoriteArtist>>

    suspend fun isArtistFavorite(artistName: String): Boolean

    suspend fun toggleFavoriteArtist(artistName: String, displayName: String, postCount: Int)

    // --- Favorite tags ---

    fun observeFavoriteTags(): Flow<List<FavoriteTag>>

    suspend fun isTagFavorite(tagName: String): Boolean

    suspend fun toggleFavoriteTag(
        tagName: String,
        displayName: String,
        category: TagCategory,
        postCount: Int
    )
}
