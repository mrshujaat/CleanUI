package com.example.mediabrowser.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.FavoriteArtist
import com.example.mediabrowser.domain.model.FavoriteTag
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagBatch
import com.example.mediabrowser.domain.model.TagBatchSerializer
import com.example.mediabrowser.domain.model.TagCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoritesTab { POSTS, TAGS, ARTISTS, BATCHES }

/**
 * The five favourites sub-sections shown on the landing list (matching the
 * redesign). Characters/Series/Tags are favourite tags filtered by category;
 * MY_POISON is the saved tag batches; ARTISTS is favourite artists.
 */
enum class FavoriteSection(val title: String, val subtitle: String) {
    POSTS("Posts", "Images and videos you favourited"),
    CHARACTERS("Characters", "Your saved favourite characters"),
    SERIES("Series", "Series you liked"),
    TAGS("Tags", "A collection of your favourite tags"),
    MY_POISON("My Poision", "Your saved searches of tags"),
    ARTISTS("Artist", "Artist that you like")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    val favoritePosts: Flow<PagingData<Post>> =
        repository.getFavoritesPaged().cachedIn(viewModelScope)

    // Same favorites stream split by media type so the Posts tab can show
    // Images and Videos separately. Each is independently cached.
    val favoriteImages: Flow<PagingData<Post>> =
        repository.getFavoritesPaged()
            .map { paging -> paging.filter { it.fileType != com.example.mediabrowser.domain.model.MediaType.VIDEO } }
            .cachedIn(viewModelScope)

    val favoriteVideos: Flow<PagingData<Post>> =
        repository.getFavoritesPaged()
            .map { paging -> paging.filter { it.fileType == com.example.mediabrowser.domain.model.MediaType.VIDEO } }
            .cachedIn(viewModelScope)

    val favoriteTags: StateFlow<List<FavoriteTag>> = repository.observeFavoriteTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteArtists: StateFlow<List<FavoriteArtist>> = repository.observeFavoriteArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    // --- Inline multi-tag search within the Tags tab ---

    private val _searchTags = MutableStateFlow<List<String>>(emptyList())
    val searchTags: StateFlow<List<String>> = _searchTags.asStateFlow()

    private val _isTagSearchActive = MutableStateFlow(false)
    val isTagSearchActive: StateFlow<Boolean> = _isTagSearchActive.asStateFlow()

    val tagSearchResults: Flow<PagingData<Post>> = _searchTags
        .map { tags -> tags.joinToString(" ") }
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty()) else repository.getPostsPaged(tags = query)
        }
        .cachedIn(viewModelScope)

    fun addSearchTag(tagName: String) {
        if (tagName !in _searchTags.value) {
            _searchTags.value = _searchTags.value + tagName
        }
    }

    fun removeSearchTag(tagName: String) {
        _searchTags.value = _searchTags.value - tagName
    }

    fun submitTagSearch() {
        if (_searchTags.value.isNotEmpty()) {
            _isTagSearchActive.value = true
        }
    }

    fun clearTagSearch() {
        _searchTags.value = emptyList()
        _isTagSearchActive.value = false
    }

    // --- Feed view (shared by Posts tab and Tags-tab search results) ---

    /** Which source the open feed reads from, since Favorites has two distinct lists. */
    private val _openFeedSource = MutableStateFlow<FeedSource?>(null)
    val openFeedSource: StateFlow<FeedSource?> = _openFeedSource.asStateFlow()

    private val _openFeedIndex = MutableStateFlow(0)
    val openFeedIndex: StateFlow<Int> = _openFeedIndex.asStateFlow()

    fun openFeedAt(source: FeedSource, index: Int) {
        _openFeedSource.value = source
        _openFeedIndex.value = index
    }

    fun closeFeed() {
        _openFeedSource.value = null
    }

    fun toggleFavorite(post: Post) {
        viewModelScope.launch { repository.toggleFavorite(post) }
    }

    fun downloadPost(post: Post) {
        viewModelScope.launch { repository.enqueueDownload(post) }
    }

    suspend fun getPostDetail(post: Post): PostDetail? =
        repository.getPostDetailsFromPost(post).getOrNull()

    fun toggleTagFavorite(name: String, category: TagCategory) {
        viewModelScope.launch {
            if (category == TagCategory.ARTIST) {
                repository.toggleFavoriteArtist(artistName = name, displayName = name, postCount = 0)
            } else {
                repository.toggleFavoriteTag(tagName = name, displayName = name, category = category, postCount = 0)
            }
        }
    }

    fun removeFavoriteTag(tagName: String) {
        viewModelScope.launch {
            repository.toggleFavoriteTag(tagName, tagName, TagCategory.GENERAL, 0)
        }
    }

    fun removeFavoriteArtist(artistName: String) {
        viewModelScope.launch {
            repository.toggleFavoriteArtist(artistName, artistName, 0)
        }
    }

    // --- Tag batches ("My Poison") ---

    val tagBatches: StateFlow<List<TagBatch>> = repository.observeTagBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Create a batch from an arbitrary tag list (used by "save from search"). */
    fun createBatch(name: String, tags: List<String>) {
        if (name.isBlank() || tags.isEmpty()) return
        viewModelScope.launch { repository.createTagBatch(name, tags) }
    }

    /** Append tags to an existing batch (the "+ → Save changes" flow). */
    fun addTagsToBatch(batch: TagBatch, newTags: List<String>) {
        val merged = (batch.tags + newTags)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        viewModelScope.launch { repository.updateTagBatch(batch.id, batch.name, merged) }
    }

    fun renameBatch(batch: TagBatch, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.updateTagBatch(batch.id, newName, batch.tags) }
    }

    fun removeTagFromBatch(batch: TagBatch, tag: String) {
        viewModelScope.launch {
            repository.updateTagBatch(batch.id, batch.name, batch.tags - tag)
        }
    }

    fun deleteBatch(batch: TagBatch) {
        viewModelScope.launch { repository.deleteTagBatch(batch.id) }
    }

    /** Run a search over all tags in a batch, reusing the Tags-tab feed machinery. */
    fun searchBatch(batch: TagBatch) {
        _searchTags.value = batch.tags
        _isTagSearchActive.value = true
    }

    /** Serialize all batches to the export text format. */
    fun exportBatchesText(): String = TagBatchSerializer.export(tagBatches.value)

    /** Parse imported text and replace the current batch set. */
    fun importBatchesText(text: String) {
        val parsed = TagBatchSerializer.import(text)
        if (parsed.isEmpty()) return
        viewModelScope.launch { repository.replaceAllTagBatches(parsed) }
    }
}

enum class FeedSource { POSTS_TAB, TAGS_SEARCH }