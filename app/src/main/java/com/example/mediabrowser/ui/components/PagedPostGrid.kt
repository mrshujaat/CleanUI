package com.example.mediabrowser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.mediabrowser.domain.model.LayoutStyle
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.TagCategory

/**
 * Shared infinite-scroll grid used by Home, Search results, and Favorites.
 * Renders [Post] items via [PostGridItem] in either a masonry (aspect-ratio
 * aware columns) or simple fixed-row grid, depending on [layoutStyle].
 */
@Composable
fun PagedPostGrid(
    items: LazyPagingItems<Post>,
    columns: Int,
    cornerRadiusDp: Int,
    layoutStyle: LayoutStyle,
    imageQuality: com.example.mediabrowser.domain.model.ImageQuality =
        com.example.mediabrowser.domain.model.ImageQuality.MEDIUM,
    onPostClick: (Post) -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onDownload: (Post) -> Unit = {},
    onTagClick: (String, TagCategory) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    emptyContent: @Composable () -> Unit = {},
    onBack: (() -> Unit)? = null,
    // Reports scroll direction so callers can collapse/reveal a header above the
    // grid: true = user scrolled DOWN (hide header), false = scrolled UP (show it).
    onScrollDirectionChanged: ((scrolledDown: Boolean) -> Unit)? = null
) {
    // Hoisted scroll states so we can observe scroll direction and report it up
    // (used by Search to collapse/reveal its header). One per layout type; only
    // the active one is ever attached to a live grid.
    val staggeredState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    if (onScrollDirectionChanged != null) {
        val isMasonry = layoutStyle == LayoutStyle.MASONRY
        val index = if (isMasonry) staggeredState.firstVisibleItemIndex else gridState.firstVisibleItemIndex
        val offset = if (isMasonry) staggeredState.firstVisibleItemScrollOffset else gridState.firstVisibleItemScrollOffset
        var prevIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
        var prevOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
        androidx.compose.runtime.LaunchedEffect(index, offset) {
            val scrolledDown = when {
                index != prevIndex -> index > prevIndex
                else -> offset > prevOffset
            }
            // Ignore tiny jitters near the very top so the header doesn't flicker.
            if (index > 0 || offset > 8) onScrollDirectionChanged(scrolledDown)
            else onScrollDirectionChanged(false)
            prevIndex = index
            prevOffset = offset
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
                FullScreenError(
                    message = "Something went wrong. Pull down to retry.",
                    onRetry = { items.retry() },
                    onBack = onBack
                )
            }
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
                FullScreenLoading()
            }
            items.itemCount == 0 -> {
                // Keep the header (Popular / Top Series on Home) visible even when
                // the feed itself is empty, then show the empty message below it.
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    headerContent()
                    emptyContent()
                }
            }
            else -> {

                when (layoutStyle) {
                    LayoutStyle.MASONRY -> {
                        androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                            state = staggeredState,
                            columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 8.dp
                        ) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                headerContent()
                            }
                            items(
                                count = items.itemCount
                            ) { index ->
                                val post = items[index]
                                if (post != null) {
                                    PostGridItem(
                                        post = post,
                                        onClick = { onPostClick(post) },
                                        onToggleFavorite = { onToggleFavorite(post) },
                                        onDownload = { onDownload(post) },
                                        onTagClick = onTagClick,
                                        cornerRadiusDp = cornerRadiusDp,
                                        columns = columns,
                                        imageQuality = imageQuality
                                    )
                                }
                            }
                            if (items.loadState.append is LoadState.Loading) {
                                item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                    PagingAppendLoading()
                                }
                            }
                            if (items.loadState.append is LoadState.Error) {
                                item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                    PagingAppendError(onRetry = { items.retry() })
                                }
                            }
                        }
                    }
                    LayoutStyle.GRID -> {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            state = gridState,
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                headerContent()
                            }
                           items(
                                count = items.itemCount
                            ) { index ->
                                val post = items[index]
                                if (post != null) {
                                    PostGridItem(
                                        post = post,
                                        onClick = { onPostClick(post) },
                                        onToggleFavorite = { onToggleFavorite(post) },
                                        onDownload = { onDownload(post) },
                                        onTagClick = onTagClick,
                                        cornerRadiusDp = cornerRadiusDp,
                                        columns = columns,
                                        imageQuality = imageQuality
                                    )
                                }
                            }
                            if (items.loadState.append is LoadState.Loading) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    PagingAppendLoading()
                                }
                            }
                            if (items.loadState.append is LoadState.Error) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    PagingAppendError(onRetry = { items.retry() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}