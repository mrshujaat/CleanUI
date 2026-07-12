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

    /** Single highest-scoring post for a tag — used as a representative thumbnail. */
    suspend fun getTopPostForTag(tag: String): Post?

    /** Flat (non-paged) list of posts — used for small home-row previews. */
    suspend fun getPostsFlat(tags: String, limit: Int): List<Post>

    /** Builds full detail for a post the caller already has in hand (from a grid/feed). */
    suspend fun getPostDetailsFromPost(post: Post): Result<PostDetail>

    /**
     * Instant, zero-network detail built from the local tag-category cache and
     * heuristics — painted immediately while [getPostDetailsFromPost] resolves.
     */
    suspend fun getPostDetailInstant(post: Post): PostDetail

    suspend fun searchTags(query: String): Result<List<TagSuggestion>>

    fun getFavoritesPaged(): Flow<PagingData<Post>>

    /** The Poison Feed: personalized, affinity-ranked recommendations. */
    fun getPoisonFeedPaged(): Flow<PagingData<Post>>

    /** Record that the user opened/viewed a post (weak taste signal). */
    suspend fun recordPostView(post: Post)

    /** Record a search query as a set of tags (explicit taste signal). */
    suspend fun recordSearch(tags: List<String>)

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

    // --- Tag batches ("My Poison") ---

    fun observeTagBatches(): Flow<List<com.example.mediabrowser.domain.model.TagBatch>>

    suspend fun createTagBatch(name: String, tags: List<String>): Long

    suspend fun updateTagBatch(id: Long, name: String, tags: List<String>)

    suspend fun deleteTagBatch(id: Long)

    /** Replaces all batches with the given set — used by import. */
    suspend fun replaceAllTagBatches(batches: List<com.example.mediabrowser.domain.model.TagBatch>)
}