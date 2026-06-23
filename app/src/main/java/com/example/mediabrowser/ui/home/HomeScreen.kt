package com.example.mediabrowser.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.trendingFeed.collectAsLazyPagingItems()
    val settings by viewModel.settings.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor)
    ) { paddingValues ->
        PagedPostGrid(
            items = pagingItems,
            columns = settings.gridColumns.coerceIn(1, 4),
            cornerRadiusDp = settings.cardCornerRadiusDp,
            layoutStyle = settings.homeLayoutStyle,
            onPostClick = { post ->
                // SAFE: Search inside itemSnapshotList to prevent triggering false page fetches
                val index = pagingItems.itemSnapshotList.indexOfFirst { it?.id == post.id }.coerceAtLeast(0)
                viewModel.openFeedAt(index)
            },
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            headerContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // FIXED: Merged structural padding bounds directly into header spacing for clean scrolling transitions
                        .padding(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = 8.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Home",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            },
            // FIXED: Modifier clean layout allows content to dynamically paint beneath navigation translucent bars smoothly
            modifier = Modifier.fillMaxSize()
        )
    }

    openFeedIndex?.let { index ->
        PostFeedScreen(
            startIndex = index,
            items = pagingItems,
            onDismiss = viewModel::closeFeed,
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            onDownload = { post -> viewModel.downloadPost(post) },
            getPostDetail = { post -> viewModel.getPostDetail(post) }
        )
    }
}