package com.example.mediabrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.withContext
import coil.request.videoFrameMillis
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory

/**
 * Full-screen, continuous vertical scroll feed (manhwa/webtoon-style) used
 * everywhere a post is opened. Wraps [FeedScreenContent] in its own [Dialog].
 *
 * Use this for top-level call sites (Home, Search, Favorites, etc). If you
 * need to show the same feed UI from somewhere that's already inside a
 * Dialog (e.g. the inline search-results screen), use [FeedScreenContent]
 * directly instead — nesting a second Dialog inside the first causes a
 * visible flash and can fail to render properly on some devices.
 */
@Composable
fun PostFeedScreen(
    startIndex: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onDownload: (Post) -> Unit,
    getPostDetail: suspend (Post) -> PostDetail?,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    items: LazyPagingItems<Post>? = null,
    fixedItems: List<Post>? = null,
    detailViewModel: DetailPageViewModel = hiltViewModel()
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        FeedScreenContent(
            startIndex = startIndex,
            onDismiss = onDismiss,
            onToggleFavorite = onToggleFavorite,
            onDownload = onDownload,
            getPostDetail = getPostDetail,
            onNavigateToArtist = onNavigateToArtist,
            items = items,
            fixedItems = fixedItems,
            detailViewModel = detailViewModel
        )
    }
}

/**
 * The actual feed UI (scrollable post list, expandable inline details,
 * floating search pill bar, full-screen search takeover) with NO Dialog
 * wrapper — safe to embed inside an already-open Dialog, e.g. from search
 * results, without stacking a second window.
 */
@Composable
fun FeedScreenContent(
    startIndex: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onDownload: (Post) -> Unit,
    getPostDetail: suspend (Post) -> PostDetail?,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    items: LazyPagingItems<Post>? = null,
    fixedItems: List<Post>? = null,
    detailViewModel: DetailPageViewModel = hiltViewModel(),
    showFloatingSearchBar: Boolean = true,
    forceShowFeed: Boolean = false,
    // When non-null, ALL tag taps are routed here instead of the shared
    // DetailPageViewModel — used by the search drill-down so each level keeps
    // its own tag set and back stack instead of mutating one global search.
    onTagActionOverride: ((String, TagCategory, TagModalAction) -> Unit)? = null
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    var expandedPostId by remember(startIndex) {
        val initialPost = items?.itemSnapshotList?.getOrNull(startIndex) ?: fixedItems?.getOrNull(startIndex)
        mutableStateOf(initialPost?.id)
    }

    val itemCount = items?.itemCount ?: fixedItems?.size ?: 0

    val isSearchActiveRaw by detailViewModel.isSearchActive.collectAsState()
    val isSearchActive = isSearchActiveRaw && !forceShowFeed
    val searchTags by detailViewModel.searchTags.collectAsState()
    val navigateToArtist by detailViewModel.navigateToArtist.collectAsState()

    LaunchedEffect(navigateToArtist) {
        navigateToArtist?.let { profile ->
            onNavigateToArtist(profile)
            detailViewModel.clearArtistNavigation()
        }
    }

    val onTagAction: (String, TagCategory, TagModalAction) -> Unit = onTagActionOverride ?: { name, category, action ->
        when (action) {
            TagModalAction.OPEN_IN_NEW_TAB -> {
                detailViewModel.openTagInline(name, category)
                if (forceShowFeed) onDismiss()
            }
            TagModalAction.ADD_TO_SEARCH -> {
                detailViewModel.addTag(name)
                if (forceShowFeed) onDismiss()
            }
            TagModalAction.TOGGLE_FAVORITE -> detailViewModel.toggleTagOrArtistFavorite(name, category)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isSearchActive) {
            InlineFeedSearch(
                viewModel = detailViewModel,
                onClose = { detailViewModel.reset() },
                onNavigateToArtist = onNavigateToArtist
            )
        } else {
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // Bottom padding so the LAST detail section (Meta) can scroll fully
            // clear of the system navigation bar instead of sitting half-hidden
            // under it in this edge-to-edge dialog.
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = navBarBottom + 48.dp)
            ) {
                items(
                    count = itemCount,
                    key = { index ->
                        // The API can return duplicate post IDs; keying by id alone
                        // crashes the list ("Key was already used"). Prefix with the
                        // index so every key is unique regardless of duplicates.
                        val post = items?.itemSnapshotList?.getOrNull(index) ?: fixedItems?.getOrNull(index)
                        "$index|${post?.id ?: index}"
                    }
                ) { index ->
                    val post = items?.itemSnapshotList?.getOrNull(index) ?: fixedItems?.getOrNull(index)

                    if (post != null) {
                        FeedItem(
                            post = post,
                            isExpanded = expandedPostId == post.id,
                            onToggleFavorite = { onToggleFavorite(post) },
                            onDownload = { onDownload(post) },
                            onToggleDetails = {
                                expandedPostId = if (expandedPostId == post.id) null else post.id
                            },
                            getPostDetail = getPostDetail,
                            onTagAction = onTagAction,
                            getPostDetailInstant = { detailViewModel.getPostDetailInstant(it) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Floating pinned bar: shows the tags you've added so far, while
            // the post details underneath stay fully visible and scrollable,
            // so you can keep tapping more tags from the same post. Only
            // tapping the arrow here actually submits and takes over the
            // screen with results. Suppressed when this content is nested
            // inside another feed's own search-results view, since only one
            // copy of the bar should ever be visible for the shared session.
            if (showFloatingSearchBar && searchTags.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                ) {
                    FloatingSearchPillBar(
                        searchTags = searchTags,
                        onTagRemoved = { tag -> detailViewModel.removeTag(tag) },
                        onSubmit = { detailViewModel.submitSearch() }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineFeedSearch(
    viewModel: DetailPageViewModel,
    onClose: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C0E))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close search", tint = Color.White)
            }
        }

        FeedInlineSearchBody(viewModel = viewModel, onNavigateToArtist = onNavigateToArtist)
    }
}

@Composable
private fun FeedItem(
    post: Post,
    isExpanded: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onToggleDetails: () -> Unit,
    getPostDetail: suspend (Post) -> PostDetail?,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit,
    getPostDetailInstant: (suspend (Post) -> PostDetail?)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val aspectRatio = if (post.height > 0) post.width.toFloat() / post.height.toFloat() else 1f

        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio.coerceIn(0.4f, 2.2f))) {
            when (post.fileType) {
                MediaType.VIDEO -> {
                    // Show only a thumbnail in the feed — NO ExoPlayer. Creating a
                    // player per video while scrolling exhausts the device's
                    // decoders and crashes. Tapping opens a VideoModal that spins up
                    // exactly one player, and closing it tears that player down.
                    var showVideoModal by remember { mutableStateOf(false) }

                    // Prefer a real thumbnail image if the post has one (browsing
                    // case). Downloaded videos have only the local .mp4 and no image
                    // thumbnail, so fall back to a dark card behind the play button.
                    val thumbCandidate = post.thumbnailUrl
                        .ifBlank { post.previewUrl }
                        .ifBlank { post.sampleUrl }
                    val hasImageThumb = thumbCandidate.isNotBlank() &&
                        !thumbCandidate.endsWith(".mp4", ignoreCase = true) &&
                        !thumbCandidate.endsWith(".webm", ignoreCase = true) &&
                        !thumbCandidate.startsWith("file://")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF15171A))
                            .clickable { showVideoModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasImageThumb) {
                            AsyncImage(
                                model = thumbCandidate,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // No image thumbnail (downloaded video). Extract a frame
                            // from the video file with MediaMetadataRetriever — the
                            // same reliable, dependency-free method used in Downloads.
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            val src = post.fileUrl
                            var frame by remember(src) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(src) {
                                frame = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val retriever = android.media.MediaMetadataRetriever()
                                    try {
                                        if (src.startsWith("file://") || src.startsWith("/")) {
                                            retriever.setDataSource(android.net.Uri.parse(src).path)
                                        } else {
                                            retriever.setDataSource(src, HashMap())
                                        }
                                        retriever.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                            ?: retriever.frameAtTime
                                    } catch (e: Exception) {
                                        null
                                    } finally {
                                        try { retriever.release() } catch (_: Exception) {}
                                    }
                                }
                            }
                            val bmp = frame
                            if (bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Play video",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    if (showVideoModal) {
                        VideoModal(
                            videoUrl = post.fileUrl,
                            onDismiss = { showVideoModal = false },
                            isFavorite = post.isFavorite,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
                MediaType.IMAGE, MediaType.GIF -> AsyncImage(
                    model = post.fileUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (post.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (post.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onToggleDetails)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Details",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Hide details" else "Show details",
                    tint = Color.White,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            InlineDetailLoader(post = post, getPostDetail = getPostDetail, onTagAction = onTagAction, getPostDetailInstant = getPostDetailInstant)
        }
    }
}

@Composable
private fun InlineDetailLoader(
    post: Post,
    getPostDetail: suspend (Post) -> PostDetail?,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit,
    getPostDetailInstant: (suspend (Post) -> PostDetail?)? = null
) {
    var detail by remember(post.id) { mutableStateOf<PostDetail?>(null) }

    LaunchedEffect(post.id) {
        // Phase 1: paint immediately from the local tag cache / heuristics —
        // no waiting on the network just to show the sections.
        if (getPostDetailInstant != null && detail == null) {
            detail = runCatching { getPostDetailInstant(post) }.getOrNull()
        }
        // Phase 2: swap in the fully-resolved detail (real artist/character/
        // series categories from the single batch lookup) when it lands.
        getPostDetail(post)?.let { detail = it }
    }

    detail?.let {
        InlineDetailContent(detail = it, onTagAction = onTagAction)
    }
}