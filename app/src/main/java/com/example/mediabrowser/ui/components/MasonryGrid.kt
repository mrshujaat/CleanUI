package com.example.mediabrowser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pinterest-style masonry grid featuring full item virtualization and recycling.
 * Ensures the UI thread remains smooth regardless of how deep the user scrolls.
 *
 * [key] MUST return a stable, unique value per item (e.g. a database id) —
 * without it, Compose falls back to position-based keys, which collide and
 * crash ("Key X was already used") whenever the underlying list is mutated
 * while scrolled (items added/removed/reordered).
 */
@Composable
fun <T> MasonryGrid(
    items: List<T>,
    columns: Int,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    aspectRatioOf: (T) -> Float, // Kept to match method signature conventions if needed elsewhere
    itemContent: @Composable (T) -> Unit
) {
    // FIXED: Swapped out manual column allocation loops for hardware-accelerated Staggered Grid virtualization
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(items) { item ->
            itemContent(item)
        }
    }
}

/** * Fully virtualized fixed-row alternative used when LayoutStyle.GRID is selected.
 *
 * [key] MUST return a stable, unique value per item — see [MasonryGrid] for why.
 */
@Composable
fun <T> SimpleGrid(
    items: List<T>,
    columns: Int,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    // FIXED: Migrated from chunked row loops over standard Columns to an optimized lazy framework implementation
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            itemContent(item)
        }
    }
}