package com.example.mediabrowser.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.HomeCategory
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onOpenCategory: (HomeCategory) -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.trendingFeed.collectAsLazyPagingItems()
    val settings by viewModel.settings.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val mostPopularPreview by viewModel.mostPopularPreview.collectAsState()
    val trendingPreview by viewModel.trendingPreview.collectAsState()
    val topSeriesCards by viewModel.topSeriesCards.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor, androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        PagedPostGrid(
            items = pagingItems,
            columns = settings.gridColumns.coerceIn(1, 4),
            cornerRadiusDp = settings.cardCornerRadiusDp,
            layoutStyle = settings.homeLayoutStyle,
            onPostClick = { post ->
                // SAFE: Search inside itemSnapshotList to prevent triggering false page fetches
                val index = pagingItems.itemSnapshotList.indexOfFirst { it?.id == post.id }.coerceAtLeast(0)
                viewModel.recordView(post)
                viewModel.openFeedAt(index)
            },
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            headerContent = {
                Column {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(paddingValues.calculateTopPadding() + 12.dp)
                    )

                    HomeCategoryRow(
                        title = HomeCategory.MostPopular.title,
                        posts = mostPopularPreview,
                        onSeeAllClick = { onOpenCategory(HomeCategory.MostPopular) }
                    )

                    HomeCategoryRow(
                        title = HomeCategory.Trending.title,
                        posts = trendingPreview,
                        onSeeAllClick = { onOpenCategory(HomeCategory.Trending) }
                    )

                    TopSeriesRow(
                        cards = topSeriesCards,
                        onCardClick = { category -> onOpenCategory(category) }
                    )

                    // Label for the main feed below: reflects the Home feed type.
                    Text(
                        text = if (settings.homeFeedType == com.example.mediabrowser.domain.model.FeedType.POISON)
                            "My Poison" else "Your Feed",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            },
            emptyContent = {
                if (settings.homeFeedType == com.example.mediabrowser.domain.model.FeedType.POISON) {
                    Text(
                        text = "Please search, like, download, and favourite to update the Poison Feed.",
                        color = Color(0xFF8A8D91),
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
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
            getPostDetail = { post -> viewModel.getPostDetail(post) },
            onNavigateToArtist = onNavigateToArtist
        )
    }
}