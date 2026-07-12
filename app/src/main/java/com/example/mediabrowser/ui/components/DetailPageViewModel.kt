package com.example.mediabrowser.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.Post
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the inline search bar and tag/artist favoriting on the Detail Page.
 * Isolated to prevent query pollution on host feeds.
 *
 * Tapping an ARTIST tag's "Open in new tab" does NOT add it to inline
 * search — it instead emits a one-shot [navigateToArtist] event, since
 * artists have their own dedicated page (with their own posts + search)
 * rather than being just another search tag.
 *
 * [queryText] backs an editable text field that stays available even on
 * the results screen (after "Open in new tab"), so additional tags can be
 * typed and added — typing a space commits the current text as a new pill,
 * same convention as the main Search tab.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailPageViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _searchTags = MutableStateFlow<List<String>>(emptyList())
    val searchTags: StateFlow<List<String>> = _searchTags.asStateFlow()

    private val _queryText = MutableStateFlow(TextFieldValue(""))
    val queryText: StateFlow<TextFieldValue> = _queryText.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // One-shot navigation event: when non-null, the screen should navigate
    // to this artist's page, then call clearArtistNavigation().
    private val _navigateToArtist = MutableStateFlow<ArtistProfile?>(null)
    val navigateToArtist: StateFlow<ArtistProfile?> = _navigateToArtist.asStateFlow()

    private val _searchTrigger = MutableStateFlow("")

    val searchResults: Flow<PagingData<Post>> = _searchTrigger
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                repository.getPostsPaged(tags = query)
            }
        }
        .cachedIn(viewModelScope)

    fun addTag(tagName: String) {
        if (tagName !in _searchTags.value) {
            _searchTags.value = _searchTags.value + tagName
        }
    }

    fun removeTag(tagName: String) {
        _searchTags.value = _searchTags.value - tagName
        if (_searchTags.value.isEmpty()) {
            reset()
        } else if (_isSearchActive.value) {
            // Stay on the results screen but re-run the search without the removed tag.
            _searchTrigger.value = _searchTags.value.joinToString(" ")
        }
    }

    /**
     * Handles typing in the editable query field. A space commits whatever
     * was typed so far as a new tag pill; if search is already active, the
     * results are immediately re-fetched to include it.
     */
    fun onQueryChanged(newValue: TextFieldValue) {
        if (newValue.text.contains(' ')) {
            val tagName = newValue.text.substringBefore(' ').trim()
            if (tagName.isNotEmpty()) {
                addTag(tagName)
                if (_isSearchActive.value) {
                    _searchTrigger.value = _searchTags.value.joinToString(" ")
                }
            }
            val remainder = newValue.text.substringAfter(' ')
            _queryText.value = TextFieldValue(remainder, selection = TextRange(remainder.length))
            return
        }
        _queryText.value = newValue
    }

    /**
     * "Open in new tab": for an ARTIST tag, this navigates to that artist's
     * dedicated page instead of adding it to inline search. For every other
     * category, it behaves as before — add the tag and immediately search.
     */
    fun openTagInline(tagName: String, category: TagCategory = TagCategory.GENERAL) {
        android.util.Log.d("ClickTrace", "openTagInline called with tagName=$tagName category=$category")
        if (category == TagCategory.ARTIST) {
            _navigateToArtist.value = ArtistProfile(
                artistId = tagName,
                displayName = tagName.replace('_', ' ').replaceFirstChar { it.uppercase() },
                postQuery = tagName
            )
            return
        }
        addTag(tagName)
        _isSearchActive.value = true
        _searchTrigger.value = _searchTags.value.joinToString(" ")
    }

    fun clearArtistNavigation() {
        _navigateToArtist.value = null
    }

    fun submitSearch() {
        if (_queryText.value.text.isNotBlank()) {
            addTag(_queryText.value.text.trim())
            _queryText.value = TextFieldValue("")
        }
        if (_searchTags.value.isNotEmpty()) {
            _isSearchActive.value = true
            _searchTrigger.value = _searchTags.value.joinToString(" ")
        }
    }

    fun reset() {
        _searchTags.value = emptyList()
        _queryText.value = TextFieldValue("")
        _isSearchActive.value = false
        _searchTrigger.value = ""
    }

    fun toggleFavorite(post: Post) {
        viewModelScope.launch { repository.toggleFavorite(post) }
    }

    fun toggleTagOrArtistFavorite(name: String, category: TagCategory) {
        viewModelScope.launch {
            if (category == TagCategory.ARTIST) {
                repository.toggleFavoriteArtist(artistName = name, displayName = name, postCount = 0)
            } else {
                repository.toggleFavoriteTag(tagName = name, displayName = name, category = category, postCount = 0)
            }
        }
    }

    suspend fun getPostDetail(post: Post): com.example.mediabrowser.domain.model.PostDetail? =
        repository.getPostDetailsFromPost(post).getOrNull()

    fun downloadPost(post: Post) {
        viewModelScope.launch { repository.enqueueDownload(post) }
    }

    /** Instant (cache/heuristic) detail — painted before the network resolves. */
    suspend fun getPostDetailInstant(post: Post): com.example.mediabrowser.domain.model.PostDetail = repository.getPostDetailInstant(post)

    // --- Search drill-down support ---

    /**
     * A standalone paged result flow for one drill-down level. Each level in the
     * nested search stack owns its own flow (remembered per query in the UI), so
     * going back a level lands on that level's still-cached results instead of a
     * single shared, overwritten search.
     */
    fun resultsFor(query: String): Flow<PagingData<Post>> =
        repository.getPostsPaged(tags = query).cachedIn(viewModelScope)

    /** Feed a submitted drill-down search to the Poison engine as a taste signal. */
    fun recordSearch(tags: List<String>) {
        viewModelScope.launch { repository.recordSearch(tags) }
    }

    /** Record a deliberate view of a post opened from a drill-down grid. */
    fun recordView(post: Post) {
        viewModelScope.launch { repository.recordPostView(post) }
    }
}