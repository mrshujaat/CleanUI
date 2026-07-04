package com.example.mediabrowser.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.poison.PoisonEngine
import com.example.mediabrowser.data.remote.ArchiveApi
import com.example.mediabrowser.data.mapper.toPost
import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import com.example.mediabrowser.domain.model.Post
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Poison Feed source. Builds a candidate pool from the user's top affinity
 * tags AND the artists from their favourites (the "hybrid" sourcing), ranks that
 * pool client-side by affinity, and mixes in ~20% random posts for exploration so
 * the feed doesn't collapse into a filter bubble.
 *
 * Performance: the heavy lifting (scoring) is a sum over a small in-memory map of
 * affinity scores, done once per page on the paging dispatcher — never on the UI
 * thread. We over-fetch a little per page so that after ranking + dedup we still
 * have a full page of good results.
 */
class PoisonPagingSource(
    private val api: ArchiveApi,
    private val engine: PoisonEngine,
    private val preferencesDataStore: PreferencesDataStore,
    private val favoriteArtistsProvider: suspend () -> List<String>,
    private val favoriteIdsProvider: suspend () -> Set<Long>,
    private val pageSize: Int
) : PagingSource<Int, Post>() {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedTerms: List<String>? = null
    private var cachedFavoriteIds: Set<Long>? = null

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    /**
     * Build the OR-query from top tags + favourite artists. rule34 treats space as
     * AND, and `{a ~ b ~ c}` as OR, so we want the OR form to pull posts matching
     * *any* of the user's interests rather than the (usually empty) intersection.
     */
    /**
     * Pick the tag/artist to query for this page. rule34's multi-tag OR syntax is
     * finicky and was erroring, so instead we rotate through the user's top
     * interests one per page — each query is plain, always-valid rule34 syntax
     * (a single tag), and across pages the feed still spans everything they like.
     * Ranking + 20% exploration still happen client-side on each page's results.
     */
    private suspend fun termsForFeed(): List<String> {
        cachedTerms?.let { return it }
        val topTags = engine.topTags(10)
        val artists = favoriteArtistsProvider()
            .map { it.trim().lowercase().replace(' ', '_') }
            .filter { it.isNotBlank() }
            .take(5)
        // Interleave artists into the tag list so the feed pulls from both.
        val combined = (topTags + artists).distinct()
        cachedTerms = combined
        return combined
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 0
        return try {
            val favoriteIds = cachedFavoriteIds ?: favoriteIdsProvider().also { cachedFavoriteIds = it }
            val settings = preferencesDataStore.settingsFlow.first()
            val terms = termsForFeed()

            // Pick this page's query tag. No signal yet → null tag → popular feed,
            // so the Poison tab is never blank even with empty history.
            val queryTag: String? = if (terms.isEmpty()) {
                null
            } else {
                terms[page % terms.size]
            }

            val raw = fetchPosts(
                tags = queryTag,
                page = page / (terms.size.coerceAtLeast(1)),  // advance the API page once we've cycled all terms
                limit = pageSize * 2,
                apiKey = settings.apiCredentialOne.trim().ifBlank { null },
                userId = settings.apiCredentialTwo.trim().ifBlank { null }
            )

            if (raw.isEmpty()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = null
                )
            }

            val candidates = raw.map { it.toPost(isFavorite = it.id in favoriteIds) }

            // Rank by affinity. Split exploration (~20%) from exploitation so the
            // user keeps discovering new things.
            val ranked = candidates
                .map { it to engine.scorePost(it) }
                .sortedByDescending { it.second }
                .map { it.first }

            val exploitCount = (pageSize * 0.8).toInt().coerceAtLeast(1)
            val exploit = ranked.take(exploitCount)
            // The remainder of the pool, shuffled, supplies the random 20%.
            val explore = ranked.drop(exploitCount).shuffled().take(pageSize - exploit.size)

            // Interleave so random posts are sprinkled through, not bolted on the end.
            val pagePosts = interleave(exploit, explore)

            LoadResult.Page(
                data = pagePosts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (pagePosts.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Log.e("PoisonPagingSource", "load failed", e)
            // Don't hard-error the first page — that blanks the whole Home screen
            // (header included). Return an empty page so Popular/Series still show
            // and the "interact to build your feed" message appears. Later pages can
            // surface a normal append error.
            if ((params.key ?: 0) == 0) {
                LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            } else {
                LoadResult.Error(e)
            }
        }
    }

    /** Sprinkle [b] elements through [a] roughly evenly. */
    private fun interleave(a: List<Post>, b: List<Post>): List<Post> {
        if (b.isEmpty()) return a
        if (a.isEmpty()) return b
        val out = ArrayList<Post>(a.size + b.size)
        val step = (a.size / (b.size + 1)).coerceAtLeast(1)
        var bi = 0
        for (i in a.indices) {
            out.add(a[i])
            if (bi < b.size && (i + 1) % step == 0) {
                out.add(b[bi]); bi++
            }
        }
        while (bi < b.size) { out.add(b[bi]); bi++ }
        return out
    }

    private suspend fun fetchPosts(
        tags: String?,
        page: Int,
        limit: Int,
        apiKey: String?,
        userId: String?
    ): List<ArchivePostDto> {
        val response = api.getPosts(
            pageId = page,
            limit = limit,
            tags = tags,
            apiKey = apiKey,
            userId = userId
        )
        // Same defensive parsing as ArchivePagingSource: the API returns a string
        // ("Missing authentication", etc.) instead of an array on error, and the
        // plain .jsonArray accessor throws on a type mismatch.
        val arrayOrNull = (response as? JsonArray)
        val primitiveOrNull = (response as? JsonPrimitive)
        return when {
            arrayOrNull != null ->
                json.decodeFromString<List<ArchivePostDto>>(response.toString())
            primitiveOrNull != null -> {
                Log.e("PoisonPagingSource", "API message: ${primitiveOrNull.content}")
                emptyList()
            }
            else -> emptyList()
        }
    }
}