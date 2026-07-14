package com.example.mediabrowser.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TikTok-style vertical video feed. Sources the booru's `video` meta-tag feed
 * (works across all supported sites) and filters out anything that isn't
 * actually a video file — some posts carry the tag but are stills.
 *
 * Favorites/downloads go through the same repository methods as everywhere
 * else, so they feed the Poison engine identically: liking or downloading a
 * video here shapes the Poison feed's video content too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideosViewModel @Inject constructor(
    private val repository: MediaRepository,
    preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    /** Videos-tab-only sort choice. Not persisted — resets on process death. */
    enum class VideoFilter(val displayName: String, val tagQuery: String) {
        LATEST("Latest", "video"),
        POPULAR("Popular", "video sort:score")
    }

    private val _filter = kotlinx.coroutines.flow.MutableStateFlow(VideoFilter.LATEST)
    val filter: StateFlow<VideoFilter> = _filter

    fun setFilter(f: VideoFilter) { _filter.value = f }

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Re-created whenever the active booru site OR the filter changes. */
    val videosFeed: Flow<PagingData<Post>> = kotlinx.coroutines.flow.combine(
        preferencesDataStore.settingsFlow.map { it.apiProviderName }.distinctUntilChanged(),
        _filter
    ) { _, f -> f }
        .flatMapLatest { f -> repository.getPostsPaged(tags = f.tagQuery) }
        .map { pagingData -> pagingData.filter { it.fileType == MediaType.VIDEO } }
        .cachedIn(viewModelScope)

    /** Live favourite ids so hearts stay in sync as you toggle mid-scroll. */
    val favoriteIds: StateFlow<Set<Long>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(post: Post) {
        viewModelScope.launch { repository.toggleFavorite(post) }
    }

    fun downloadPost(post: Post) {
        viewModelScope.launch { repository.enqueueDownload(post) }
    }

    /** Watching a video page counts as a view signal for the Poison engine. */
    fun recordView(post: Post) {
        viewModelScope.launch { repository.recordPostView(post) }
    }
}
