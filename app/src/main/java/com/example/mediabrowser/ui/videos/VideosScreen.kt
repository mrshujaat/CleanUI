package com.example.mediabrowser.ui.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.ui.components.DetailPageViewModel
import com.example.mediabrowser.ui.components.InlineDetailContent
import com.example.mediabrowser.ui.components.TagModalAction
import com.example.mediabrowser.ui.components.VideoPlayer

/**
 * Doom-scroll video feed: one full-screen video per page, swipe vertically.
 * The neighboring page's player is composed off-screen and PREPARED (paused),
 * so a swipe flips playWhenReady on an already-buffered player — that's what
 * makes page changes feel TikTok-instant instead of showing a spinner.
 */
@Composable
fun VideosScreen(
    onNavigateToArtist: (ArtistProfile) -> Unit,
    startPost: Post? = null,
    onStartPostConsumed: () -> Unit = {},
    viewModel: VideosViewModel = hiltViewModel(),
    detailViewModel: DetailPageViewModel = hiltViewModel()
) {
    val items = viewModel.videosFeed.collectAsLazyPagingItems()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val context = LocalContext.current
    var infoPost by remember { mutableStateOf<Post?>(null) }
    val pagerState = rememberPagerState(pageCount = { items.itemCount })

    // If we were opened from a specific Home tile, scroll to that post as soon
    // as it appears in the paged feed. remember(startPost?.id) resets the guard
    // if the user opens another Home tile later.
    var startTargetHandled by remember(startPost?.id) { mutableStateOf(false) }
    LaunchedEffect(items.itemCount, startPost?.id) {
        val target = startPost ?: return@LaunchedEffect
        if (startTargetHandled) return@LaunchedEffect
        for (i in 0 until items.itemCount) {
            if (items.peek(i)?.id == target.id) {
                pagerState.scrollToPage(i)
                startTargetHandled = true
                onStartPostConsumed()
                break
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (items.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (items.loadState.refresh is androidx.paging.LoadState.Loading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        "No videos found on this site.",
                        color = Color(0xFF8A8D91),
                        fontSize = 15.sp
                    )
                }
            }
        } else {

            LaunchedEffect(pagerState.settledPage, items.itemCount) {
                if (items.itemCount > 0 && pagerState.settledPage < items.itemCount) {
                    items.peek(pagerState.settledPage)?.let { viewModel.recordView(it) }
                }
            }

            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { index -> items.peek(index)?.id ?: index },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = items[page]
                if (post == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    val isCurrent = pagerState.settledPage == page
                    val isFav = post.id in favoriteIds

                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        // showControls = true gives us the scrubber + mute/volume
                        // toggle at the bottom of the player, and tap-to-play/pause.
                        VideoPlayer(
                            videoUrl = post.fileUrl,
                            autoPlay = isCurrent,
                            loop = true,
                            startMuted = false,
                            showControls = true,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Right-side action rail: favourite, download, info (opens
                        // the details sheet with artist tag, character/series tags,
                        // etc. — tapping the artist chip opens their page).
                        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp, bottom = navBottom + 80.dp)
                        ) {
                            IconButton(onClick = { viewModel.toggleFavorite(post) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFav) "Remove favourite" else "Favourite",
                                    tint = if (isFav) Color(0xFFE53935) else Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(onClick = {
                                viewModel.downloadPost(post)
                                android.widget.Toast.makeText(context, "Download started", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = { infoPost = post }) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Details",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Bottom-left summary: score + first tags, sits above the
                        // scrubber. Tapping this whole strip also opens details,
                        // so users can access the artist page without hunting for
                        // the small (i) button.
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, end = 72.dp, bottom = navBottom + 68.dp)
                                .clickable { infoPost = post }
                        ) {
                            Text(
                                text = "★ ${post.score}  ·  tap for details & artist",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = post.tags.take(5).joinToString("  ") { it.replace('_', ' ') },
                                color = Color(0xFFB9BCC0),
                                fontSize = 12.sp,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Filter chips pinned to the top — Popular (all-time) vs Latest. Sits
        // above the video, clear of the status bar, minimal so it doesn't
        // fight the video content.
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + 8.dp)
        ) {
            VideosViewModel.VideoFilter.entries.forEach { f ->
                val selected = filter == f
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) Color(0xFFFFFFFF).copy(alpha = 0.20f)
                            else Color(0x66000000),
                            androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                        .clickable { viewModel.setFilter(f) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = f.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        infoPost?.let { post ->
            VideoDetailsSheet(
                post = post,
                detailViewModel = detailViewModel,
                onDismiss = { infoPost = null },
                onNavigateToArtist = { profile ->
                    infoPost = null
                    onNavigateToArtist(profile)
                }
            )
        }
    }
}

/**
 * Bottom-sheet-style dialog that shows the same [InlineDetailContent] used in
 * the image feed — categorized tags with a live artist chip. Tapping the
 * artist chip navigates to their page; tapping another tag adds it to search
 * (would need a nav callback; for now we just handle artist).
 */
@Composable
private fun VideoDetailsSheet(
    post: Post,
    detailViewModel: DetailPageViewModel,
    onDismiss: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit
) {
    var detail by remember(post.id) { mutableStateOf<PostDetail?>(null) }
    LaunchedEffect(post.id) {
        // Instant-cache paint, then upgrade to fully-resolved detail.
        detail = runCatching { detailViewModel.getPostDetailInstant(post) }.getOrNull()
        detailViewModel.getPostDetail(post)?.let { detail = it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(onClick = onDismiss)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .background(Color(0xFF0B0C0E))
                    .clickable(enabled = false, onClick = {})  // swallow taps
            ) {
                val d = detail
                if (d == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        InlineDetailContent(
                            detail = d,
                            onTagAction = { name, category, action ->
                                if (category == TagCategory.ARTIST && action == TagModalAction.OPEN_IN_NEW_TAB) {
                                    onNavigateToArtist(
                                        ArtistProfile(
                                            artistId = name,
                                            displayName = name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            postQuery = name
                                        )
                                    )
                                } else if (category == TagCategory.ARTIST) {
                                    // Any interaction with the artist tag → open page.
                                    onNavigateToArtist(
                                        ArtistProfile(
                                            artistId = name,
                                            displayName = name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            postQuery = name
                                        )
                                    )
                                } else if (action == TagModalAction.TOGGLE_FAVORITE) {
                                    detailViewModel.toggleTagOrArtistFavorite(name, category)
                                }
                                // Other tag actions (search-related) intentionally
                                // no-op here — the videos tab is a viewer, not a
                                // search entry point.
                            }
                        )
                    }
                }
            }
        }
    }
}

