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
            val httpResponse = api.getPosts(
                pageId = currentPage,
                limit = pageSize,
                tags = query.ifBlank { null },
                apiKey = settings.apiCredentialOne.trim().ifBlank { null },
                userId = settings.apiCredentialTwo.trim().ifBlank { null }
            )

            // parsePostResponse auto-detects JSON vs XML — some booru forks
            // (Realbooru is a known offender) ignore `json=1` and return XML.
            val body = httpResponse.body()?.string().orEmpty()
            val response: List<ArchivePostDto> =
                com.example.mediabrowser.data.remote.parsePostResponse(body)
            Log.d("ArchivePagingSource", "Loaded page $currentPage with ${response.size} items")

            // Handle empty response gracefully
            if (response.isEmpty()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (currentPage == 0) null else currentPage - 1,
                    nextKey = null
                )
            }

            val cdnBase = com.example.mediabrowser.domain.model.BooruSite.fromSettings(settings).cdnBase
            val posts = response.map { dto: ArchivePostDto ->
                dto.toPost(isFavorite = dto.id in favoriteIds, cdnBase = cdnBase)
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