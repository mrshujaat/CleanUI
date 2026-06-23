package com.example.mediabrowser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
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
    onPostClick: (Post) -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onTagClick: (String, TagCategory) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    emptyContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
                FullScreenError(
                    message = "Something went wrong. Pull down to retry.",
                    onRetry = { items.retry() }
                )
            }
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
                FullScreenLoading()
            }
            items.itemCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize()) { emptyContent() }
            }
            else -> {
                // FIXED: Use itemSnapshotList to safely copy items without triggering side-effects
                val snapshotItems = items.itemSnapshotList.mapNotNull { it }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    item { headerContent() }
                    item {
                        when (layoutStyle) {
                            LayoutStyle.MASONRY -> MasonryGrid(
                                items = snapshotItems,
                                columns = columns,
                                aspectRatioOf = { post ->
                                    if (post.height > 0) post.width.toFloat() / post.height.toFloat() else 0.75f
                                }
                            ) { post ->
                                PostGridItem(
                                    post = post,
                                    onClick = { onPostClick(post) },
                                    onToggleFavorite = { onToggleFavorite(post) },
                                    onTagClick = onTagClick,
                                    cornerRadiusDp = cornerRadiusDp
                                )
                            }
                            LayoutStyle.GRID -> SimpleGrid(
                                items = snapshotItems,
                                columns = columns
                            ) { post ->
                                PostGridItem(
                                    post = post,
                                    onClick = { onPostClick(post) },
                                    onToggleFavorite = { onToggleFavorite(post) },
                                    onTagClick = onTagClick,
                                    cornerRadiusDp = cornerRadiusDp
                                )
                            }
                        }
                    }

                    if (items.loadState.append is LoadState.Loading) {
                        item { PagingAppendLoading() }
                    }

                    if (items.loadState.append is LoadState.Error) {
                        item { PagingAppendError(onRetry = { items.retry() }) }
                    }
                }
            }
        }
    }
}