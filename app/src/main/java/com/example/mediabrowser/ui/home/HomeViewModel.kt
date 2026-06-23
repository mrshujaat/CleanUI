package com.example.mediabrowser.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    // A refresh signal trigger stream to force-invalidate the paging stream on demand
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1)

    // FIXED: Transformed the static stream into a reactive pipeline that can gracefully 
    // re-fetch or clear cache bounds whenever a refresh event is triggered.
    val trendingFeed: Flow<PagingData<Post>> = refreshSignal
        .onStart { emit(Unit) } // Automatically load the initial page on start
        .flatMapLatest {
            repository.getPostsPaged(tags = "")
        }
        .cachedIn(viewModelScope)

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Index in [trendingFeed] the user tapped, or null when the feed view is closed. */
    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    /** Public handle allowing pull-to-refresh indicators to cleanly kick off a new data stream fetch */
    fun refreshFeed() {
        viewModelScope.launch {
            refreshSignal.emit(Unit)
        }
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