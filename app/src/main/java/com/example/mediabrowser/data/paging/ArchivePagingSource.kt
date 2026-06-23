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
            
            // Explicitly use pageSize to ensure consistent page-based indexing for the API
            val response = api.getPosts(
                pageId = currentPage,
                limit = pageSize,
                tags = query.ifBlank { null }
            )

            Log.d("ArchivePagingSource", "Loaded page $currentPage with ${response.size} items")

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