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

/** Top-level split in the Downloads screen: images vs videos. */
enum class DownloadSection { IMAGES, VIDEOS }

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val preferencesDataStore: com.example.mediabrowser.data.local.PreferencesDataStore
) : ViewModel() {

    val settings: StateFlow<com.example.mediabrowser.domain.model.AppSettings> = preferencesDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.mediabrowser.domain.model.AppSettings())

    private val _sortOrder = MutableStateFlow(DownloadSortOrder.DATE)
    val sortOrder: StateFlow<DownloadSortOrder> = _sortOrder.asStateFlow()

    private val _viewStyle = MutableStateFlow(DownloadViewStyle.LIST)
    val viewStyle: StateFlow<DownloadViewStyle> = _viewStyle.asStateFlow()

    // Images-vs-Videos split for the Downloads screen.
    private val _section = MutableStateFlow(DownloadSection.IMAGES)
    val section: StateFlow<DownloadSection> = _section.asStateFlow()

    fun setSection(section: DownloadSection) {
        _section.value = section
    }

    private val rawDownloads = repository.observeDownloads()

    // Download toasts ("Added to downloads", "Image/Video downloaded") are fired
    // directly by DownloadWorker, so they show regardless of which screen the
    // user is on. No screen-bound watcher is needed here anymore.

    // Master stream of sorted download items
    val downloads: StateFlow<List<DownloadItem>> = combine(rawDownloads, _sortOrder) { items, order ->
    val sorted = when (order) {
        DownloadSortOrder.DATE -> items.sortedByDescending { it.createdAt }
        DownloadSortOrder.SIZE -> items.sortedByDescending { it.fileSizeBytes }
    }
    android.util.Log.d("DownloadsDebug", "ids=${sorted.map { it.id }} postIds=${sorted.map { it.postId }}")
    sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // FIXED: Safely project completed posts in sync with the master sort order, keeping allocations off the UI thread
    val completedPosts: StateFlow<List<Post>> = downloads
    .map { items ->
        val completed = items.filter { it.status == DownloadStatus.COMPLETED }.map { it.toPost() }
        android.util.Log.d("DownloadsDebug", "completedPosts ids=${completed.map { it.id }}")
        completed
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Downloads filtered to the active section (images include GIFs; videos separate).
    val sectionedDownloads: StateFlow<List<DownloadItem>> =
        combine(downloads, _section) { items, section ->
            items.filter {
                when (section) {
                    DownloadSection.VIDEOS -> it.mediaType == com.example.mediabrowser.domain.model.MediaType.VIDEO
                    DownloadSection.IMAGES -> it.mediaType != com.example.mediabrowser.domain.model.MediaType.VIDEO
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Completed posts filtered to the active section, for the in-app feed viewer.
    val sectionedCompletedPosts: StateFlow<List<Post>> =
        combine(completedPosts, _section, downloads) { posts, section, items ->
            val videoIds = items
                .filter { it.mediaType == com.example.mediabrowser.domain.model.MediaType.VIDEO }
                .map { it.postId }
                .toSet()
            posts.filter { post ->
                when (section) {
                    DownloadSection.VIDEOS -> post.id in videoIds
                    DownloadSection.IMAGES -> post.id !in videoIds
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
    sampleUrl = this.localUri ?: this.fileUrl,
    fileUrl = this.localUri ?: this.fileUrl,
    fileType = this.mediaType,
    width = this.width,
    height = this.height,
    score = this.score,
    tags = this.tags,
    isFavorite = false
)