package com.example.mediabrowser.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.HomeCategory
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    /** Builds (and caches, per-instance) the paged flow for whichever category is shown. */
    private var cachedFlow: Flow<PagingData<Post>>? = null
    private var cachedForCategory: HomeCategory? = null

    fun posts(category: HomeCategory): Flow<PagingData<Post>> {
        if (cachedForCategory != category) {
            cachedFlow = repository.getPostsPaged(tags = category.tagQuery).cachedIn(viewModelScope)
            cachedForCategory = category
        }
        return cachedFlow!!
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