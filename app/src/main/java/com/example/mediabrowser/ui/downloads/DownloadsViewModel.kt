package com.example.mediabrowser.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediabrowser.data.mapper.toPostDetail
import com.example.mediabrowser.data.repository.MediaRepository
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.DownloadStatus
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DownloadSortOrder { DATE, SIZE }
enum class DownloadViewStyle { LIST, GRID }

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(DownloadSortOrder.DATE)
    val sortOrder: StateFlow<DownloadSortOrder> = _sortOrder.asStateFlow()

    private val _viewStyle = MutableStateFlow(DownloadViewStyle.LIST)
    val viewStyle: StateFlow<DownloadViewStyle> = _viewStyle.asStateFlow()

    private val rawDownloads = repository.observeDownloads()

    // Master stream of sorted download items
    val downloads: StateFlow<List<DownloadItem>> = combine(rawDownloads, _sortOrder) { items, order ->
        when (order) {
            DownloadSortOrder.DATE -> items.sortedByDescending { it.createdAt }
            DownloadSortOrder.SIZE -> items.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // FIXED: Safely project completed posts in sync with the master sort order, keeping allocations off the UI thread
    val completedPosts: StateFlow<List<Post>> = downloads
        .map { items ->
            items.filter { it.status == DownloadStatus.COMPLETED }.map { it.toPost() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Index in [completedPosts] the user tapped, or null when closed. */
    private val _openFeedIndex = MutableStateFlow<Int?>(null)
    val openFeedIndex: StateFlow<Int?> = _openFeedIndex.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun setSortOrder(order: DownloadSortOrder) {
        _sortOrder.value = order
    }

    fun setViewStyle(style: DownloadViewStyle) {
        _viewStyle.value = style
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch { repository.deleteDownload(id) }
    }

    /** * FIXED: This accepts the absolute Post model instance clicked in the UI, 
     * resolving its safe index position strictly within the completed collection boundary.
     */
    fun openDownloadPost(post: Post) {
        val verifiedIndex = completedPosts.value.indexOfFirst { it.id == post.id }
        if (verifiedIndex != -1) {
            _openFeedIndex.value = verifiedIndex
        }
    }

    fun closeFeed() {
        _openFeedIndex.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun getPostDetail(post: Post): PostDetail? =
        downloads.value.firstOrNull { it.postId == post.id && it.status == DownloadStatus.COMPLETED }?.toPostDetail()
}

/** * FIXED: Removed 'private' so this mapping extension can be called from DownloadsScreen.kt
 */
fun DownloadItem.toPost(): Post = Post(
    id = this.postId,
    thumbnailUrl = this.localUri ?: this.fileUrl,
    previewUrl = this.localUri ?: this.fileUrl,
    fileUrl = this.localUri ?: this.fileUrl,
    fileType = this.mediaType,
    width = this.width,
    height = this.height,
    score = this.score,
    tags = this.tags,
    isFavorite = false
)