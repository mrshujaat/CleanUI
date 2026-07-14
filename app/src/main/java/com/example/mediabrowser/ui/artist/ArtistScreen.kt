package com.example.mediabrowser.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@Composable
fun ArtistScreen(
    profile: ArtistProfile,
    onBackClick: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    LaunchedEffect(profile.artistId) {
        viewModel.load(profile)
        viewModel.refreshArtistFavorite(profile.postQuery, profile.displayName)
    }

    val pagingItems = viewModel.posts.collectAsLazyPagingItems()
    val isArtistFav by viewModel.isArtistFavorite.collectAsState()

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
                ArtistHeader(
                    profile = profile,
                    onBackClick = onBackClick,
                    isFavorite = isArtistFav,
                    onToggleFavorite = { viewModel.toggleArtistFavorite() }
                )
            },
            emptyContent = {
                Text(
                    text = "No posts found for this artist.",
                    color = Color(0xFF8A8D91),
                    modifier = Modifier.padding(24.dp)
                )
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
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

@Composable
private fun ArtistHeader(
    profile: ArtistProfile,
    onBackClick: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            // Favourite THIS artist — so you can follow them without leaving the page.
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unfollow artist" else "Follow artist",
                    tint = if (isFavorite) Color(0xFFE53935) else Color.White
                )
            }
        }

        Box(
            modifier = Modifier.size(72.dp).background(Color(0xFF2DD4BF).copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color(0xFF2DD4BF),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = profile.displayName,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )

        profile.bio?.let {
            Text(
                text = it,
                color = Color(0xFF8A8D91),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        } ?: Text(text = "", modifier = Modifier.padding(bottom = 16.dp))
    }
}