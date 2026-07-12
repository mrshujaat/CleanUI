package com.example.mediabrowser.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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

/**
 * Full grid for whichever [HomeCategory] was tapped from a Home row's "see
 * all" chevron or one of its thumbnail cards. Mirrors HomeScreen's layout
 * but is parameterized by the category's tag query, with a back button
 * instead of a search icon.
 */
@Composable
fun CategoryDetailScreen(
    category: HomeCategory,
    onBackClick: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.posts(category).collectAsLazyPagingItems()
    val settings by viewModel.settings.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
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
                    imageQuality = settings.imageQuality,
            onPostClick = { post ->
                val index = pagingItems.itemSnapshotList.indexOfFirst { it?.id == post.id }.coerceAtLeast(0)
                viewModel.openFeedAt(index)
            },
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            headerContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = 8.dp,
                            start = 4.dp,
                            end = 16.dp
                        )
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = category.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            },
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
