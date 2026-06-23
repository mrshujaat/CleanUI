package com.example.mediabrowser.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.TagCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the inline search bar and tag/artist favoriting on the Detail Page.
 * Isolated to prevent query pollution on host feeds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailPageViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _searchTags = MutableStateFlow<List<String>>(emptyList())
    val searchTags: StateFlow<List<String>> = _searchTags.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // FIXED: An isolated trigger stream that holds the string query sent to the network layer
    private val _searchTrigger = MutableStateFlow("")

    // The paging pipeline now only re-runs when the explicit trigger string changes
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
        
        // If the user clears out all tags, auto-reset search back to the details layout
        if (_searchTags.value.isEmpty()) {
            reset()
        }
    }

    /** "Open in new tab": adds the tag AND immediately activates inline search. */
    fun openTagInline(tagName: String) {
        addTag(tagName)
        _isSearchActive.value = true
        // Fire search immediately
        _searchTrigger.value = _searchTags.value.joinToString(" ")
    }

    fun submitSearch() {
        if (_searchTags.value.isNotEmpty()) {
            _isSearchActive.value = true
            // FIXED: Only commit the text query out to the pager pipeline when requested
            _searchTrigger.value = _searchTags.value.joinToString(" ")
        }
    }

    fun reset() {
        _searchTags.value = emptyList()
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
}