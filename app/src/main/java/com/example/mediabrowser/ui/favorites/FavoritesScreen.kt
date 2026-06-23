package com.example.mediabrowser.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.FavoriteArtist
import com.example.mediabrowser.domain.model.FavoriteTag
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.ui.components.InteractiveTagChip
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.TagChip
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    onOpenArtist: (ArtistProfile) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(FavoritesTab.POSTS) }
    val settings by viewModel.settings.collectAsState()
    val openFeedSource by viewModel.openFeedSource.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    val postsPagingItems = viewModel.favoritePosts.collectAsLazyPagingItems()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = "Favorites",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                FavoritesTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (selectedTab) {
                FavoritesTab.POSTS -> {
                    PagedPostGrid(
                        items = postsPagingItems,
                        columns = settings.gridColumns.coerceIn(1, 4),
                        cornerRadiusDp = settings.cardCornerRadiusDp,
                        layoutStyle = settings.favoritesLayoutStyle,
                        onPostClick = { post ->
                            val index = (0 until postsPagingItems.itemCount)
                                .firstOrNull { postsPagingItems[it]?.id == post.id } ?: 0
                            viewModel.openFeedAt(FeedSource.POSTS_TAB, index)
                        },
                        onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                        emptyContent = {
                            Text(
                                text = "No favorites yet. Tap the heart on any post to save it here.",
                                color = Color(0xFF8A8D91),
                                modifier = Modifier.padding(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                FavoritesTab.TAGS -> {
                    TagsTabContent(viewModel)
                }
                FavoritesTab.ARTISTS -> {
                    val artists by viewModel.favoriteArtists.collectAsState()
                    ArtistsPillRow(artists, onOpenArtist = onOpenArtist, onRemove = viewModel::removeFavoriteArtist)
                }
            }
        }
    }

    if (openFeedSource == FeedSource.POSTS_TAB) {
        PostFeedScreen(
            startIndex = openFeedIndex,
            items = postsPagingItems,
            onDismiss = viewModel::closeFeed,
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            onDownload = { post -> viewModel.downloadPost(post) },
            getPostDetail = { post -> viewModel.getPostDetail(post) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsTabContent(viewModel: FavoritesViewModel) {
    val tags by viewModel.favoriteTags.collectAsState()
    val searchTags by viewModel.searchTags.collectAsState()
    val isTagSearchActive by viewModel.isTagSearchActive.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val openFeedSource by viewModel.openFeedSource.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()

    val tagSearchPagingItems = viewModel.tagSearchResults.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize()) {
        if (searchTags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(modifier = Modifier.fillMaxWidth(0.85f)) {
                    searchTags.forEach { tag ->
                        TagChip(
                            label = tag.replace('_', ' '),
                            category = TagCategory.GENERAL,
                            onClick = { viewModel.removeSearchTag(tag) }
                        )
                    }
                }
                IconButton(onClick = { viewModel.submitTagSearch() }) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Search", tint = Color.White)
                }
            }
        }

        if (isTagSearchActive) {
            PagedPostGrid(
                items = tagSearchPagingItems,
                columns = settings.gridColumns.coerceIn(2, 4),
                cornerRadiusDp = settings.cardCornerRadiusDp,
                layoutStyle = settings.favoritesLayoutStyle,
                onPostClick = { post ->
                    val index = (0 until tagSearchPagingItems.itemCount)
                        .firstOrNull { tagSearchPagingItems[it]?.id == post.id } ?: 0
                    viewModel.openFeedAt(FeedSource.TAGS_SEARCH, index)
                },
                onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                emptyContent = {
                    Text(text = "No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
                },
                modifier = Modifier.fillMaxSize()
            )

            if (openFeedSource == FeedSource.TAGS_SEARCH) {
                PostFeedScreen(
                    startIndex = openFeedIndex,
                    items = tagSearchPagingItems,
                    onDismiss = viewModel::closeFeed,
                    onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                    onDownload = { post -> viewModel.downloadPost(post) },
                    getPostDetail = { post -> viewModel.getPostDetail(post) }
                )
            }
        } else {
            TagsPillGrid(
                tags = tags,
                onTagOpenInline = { tagName ->
                    viewModel.addSearchTag(tagName)
                    viewModel.submitTagSearch()
                },
                onAddToSearch = viewModel::addSearchTag,
                onToggleFavorite = viewModel::toggleTagFavorite,
                onRemove = viewModel::removeFavoriteTag
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsPillGrid(
    tags: List<FavoriteTag>,
    onTagOpenInline: (String) -> Unit,
    onAddToSearch: (String) -> Unit,
    onToggleFavorite: (String, TagCategory) -> Unit,
    onRemove: (String) -> Unit
) {
    if (tags.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "No favorite tags yet.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
        }
        return
    }

    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        tags.forEach { tag ->
            InteractiveTagChip(
                label = tag.displayName,
                category = tag.category,
                isFavorite = true,
                onOpenInNewTab = { onTagOpenInline(tag.tagName) },
                onAddToSearch = { onAddToSearch(tag.tagName) },
                onToggleFavorite = { onRemove(tag.tagName) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistsPillRow(
    artists: List<FavoriteArtist>,
    onOpenArtist: (ArtistProfile) -> Unit,
    onRemove: (String) -> Unit
) {
    if (artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "No favorite artists yet.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
        }
        return
    }

    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        artists.forEach { artist ->
            TagChip(
                label = artist.displayName,
                category = TagCategory.ARTIST,
                onClick = {
                    onOpenArtist(
                        ArtistProfile(
                            artistId = artist.artistName,
                            displayName = artist.displayName,
                            postQuery = artist.artistName
                        )
                    )
                }
            )
        }
    }
}