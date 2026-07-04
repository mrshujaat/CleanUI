package com.example.mediabrowser.ui.search

import kotlinx.coroutines.flow.combine
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    // Uses TextFieldValue (not plain String) so cursor/selection position survives
    // recomposition correctly while suggestions are loading in the background.
    private val _queryText = MutableStateFlow(TextFieldValue(""))
    val queryText: StateFlow<TextFieldValue> = _queryText.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TagSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TagSuggestion>> = _suggestions.asStateFlow()

    // True only after the user explicitly submits (taps the arrow), and reset
    // back to false whenever there are no tags left — so clearing all tags
    // always returns to the default search screen.
    private val _hasSubmitted = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = combine(_selectedTags, _hasSubmitted) { tags, submitted ->
        tags.isNotEmpty() && submitted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            _queryText
                .map { it.text }
                .debounce(150)
                .distinctUntilChanged()
                // The "current term" is everything after the last comma (commas
                // separate tags). Spaces within it are converted to underscores so
                // typing "naruto uzumaki" matches the API tag "naruto_uzumaki".
                .map { it.substringAfterLast(',').trim().replace(' ', '_') }
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { term -> fetchSuggestions(term) }
        }
    }

    private fun fetchSuggestions(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            repository.searchTags(query)
                .onSuccess { _suggestions.value = it }
                .onFailure { _suggestions.value = emptyList() }
        }
    }

    fun onQueryChanged(newValue: TextFieldValue) {
        val oldText = _queryText.value.text

        // Backspace on an already-empty field removes the last committed tag, so the
        // user can delete pills with the keyboard like a normal chip input.
        if (newValue.text.isEmpty() && oldText.isEmpty() && _selectedTags.value.isNotEmpty()) {
            _selectedTags.value = _selectedTags.value.dropLast(1)
            return
        }

        // A COMMA commits the text before it as a tag, stored EXACTLY as typed —
        // "naruto_uzumaki" stays with the underscore, "naruto uzumaki" stays with
        // the space. No conversion either way.
        if (newValue.text.contains(',')) {
            val parts = newValue.text.split(',')
            parts.dropLast(1).forEach { raw ->
                val tagName = raw.trim()
                if (tagName.isNotEmpty() && tagName !in _selectedTags.value) {
                    _selectedTags.value = _selectedTags.value + tagName
                }
            }
            val remainder = parts.last()
            _queryText.value = TextFieldValue(remainder, selection = TextRange(remainder.length))
            if (remainder.isBlank()) _suggestions.value = emptyList()
            return
        }

        _queryText.value = newValue
        if (newValue.text.isBlank()) _suggestions.value = emptyList()
    }

    fun onTagSelected(tag: TagSuggestion) {
        if (tag.name !in _selectedTags.value) {
            _selectedTags.value = _selectedTags.value + tag.name
        }
        _queryText.value = TextFieldValue("")
        _suggestions.value = emptyList()
    }

    fun addTagDirectly(tagName: String) {
        if (tagName !in _selectedTags.value) {
            _selectedTags.value = _selectedTags.value + tagName
        }
    }

    fun onTagRemoved(tag: String) {
    _selectedTags.value = _selectedTags.value - tag
    if (_selectedTags.value.isEmpty()) {
        _hasSubmitted.value = false
    }
}

    fun onSearchSubmit() {
    if (_selectedTags.value.isNotEmpty()) {
        _hasSubmitted.value = true
        // Searches are an explicit taste signal — feed them to the engine.
        val tags = _selectedTags.value
        viewModelScope.launch { repository.recordSearch(tags) }
    }
}

    /** Record a deliberate view of a post (opening it in the results feed). */
    fun recordView(post: Post) {
        viewModelScope.launch { repository.recordPostView(post) }
    }

    fun clearSearch() {
    _selectedTags.value = emptyList()
    _queryText.value = TextFieldValue("")
    _suggestions.value = emptyList()
    _hasSubmitted.value = false
}

    /**
     * Returns from the results view back to tag editing WITHOUT discarding the
     * selected tags. Used by the error screen's "Back" button so a failed search
     * doesn't strand the user with only a Retry option.
     */
    fun backToTagEditing() {
        _hasSubmitted.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = _selectedTags
        // rule34 separates multiple tags with spaces and uses underscores WITHIN a
        // tag. A tag may be displayed with a space ("naruto uzumaki"), so convert
        // each tag's internal spaces to underscores before joining with spaces.
        .map { tags -> tags.joinToString(" ") { it.trim().replace(' ', '_') } }
        .flatMapLatest { tagQuery ->
            if (tagQuery.isBlank()) flowOf(PagingData.empty()) else repository.getPostsPaged(tags = tagQuery)
        }
        .cachedIn(viewModelScope)

    /** Index in [searchResults] the user tapped, or null when the feed view is closed. */
    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    fun openFeedAt(index: Int) {
        _openFeedIndex.value = index
    }

    fun closeFeed() {
        _openFeedIndex.value = null
    }

    fun toggleFavorite(post: Post) {
        viewModelScope.launch { repository.toggleFavorite(post) }
    }

    fun downloadPost(post: Post) {
        viewModelScope.launch { repository.enqueueDownload(post) }
    }

    suspend fun getPostDetail(post: Post): PostDetail? =
        repository.getPostDetailsFromPost(post).getOrNull()

    /** Save the current search tags as a named batch ("My Poison"). */
    fun saveCurrentTagsAsBatch(name: String) {
        val tags = _selectedTags.value
        if (name.isBlank() || tags.isEmpty()) return
        viewModelScope.launch { repository.createTagBatch(name, tags) }
    }
}