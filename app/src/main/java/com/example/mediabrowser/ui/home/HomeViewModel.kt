package com.example.mediabrowser.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.DEFAULT_TOP_SERIES
import com.example.mediabrowser.domain.model.HomeCategory
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A representative post for one "Top Series" row card, or null while loading/unavailable. */
data class TopSeriesCard(
    val category: HomeCategory.TopSeries,
    val thumbnail: Post?
)

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
    // Now also reacts to the Home feed-type setting: DEFAULT shows the trending feed,
    // POISON shows the personalized recommendation feed. flatMapLatest tears down the
    // old pager and stands up the new one when the toggle flips.
    val trendingFeed: Flow<PagingData<Post>> =
        combine(
            refreshSignal.onStart { emit(Unit) },
            preferencesDataStore.settingsFlow
                .map { it.homeFeedType }
                .distinctUntilChanged()
        ) { _, feedType -> feedType }
            .flatMapLatest { feedType ->
                when (feedType) {
                    com.example.mediabrowser.domain.model.FeedType.POISON -> repository.getPoisonFeedPaged()
                    else -> repository.getPostsPaged(tags = "")
                }
            }
            .cachedIn(viewModelScope)

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Small, non-paged preview list for the "Most Popular" home row. */
    private val _mostPopularPreview = MutableStateFlow<List<Post>>(emptyList())
    val mostPopularPreview: StateFlow<List<Post>> = _mostPopularPreview.asStateFlow()

    /** Small, non-paged preview list for the "Trending" home row. */
    private val _trendingPreview = MutableStateFlow<List<Post>>(emptyList())
    val trendingPreview: StateFlow<List<Post>> = _trendingPreview.asStateFlow()

    /** One representative thumbnail post per franchise tag, for the "Top Series" row. */
    private val _topSeriesCards = MutableStateFlow<List<TopSeriesCard>>(
        DEFAULT_TOP_SERIES.map { TopSeriesCard(category = it, thumbnail = null) }
    )
    val topSeriesCards: StateFlow<List<TopSeriesCard>> = _topSeriesCards.asStateFlow()

    /** Index in [trendingFeed] the user tapped, or null when the feed view is closed. */
    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    init {
        loadRowPreviews()
    }

    private fun loadRowPreviews() {
        viewModelScope.launch {
            _mostPopularPreview.value = repository.getPostsFlat(
                tags = HomeCategory.MostPopular.tagQuery,
                limit = 15
            )
        }
        viewModelScope.launch {
            _trendingPreview.value = repository.getPostsFlat(
                tags = HomeCategory.Trending.tagQuery,
                limit = 15
            )
        }
        viewModelScope.launch {
            DEFAULT_TOP_SERIES.forEachIndexed { index, series ->
                val thumbnail = repository.getTopPostForTag(series.tagName)
                _topSeriesCards.value = _topSeriesCards.value.toMutableList().also {
                    it[index] = TopSeriesCard(category = series, thumbnail = thumbnail)
                }
            }
        }
    }

    /** Public handle allowing pull-to-refresh indicators to cleanly kick off a new data stream fetch */
    fun refreshFeed() {
        viewModelScope.launch {
            refreshSignal.emit(Unit)
        }
    }

    fun openFeedAt(index: Int) {
        _openFeedIndex.value = index
    }

    /** Record a deliberate view of a post (opening it in the feed). */
    fun recordView(post: Post) {
        viewModelScope.launch { repository.recordPostView(post) }
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