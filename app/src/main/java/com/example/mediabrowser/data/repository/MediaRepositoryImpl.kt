package com.example.mediabrowser.data.repository

import kotlinx.serialization.json.jsonArray
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
import com.example.mediabrowser.data.mapper.toPost
import com.example.mediabrowser.data.paging.ArchivePagingSource
import com.example.mediabrowser.data.remote.MediaApiException
import com.example.mediabrowser.data.remote.ArchiveApi
import com.example.mediabrowser.data.remote.dto.ArchivePostDto
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_PAGE_SIZE = 20
private const val AUTOCOMPLETE_URL = "https://api.rule34.xxx/autocomplete.php"

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val archiveApi: ArchiveApi,
    private val favoriteDao: FavoriteDao,
    private val downloadDao: DownloadDao,
    private val imageLoader: ImageLoader,
    private val downloadScheduler: DownloadScheduler,
    private val preferencesDataStore: PreferencesDataStore,
    private val favoriteArtistDao: FavoriteArtistDao,
    private val favoriteTagDao: FavoriteTagDao,
    private val tagBatchDao: com.example.mediabrowser.data.local.dao.TagBatchDao,
    private val poisonEngine: com.example.mediabrowser.data.poison.PoisonEngine
) : MediaRepository {

    // Resolved tag categories are stable; cache them in-memory AND on disk so
    // opening a post detail is instant for every tag ever seen — across app
    // restarts, like r34.app's local tag database.
    private val tagCategoryCache = java.util.concurrent.ConcurrentHashMap<String, TagSuggestion>()
    private val tagCacheLoaded = java.util.concurrent.atomic.AtomicBoolean(false)
    private val tagCacheFile by lazy { java.io.File(appContext.filesDir, "tag_categories.tsv") }

    /** Load the persisted tag-category cache once, lazily, off the main thread. */
    private suspend fun ensureTagCacheLoaded() {
        if (!tagCacheLoaded.compareAndSet(false, true)) return
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!tagCacheFile.exists()) return@withContext
                tagCacheFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val name = parts[0]
                        val category = TagCategory.entries.find { it.name == parts[1] } ?: TagCategory.GENERAL
                        val count = parts[2].toIntOrNull() ?: 0
                        tagCategoryCache[name] = TagSuggestion(
                            name = name,
                            displayName = name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            category = category,
                            postCount = count
                        )
                    }
                }
                android.util.Log.d("TagCategories", "loaded ${tagCategoryCache.size} cached tag categories")
            } catch (e: Exception) {
                android.util.Log.w("TagCategories", "failed to load tag cache", e)
            }
        }
    }

    /** Persist the whole cache (a few hundred KB at most) — cheap, atomic-ish. */
    private suspend fun persistTagCache() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tmp = java.io.File(appContext.filesDir, "tag_categories.tsv.tmp")
                tmp.bufferedWriter().use { w ->
                    tagCategoryCache.forEach { (name, s) ->
                        w.write(name); w.write("\t"); w.write(s.category.name); w.write("\t"); w.write(s.postCount.toString()); w.write("\n")
                    }
                }
                tmp.renameTo(tagCacheFile)
            } catch (e: Exception) {
                android.util.Log.w("TagCategories", "failed to persist tag cache", e)
            }
        }
    }

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

    /**
     * Fetches the single highest-scoring post for a given tag — used as the
     * representative thumbnail for a "Top Series" row card on Home.
     */
    override suspend fun getTopPostForTag(tag: String): Post? {
        return try {
            val settings = preferencesDataStore.settingsFlow.first()
            val jsonResponse = archiveApi.getPosts(
                pageId = 0,
                limit = 1,
                tags = "sort:score $tag",
                apiKey = settings.apiCredentialOne.trim().ifBlank { null },
                userId = settings.apiCredentialTwo.trim().ifBlank { null }
            )

            val posts: List<ArchivePostDto> = when {
                jsonResponse.jsonArray != null ->
                    Json { ignoreUnknownKeys = true }.decodeFromString(jsonResponse.toString())
                else -> emptyList()
            }

            val dto = posts.firstOrNull() ?: return null
            val favoriteIds = observeFavoriteIds().first()
            dto.toPost(isFavorite = dto.id in favoriteIds)
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "getTopPostForTag failed for $tag", e)
            null
        }
    }

    /**
     * Flat (non-paged) list of posts — used for the small "Most Popular" and
     * "Trending" preview rows on Home, which only need a handful of items.
     */
    override suspend fun getPostsFlat(tags: String, limit: Int): List<Post> {
        return try {
            val settings = preferencesDataStore.settingsFlow.first()
            val jsonResponse = archiveApi.getPosts(
                pageId = 0,
                limit = limit,
                tags = tags.ifBlank { null },
                apiKey = settings.apiCredentialOne.trim().ifBlank { null },
                userId = settings.apiCredentialTwo.trim().ifBlank { null }
            )
            val posts: List<ArchivePostDto> = when {
                jsonResponse.jsonArray != null ->
                    Json { ignoreUnknownKeys = true }.decodeFromString(jsonResponse.toString())
                else -> emptyList()
            }
            val favoriteIds = observeFavoriteIds().first()
            posts.map { dto -> dto.toPost(isFavorite = dto.id in favoriteIds) }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "getPostsFlat failed for tags=$tags", e)
            emptyList()
        }
    }

    override suspend fun getPostDetailsFromPost(post: Post): Result<PostDetail> {
        val isFav = favoriteDao.isFavorite(post.id)

        // Resolve real categories from the tag endpoint (bounded by a timeout so a
        // slow lookup can't freeze the detail). The post JSON has no categories, so
        // this is what makes Artist/Character/etc. populate correctly.
        val categorizedTags = try {
            withTimeout(4000L) { resolveTagCategories(post.tags) }
        } catch (e: Exception) {
            android.util.Log.w("TagCategories", "resolve timed out; using cache+heuristic", e)
            categorizeFromCacheOrHeuristic(post.tags)
        }

        // Surface the first artist as uploader, but keep artists in the tag list so
        // the detail UI can render an Artists section from ARTIST-category tags.
        val artistName = categorizedTags.firstOrNull { it.category == TagCategory.ARTIST }?.name

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
                tags = categorizedTags,
                isFavorite = isFav
            )
        )
    }

    /** Instant categorization from cache, else local heuristic (timeout fallback). */
    private fun categorizeFromCacheOrHeuristic(tagNames: List<String>): List<TagInfo> =
        tagNames.map { name ->
            val meta = tagCategoryCache[name]
            if (meta != null) TagInfo(name = meta.name, category = meta.category, postCount = meta.postCount)
            else TagInfo(name = name, category = categorizeTag(name), postCount = 0)
        }

    /**
     * Zero-network detail for INSTANT display: categories come from the
     * persistent tag cache (exact) or the local heuristic (approximate).
     * The UI paints this immediately, then swaps in [getPostDetailsFromPost]'s
     * fully-resolved version when the (single) network lookup lands.
     */
    override suspend fun getPostDetailInstant(post: Post): PostDetail {
        ensureTagCacheLoaded()
        val isFav = favoriteDao.isFavorite(post.id)
        val categorizedTags = categorizeFromCacheOrHeuristic(post.tags)
        val artistName = categorizedTags.firstOrNull { it.category == TagCategory.ARTIST }?.name
        return PostDetail(
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
            tags = categorizedTags,
            isFavorite = isFav
        )
    }

    /**
     * Resolves each tag's real category. FIXED for speed: instead of one HTTP
     * request per tag (a 40-tag post = 40 requests racing the timeout, which is
     * why details were slow and the artist often never resolved), this sends
     * ONE batch request via the `names` list-param covering every uncached tag.
     * Anything the batch response missed (the param occasionally drops entries)
     * gets a targeted per-tag fallback — normally 0–3 tags, not 40. Results are
     * persisted to disk, so previously-seen tags resolve instantly forever.
     */
    private suspend fun resolveTagCategories(tagNames: List<String>): List<TagInfo> {
        if (tagNames.isEmpty()) return emptyList()
        ensureTagCacheLoaded()
        val distinct = tagNames.distinct()
        val uncached = distinct.filter { !tagCategoryCache.containsKey(it) }

        if (uncached.isNotEmpty()) {
            try {
                val settings = preferencesDataStore.settingsFlow.first()
                val apiKey = settings.apiCredentialOne.trim().ifBlank { null }
                val userId = settings.apiCredentialTwo.trim().ifBlank { null }

                // Phase 1: ONE request for the whole set.
                var batchResolvedAny = false
                try {
                    val response = archiveApi.getTagsByNames(
                        names = uncached.joinToString(" "),
                        limit = (uncached.size * 2).coerceAtLeast(100),
                        apiKey = apiKey, userId = userId
                    )
                    parseTagSuggestionsXml(response.body()?.string().orEmpty()).forEach { s ->
                        tagCategoryCache[s.name] = s
                        batchResolvedAny = true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TagCategories", "batch lookup failed", e)
                }

                // Phase 2: per-tag fallback for whatever the batch missed. If the
                // batch worked at all this is a handful of stragglers; if the
                // endpoint returned nothing (rare), fall back to the old full
                // per-tag path so categories still resolve.
                val missed = uncached.filter { !tagCategoryCache.containsKey(it) }
                val toFetchIndividually = if (batchResolvedAny) missed else uncached
                if (toFetchIndividually.isNotEmpty()) {
                    toFetchIndividually.chunked(8).forEach { batch ->
                        coroutineScope {
                            batch.map { tagName ->
                                async {
                                    try {
                                        val response = archiveApi.getTagByName(
                                            name = tagName, apiKey = apiKey, userId = userId
                                        )
                                        parseTagSuggestionsXml(response.body()?.string().orEmpty())
                                            .firstOrNull { it.name == tagName }
                                            ?.let { tagCategoryCache[tagName] = it }
                                    } catch (e: Exception) {
                                        android.util.Log.w("TagCategories", "lookup failed: $tagName", e)
                                    }
                                }
                            }.awaitAll()
                        }
                    }
                }

                persistTagCache()
            } catch (e: Exception) {
                android.util.Log.w("TagCategories", "metadata lookup failed; using heuristic", e)
            }
        }

        return tagNames.map { name ->
            val meta = tagCategoryCache[name]
            if (meta != null) TagInfo(name = meta.name, category = meta.category, postCount = meta.postCount)
            else TagInfo(name = name, category = categorizeTag(name), postCount = 0)
        }
    }

    private fun categorizeTag(tag: String): TagCategory {
        val lowerTag = tag.lowercase()
        
        // Meta tags - technical/descriptive tags
        val metaTags = setOf(
            "ai_generated", "absurdres", "highres", "hi_res", "high_quality", "high_resolution",
            "lowres", "commentary_request", "digital_media_(artwork)", "digital_drawing_(artwork)",
            "traditional_media_(artwork)", "sketch", "vector", "flash", "animated", "video",
            "sound", "sound_effects", "tagme", "bad_id", "bad_pixiv_id", "bad_link",
            "artist_request", "fictional", "official_art", "cosplay", "3d", "2d",
            "sample", "alternate_breast_size", "alternate_costume", "alternate_view",
            "censored", "uncensored", "partial_censored", "mosaic_censorship",
            "bar_censor", "light_censor", "sticker", "text", "watermark"
        )
        
        // Copyright tags - series/franchise names (usually contain parentheses or specific patterns)
        if (lowerTag.contains("_(series)") || lowerTag.contains("_(franchise)") ||
            lowerTag.contains("_(game)") || lowerTag.contains("_(anime)") ||
            lowerTag.contains("_(manga)") || lowerTag.contains("_(novel)") ||
            lowerTag.contains("_(visual)") || lowerTag.contains("_(project)") ||
            lowerTag.contains("_(universe)") || lowerTag.contains("_(world)") ||
            lowerTag == "horimiya" || lowerTag == "fate" || lowerTag == "naruto" ||
            lowerTag == "one_piece" || lowerTag == "dragon_ball" || lowerTag == "pokemon" ||
            lowerTag == "arknights" || lowerTag == "genshin_impact" || lowerTag == "hololive") {
            return TagCategory.COPYRIGHT
        }
        
        // Meta tags
        if (metaTags.contains(lowerTag) || lowerTag.startsWith("absurdres") ||
            lowerTag.startsWith("highres") || lowerTag.startsWith("lowres")) {
            return TagCategory.META
        }
        
        // Artist tags - typically the uploader or specific artist naming patterns
        // This is a heuristic - in a real implementation, you'd query the API for artist info
        if (lowerTag.contains("artist:") || lowerTag.contains("(artist)")){
            //post.tags.size > 0 && lowerTag == post.tags.lastOrNull()?.lowercase()) {
            // Don't auto-categorize last tag as artist anymore - let it be general
            // unless it has explicit artist markers
        }
        
        // Character tags - usually have specific naming patterns
        // This is a simplified heuristic
        if (lowerTag.contains("_(character)") || lowerTag.contains("char:") ||
            lowerTag.matches(Regex("^[a-z]+_[a-z_]+\\(.*\\)$"))) {
            return TagCategory.CHARACTER
        }
        
        // Default to GENERAL for most descriptive tags
        return TagCategory.GENERAL
    }

    override suspend fun searchTags(query: String): Result<List<TagSuggestion>> {
        if (query.isBlank()) return Result.success(emptyList())
        val settings = preferencesDataStore.settingsFlow.first()
        val apiKey = settings.apiCredentialOne.trim().ifBlank { null }
        val userId = settings.apiCredentialTwo.trim().ifBlank { null }

        // The caller (ViewModel) already extracts the trailing term and normalizes
        // spaces to underscores, so use the query as-is — don't re-split on space,
        // which would break underscore tags like "naruto_uzumaki".
        val term = query.trim()
        if (term.isEmpty()) return Result.success(emptyList())

        // Two sources, merged:
        //  1) PRIMARY (orange): the native autocomplete endpoint — same ranked,
        //     alias-aware results r34.app shows. These come first.
        //  2) SECONDARY: prefix-match from the tag endpoint (cat → cat_girl,
        //     cat_ears, caty…) for broader coverage autocomplete may miss.
        // Autocomplete results win on dedupe so a tag in both stays orange.
        val primary = fetchAutocompleteSuggestions(term, apiKey, userId)
        val secondary = fetchPrefixSuggestions(term, apiKey, userId)

        val seen = HashSet<String>()
        val merged = ArrayList<TagSuggestion>(primary.size + secondary.size)
        primary.forEach { if (seen.add(it.name)) merged += it }
        secondary.forEach { if (seen.add(it.name)) merged += it }

        return Result.success(merged)
    }

    /** Native autocomplete — marked primary (orange), kept in the site's ranking. */
    private suspend fun fetchAutocompleteSuggestions(
        term: String,
        apiKey: String?,
        userId: String?
    ): List<TagSuggestion> = try {
        val response = archiveApi.autocompleteTags(
            url = AUTOCOMPLETE_URL,
            query = term,
            apiKey = apiKey,
            userId = userId
        )
        parseAutocompleteJson(response.body()?.string().orEmpty())
    } catch (e: Exception) {
        android.util.Log.w("SearchTags", "autocomplete failed for '$term'", e)
        emptyList()
    }

    /** Prefix-match supplements, sorted by popularity, marked non-primary. */
    private suspend fun fetchPrefixSuggestions(
        term: String,
        apiKey: String?,
        userId: String?
    ): List<TagSuggestion> = try {
        val response = archiveApi.searchTags(
            namePattern = "$term%",
            limit = 1000,
            apiKey = apiKey,
            userId = userId
        )
        parseTagSuggestionsXml(response.body()?.string().orEmpty())
            .filter { it.name.startsWith(term, ignoreCase = true) }
            .sortedByDescending { it.postCount }
            .map { it.copy(isPrimary = false) }
    } catch (e: Exception) {
        android.util.Log.w("SearchTags", "prefix search failed for '$term'", e)
        emptyList()
    }

    /**
     * Parses the autocomplete JSON array. Each element:
     *   { "label": "tag (123)", "value": "tag", "type": "copyright" }
     * Post count is parsed from the label's trailing "(N)"; type → category.
     * All results are marked primary (orange).
     */
    private fun parseAutocompleteJson(json: String): List<TagSuggestion> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = Json.parseToJsonElement(json).jsonArray
            arr.mapNotNull { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                fun str(key: String): String? =
                    (obj[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

                val value = str("value")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val label = str("label") ?: value
                val count = label.substringAfterLast('(', "")
                    .substringBefore(')', "")
                    .filter { it.isDigit() }
                    .toIntOrNull() ?: 0

                TagSuggestion(
                    name = value,
                    displayName = value.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    category = categoryFromTypeString(str("type")),
                    postCount = count,
                    isPrimary = true
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchTags", "Failed to parse autocomplete JSON", e)
            emptyList()
        }
    }

    private fun categoryFromTypeString(type: String?): TagCategory = when (type?.lowercase()) {
        "artist" -> TagCategory.ARTIST
        "copyright" -> TagCategory.COPYRIGHT
        "character" -> TagCategory.CHARACTER
        "metadata", "meta" -> TagCategory.META
        else -> TagCategory.GENERAL
    }

    private fun parseTagSuggestionsXml(xml: String): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(java.io.StringReader(xml))

            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "tag") {
                    val name = parser.getAttributeValue(null, "name") ?: continue
                    val count = parser.getAttributeValue(null, "count")?.toIntOrNull() ?: 0
                    val typeCode = parser.getAttributeValue(null, "type")?.toIntOrNull() ?: 0

                    val category = when (typeCode) {
                        1 -> TagCategory.ARTIST
                        3 -> TagCategory.COPYRIGHT
                        4 -> TagCategory.CHARACTER
                        5 -> TagCategory.META
                        else -> TagCategory.GENERAL
                    }

                    suggestions.add(
                        TagSuggestion(
                            name = name,
                            displayName = name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            category = category,
                            postCount = count
                        )
                    )
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchTags", "Failed to parse tag XML", e)
        }
        return suggestions
    }

    override fun getFavoritesPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { favoriteDao.pagingSource() }
        ).flow.map { pagingData -> pagingData.map { entity -> entity.toPostDomain() } }
    }

    override fun getPoisonFeedPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = {
                com.example.mediabrowser.data.paging.PoisonPagingSource(
                    api = archiveApi,
                    engine = poisonEngine,
                    preferencesDataStore = preferencesDataStore,
                    favoriteArtistsProvider = {
                        favoriteArtistDao.observeAll().first().map { it.artistName }
                    },
                    favoriteIdsProvider = { observeFavoriteIds().first() },
                    pageSize = DEFAULT_PAGE_SIZE
                )
            }
        ).flow
    }

    override suspend fun recordPostView(post: Post) {
        poisonEngine.record(post, com.example.mediabrowser.data.poison.PoisonEngine.InteractionType.VIEW)
    }

    override suspend fun recordSearch(tags: List<String>) {
        poisonEngine.recordSearch(tags)
    }

    override fun observeFavoriteIds(): Flow<Set<Long>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    override suspend fun isFavorite(postId: Long): Boolean = favoriteDao.isFavorite(postId)

    override suspend fun toggleFavorite(post: Post) {
        if (favoriteDao.isFavorite(post.id)) {
            favoriteDao.deleteById(post.id)
        } else {
            favoriteDao.insert(post.toFavoriteEntity())
            // Favouriting is the strongest taste signal — feed the engine.
            poisonEngine.record(post, com.example.mediabrowser.data.poison.PoisonEngine.InteractionType.FAVORITE)
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
        // Downloading is a strong taste signal.
        poisonEngine.record(post, com.example.mediabrowser.data.poison.PoisonEngine.InteractionType.DOWNLOAD)
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
        // Remove the actual private file from disk, not just the DB row.
        try {
            val entity = downloadDao.getById(id)
            entity?.localUri?.let { uriString ->
                val uri = android.net.Uri.parse(uriString)
                uri.path?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DeleteDownload", "Failed to delete file for $id", e)
        }
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

    // --- Tag batches ("My Poison") ---

    override fun observeTagBatches(): Flow<List<com.example.mediabrowser.domain.model.TagBatch>> =
        tagBatchDao.observeAll().map { list ->
            list.map { entity ->
                com.example.mediabrowser.domain.model.TagBatch(
                    id = entity.id,
                    name = entity.name,
                    tags = entity.tags.split(" ").filter { it.isNotBlank() },
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }

    override suspend fun createTagBatch(name: String, tags: List<String>): Long {
        val now = System.currentTimeMillis()
        return tagBatchDao.insert(
            com.example.mediabrowser.data.local.entity.TagBatchEntity(
                name = name.trim(),
                tags = tags.filter { it.isNotBlank() }.joinToString(" "),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateTagBatch(id: Long, name: String, tags: List<String>) {
        val existing = tagBatchDao.getById(id) ?: return
        tagBatchDao.update(
            existing.copy(
                name = name.trim(),
                tags = tags.filter { it.isNotBlank() }.joinToString(" "),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTagBatch(id: Long) {
        tagBatchDao.deleteById(id)
    }

    override suspend fun replaceAllTagBatches(
        batches: List<com.example.mediabrowser.domain.model.TagBatch>
    ) {
        tagBatchDao.clearAll()
        batches.forEach { batch ->
            tagBatchDao.insert(
                com.example.mediabrowser.data.local.entity.TagBatchEntity(
                    name = batch.name.trim(),
                    tags = batch.tags.filter { it.isNotBlank() }.joinToString(" "),
                    createdAt = batch.createdAt,
                    updatedAt = batch.updatedAt
                )
            )
        }
    }
}