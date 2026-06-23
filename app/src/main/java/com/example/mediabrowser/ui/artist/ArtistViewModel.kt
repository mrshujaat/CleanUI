package com.example.mediabrowser.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the dedicated Artist Page: a bio-style header plus a virtualized grid
 * of that artist's posts. Evaluates profile pipeline updates reactively.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _profile = MutableStateFlow<ArtistProfile?>(null)
    val profile: StateFlow<ArtistProfile?> = _profile.asStateFlow()

    // FIXED: Flattened the pipeline. We transform profile updates directly into paging data 
    // without nesting Flows inside a StateFlow, preventing collection dropped-linkage bugs.
    val posts: Flow<PagingData<Post>> = _profile
        .filterNotNull()
        .flatMapLatest { profile ->
            repository.getPostsPaged(tags = profile.postQuery)
        }
        .cachedIn(viewModelScope)

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Index in the artist's post feed the user tapped, or null when the feed view is closed. */
    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    fun load(profile: ArtistProfile) {
        // If this artist is already loaded, skip resetting the stream to preserve pagination cache
        if (_profile.value?.artistId == profile.artistId) return
        _profile.value = profile
    }

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