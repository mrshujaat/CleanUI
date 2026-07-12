package com.example.mediabrowser.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.TagCategory

/**
 * The nested search drill-down opened when a post is tapped in Search results.
 *
 * Flow: single post (HD, details expanded) → add tags from its details to the
 * floating pill bar (previous search tags are pre-seeded) → Go runs a NEW
 * search level → tapping any result opens that single post the same way →
 * repeat indefinitely. Every level keeps its own tag set and its own cached
 * results, and back (system back or the X/arrow) pops exactly one level, all
 * the way back to the original search grid.
 *
 * Hosted in ONE Dialog — the levels stack as overlays inside it, so no nested
 * Dialog flashing.
 */
@Composable
fun SearchDrillDownDialog(
    post: Post,
    baseTags: List<String>,
    onDismiss: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: DetailPageViewModel = hiltViewModel()
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Back presses are handled level-by-level by the BackHandlers inside —
        // letting the Dialog also consume back would dismiss the whole stack.
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        PostDrillDown(
            post = post,
            baseTags = baseTags,
            onClose = onDismiss,
            onCloseAll = onDismiss,
            onNavigateToArtist = onNavigateToArtist,
            viewModel = viewModel
        )
    }
}

/**
 * One post shown alone in HD with its details expanded. Tag actions:
 *  - "Add to search" → appends to this view's pill bar (seeded with [baseTags]).
 *  - "Open in new tab" → non-artist tags start a fresh child search of just
 *    that tag; artist tags close the whole stack and open the artist page.
 *  - Pill bar Go → child search level with the accumulated tags.
 */
@Composable
fun PostDrillDown(
    post: Post,
    baseTags: List<String>,
    onClose: () -> Unit,
    onCloseAll: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: DetailPageViewModel
) {
    // Tags accumulated for the NEXT search: previous search's tags stay in.
    var pendingTags by remember(post.id) { mutableStateOf(baseTags) }
    // Non-null → a child search level is stacked on top of this post.
    var childTags by remember(post.id) { mutableStateOf<List<String>?>(null) }

    val onTagAction: (String, TagCategory, TagModalAction) -> Unit = { name, category, action ->
        when (action) {
            TagModalAction.OPEN_IN_NEW_TAB -> {
                if (category == TagCategory.ARTIST) {
                    onCloseAll()
                    onNavigateToArtist(
                        ArtistProfile(
                            artistId = name,
                            displayName = name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            postQuery = name
                        )
                    )
                } else {
                    viewModel.recordSearch(listOf(name))
                    childTags = listOf(name)
                }
            }
            TagModalAction.ADD_TO_SEARCH -> {
                if (name !in pendingTags) pendingTags = pendingTags + name
            }
            TagModalAction.TOGGLE_FAVORITE -> viewModel.toggleTagOrArtistFavorite(name, category)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Single-post "feed" of exactly one item: HD image, details expanded by
        // default, favourite/download row — all reused from the feed item UI.
        FeedScreenContent(
            startIndex = 0,
            onDismiss = onClose,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDownload = { viewModel.downloadPost(it) },
            getPostDetail = { viewModel.getPostDetail(it) },
            onNavigateToArtist = onNavigateToArtist,
            fixedItems = listOf(post),
            detailViewModel = viewModel,
            showFloatingSearchBar = false,   // we render our own, per-level bar below
            forceShowFeed = true,            // never let the shared search flag take over
            onTagActionOverride = onTagAction
        )

        if (pendingTags.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
            ) {
                FloatingSearchPillBar(
                    searchTags = pendingTags,
                    onTagRemoved = { tag -> pendingTags = pendingTags - tag },
                    onSubmit = {
                        if (pendingTags.isNotEmpty()) {
                            viewModel.recordSearch(pendingTags)
                            childTags = pendingTags
                        }
                    }
                )
            }
        }

        // Back pops one level: close the child search if open, else this post.
        BackHandler { if (childTags != null) childTags = null else onClose() }

        childTags?.let { tags ->
            DrillSearchLevel(
                initialTags = tags,
                onClose = { childTags = null },
                onCloseAll = onCloseAll,
                onNavigateToArtist = onNavigateToArtist,
                viewModel = viewModel
            )
        }
    }
}

/**
 * One search-results level in the drill-down stack: its tag pills (removable —
 * removing re-runs this level's search), its own cached paged grid, and a
 * back arrow that pops just this level. Tapping a result opens [PostDrillDown]
 * for that post, seeded with this level's tags — recursion gives unlimited depth.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrillSearchLevel(
    initialTags: List<String>,
    onClose: () -> Unit,
    onCloseAll: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: DetailPageViewModel
) {
    var levelTags by remember { mutableStateOf(initialTags) }
    var openPost by remember { mutableStateOf<Post?>(null) }

    val query = levelTags.joinToString(" ") { it.trim().replace(' ', '_') }
    // Each level remembers its own flow per query — going back re-shows this
    // level's already-cached results instead of refetching a shared search.
    val resultsFlow = remember(query) { viewModel.resultsFor(query) }
    val results = resultsFlow.collectAsLazyPagingItems()
    val settings by viewModel.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back one search", tint = Color.White)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp)
                ) {
                    levelTags.forEach { tag ->
                        LevelTagPill(
                            tag = tag,
                            onRemove = {
                                levelTags = levelTags - tag
                                if (levelTags.isEmpty()) onClose()
                            }
                        )
                    }
                }
                IconButton(onClick = onCloseAll) {
                    Icon(Icons.Filled.Close, contentDescription = "Close all", tint = Color.White)
                }
            }

            PagedPostGrid(
                items = results,
                columns = settings.gridColumns.coerceIn(1, 4),
                cornerRadiusDp = settings.cardCornerRadiusDp,
                layoutStyle = settings.homeLayoutStyle,
                imageQuality = settings.imageQuality,
                onPostClick = { post ->
                    viewModel.recordView(post)
                    openPost = post
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDownload = { viewModel.downloadPost(it) },
                emptyContent = {
                    Text("No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
                },
                onBack = onClose,
                modifier = Modifier.fillMaxSize()
            )
        }

        BackHandler { if (openPost != null) openPost = null else onClose() }

        openPost?.let { post ->
            PostDrillDown(
                post = post,
                baseTags = levelTags,
                onClose = { openPost = null },
                onCloseAll = onCloseAll,
                onNavigateToArtist = onNavigateToArtist,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun LevelTagPill(tag: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFF1A1D1F), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = tag.replace('_', ' '),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove $tag",
            tint = Color.White,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}
