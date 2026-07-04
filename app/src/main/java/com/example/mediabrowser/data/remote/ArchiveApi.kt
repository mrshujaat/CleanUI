package com.example.mediabrowser.data.remote

import kotlinx.serialization.json.JsonElement
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
        @Query("tags") tags: String? = null,
        @Query("api_key") apiKey: String? = null,
        @Query("user_id") userId: String? = null
    ): JsonElement

    /**
     * Searches for tags matching a name pattern, for autocomplete suggestions.
     * @param namePattern SQL LIKE-style pattern, e.g. "app%" to match tags starting with "app".
     * @param limit Max number of suggestions to return.
     * @param orderBy Sort order — "count" ranks by post count (most popular first).
     */
    @GET("index.php?page=dapi&s=tag&q=index&json=1")
    suspend fun searchTags(
        @Query("name_pattern") namePattern: String,
        @Query("limit") limit: Int = 25,
        @Query("orderby") orderBy: String = "count",
        @Query("api_key") apiKey: String? = null,
        @Query("user_id") userId: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    /**
     * Native autocomplete endpoint — the same one the rule34.xxx web search box
     * (and r34.app) use. Substring + alias matching, results pre-ranked by the
     * site. Returns JSON: [{ "label": "tag (123)", "value": "tag", "type": "..." }].
     * Hosted at the host root, so we pass an absolute URL via @Url.
     */
    @GET
    suspend fun autocompleteTags(
        @retrofit2.http.Url url: String,
        @Query("q") query: String,
        @Query("api_key") apiKey: String? = null,
        @Query("user_id") userId: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    /**
     * Looks up a SINGLE tag by exact name to resolve its real category
     * (artist/character/copyright/meta/general) and post count. The post JSON
     * carries only bare tag strings, so this is the authoritative source. Looked
     * up per-tag (cached) because rule34's `names` list-param is unreliable.
     */
    @GET("index.php?page=dapi&s=tag&q=index&json=1")
    suspend fun getTagByName(
        @Query("name") name: String,
        @Query("api_key") apiKey: String? = null,
        @Query("user_id") userId: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>
}