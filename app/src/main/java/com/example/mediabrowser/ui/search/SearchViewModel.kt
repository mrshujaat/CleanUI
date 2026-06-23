package com.example.mediabrowser.ui.search

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

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TagSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TagSuggestion>> = _suggestions.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            _queryText
                .debounce(250)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query -> fetchSuggestions(query) }
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

    fun onQueryChanged(text: String) {
        _queryText.value = text
        if (text.isBlank()) _suggestions.value = emptyList()
    }

    fun onTagSelected(tag: TagSuggestion) {
        if (tag.name !in _selectedTags.value) {
            _selectedTags.value = _selectedTags.value + tag.name
        }
        _queryText.value = ""
        _suggestions.value = emptyList()
    }

    fun addTagDirectly(tagName: String) {
        if (tagName !in _selectedTags.value) {
            _selectedTags.value = _selectedTags.value + tagName
        }
    }

    fun onTagRemoved(tag: String) {
        _selectedTags.value = _selectedTags.value - tag
    }

    fun onSearchSubmit() {
        if (_selectedTags.value.isNotEmpty()) {
            _isSearchActive.value = true
        }
    }

    fun clearSearch() {
        _selectedTags.value = emptyList()
        _queryText.value = ""
        _suggestions.value = emptyList()
        _isSearchActive.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = _selectedTags
        .map { tags -> tags.joinToString(" ") }
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
}