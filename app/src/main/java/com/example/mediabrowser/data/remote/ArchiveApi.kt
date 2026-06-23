package com.example.mediabrowser.data.remote

import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ArchiveApi {
    /**
     * Fetches posts with explicit pagination parameters.
     * @param pageId The page index (pid).
     * @param limit The number of items per page.
     * @param tags The search query string (optional).
     */
    @GET("index.php?page=dapi&s=post&q=index&json=1")
    suspend fun getPosts(
        @Query("pid") pageId: Int,
        @Query("limit") limit: Int,
        @Query("tags") tags: String? = null
    ): List<ArchivePostDto>
}