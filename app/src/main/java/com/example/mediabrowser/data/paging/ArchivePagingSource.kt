package com.example.mediabrowser.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.remote.ArchiveApi
import com.example.mediabrowser.data.remote.MediaApiException
import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.data.mapper.toPost
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class ArchivePagingSource(
    private val api: ArchiveApi,
    private val query: String,
    private val pageSize: Int,
    private val preferencesDataStore: PreferencesDataStore,
    private val favoriteIdsProvider: suspend () -> Set<Long>
) : PagingSource<Int, Post>() {

    private var cachedFavoriteIds: Set<Long>? = null
    private val json = Json { ignoreUnknownKeys = true }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val currentPage = params.key ?: 0
        
        return try {
            val favoriteIds = cachedFavoriteIds ?: favoriteIdsProvider().also { cachedFavoriteIds = it }
            val settings = preferencesDataStore.settingsFlow.first()
            
            // Explicitly use pageSize to ensure consistent page-based indexing for the API
            val jsonResponse = api.getPosts(
                pageId = currentPage,
                limit = pageSize,
                tags = query.ifBlank { null },
                apiKey = settings.apiCredentialOne.trim().ifBlank { null },
                userId = settings.apiCredentialTwo.trim().ifBlank { null }
            )

            Log.d("ArchivePagingSource", "Raw JSON response: ${jsonResponse.toString()}")

            // Handle different response types. Use the *OrNull accessors — the
            // plain .jsonArray / .jsonPrimitive properties THROW on a type mismatch
            // rather than returning null, which previously crashed the pager when
            // the API returned an error string instead of an array.
            val arrayOrNull = (jsonResponse as? kotlinx.serialization.json.JsonArray)
            val primitiveOrNull = (jsonResponse as? kotlinx.serialization.json.JsonPrimitive)
            val response: List<ArchivePostDto> = when {
                arrayOrNull != null -> {
                    Log.d("ArchivePagingSource", "Loaded page $currentPage with ${arrayOrNull.size} items")
                    json.decodeFromString<List<ArchivePostDto>>(jsonResponse.toString())
                }
                primitiveOrNull != null -> {
                    // API returned a string message (likely an auth error).
                    val message = primitiveOrNull.content
                    Log.e("ArchivePagingSource", "API returned message: $message")
                    emptyList()
                }
                else -> {
                    Log.d("ArchivePagingSource", "Unexpected response for page $currentPage: ${jsonResponse::class.simpleName}")
                    emptyList()
                }
            }

            // Handle empty response gracefully
            if (response.isEmpty()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (currentPage == 0) null else currentPage - 1,
                    nextKey = null
                )
            }

            val posts = response.map { dto: ArchivePostDto ->
                dto.toPost(isFavorite = dto.id in favoriteIds)
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (currentPage == 0) null else currentPage - 1,
                // Only provide nextKey if we received enough items to suggest there's a next page
                nextKey = if (response.size < pageSize) null else currentPage + 1
            )
        } catch (e: SocketTimeoutException) {
            LoadResult.Error(MediaApiException.Timeout(e))
        } catch (e: IOException) {
            LoadResult.Error(MediaApiException.NoConnectivity(e))
        } catch (e: HttpException) {
            LoadResult.Error(MediaApiException.ServerError(e.code(), e))
        } catch (e: Exception) {
            Log.e("ArchivePagingSource", "Error loading data", e)
            LoadResult.Error(MediaApiException.Unknown(e))
        }
    }
}