package com.example.mediabrowser.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.ArrowBack
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

/**
 * [onOpenArtist] is used by the Artists tab's own pill row (tapping a
 * favorited artist directly). [onNavigateToArtist] is used by any
 * PostFeedScreen hosted here, when a tag inside a post's expanded details
 * turns out to be an artist tag — same destination, different trigger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    onOpenArtist: (ArtistProfile) -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    section: FavoriteSection? = null,
    onOpenSection: (FavoriteSection) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    // No section selected → show the landing list of sections.
    if (section == null) {
        FavoritesLanding(accentColor = accentColor, onOpenSection = onOpenSection)
        return
    }

    // A section is selected → show that section's content.
    FavoriteSectionContent(
        section = section,
        viewModel = viewModel,
        onOpenArtist = onOpenArtist,
        onNavigateToArtist = onNavigateToArtist,
        onBack = onBack
    )
}

/** The Favourites landing list: title + five tappable section rows (matches the PDF). */
@Composable
private fun FavoritesLanding(
    accentColor: Color,
    onOpenSection: (FavoriteSection) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor, androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = "Favourites",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

            FavoriteSection.entries.forEach { sec ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSection(sec) }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = sec.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = sec.subtitle,
                        color = Color(0xFF8A8D91),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteSectionContent(
    section: FavoriteSection,
    viewModel: FavoritesViewModel,
    onOpenArtist: (ArtistProfile) -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val openFeedSource by viewModel.openFeedSource.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))
    val context = androidx.compose.ui.platform.LocalContext.current

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.exportBatchesText().toByteArray())
                }
                android.widget.Toast.makeText(context, "Batches exported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                viewModel.importBatchesText(text)
                android.widget.Toast.makeText(context, "Batches imported", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor, androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Header with back button + section title.
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            when (section) {
                FavoriteSection.POSTS -> {
                    // Sub-tabs: split favourited posts into Images and Videos.
                    var postMediaTab by remember { mutableStateOf(PostMediaTab.IMAGES) }
                    val postItems = when (postMediaTab) {
                        PostMediaTab.IMAGES -> viewModel.favoriteImages.collectAsLazyPagingItems()
                        PostMediaTab.VIDEOS -> viewModel.favoriteVideos.collectAsLazyPagingItems()
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PostMediaTabPill(
                                label = "Images",
                                selected = postMediaTab == PostMediaTab.IMAGES,
                                accent = accentColor,
                                onClick = { postMediaTab = PostMediaTab.IMAGES }
                            )
                            PostMediaTabPill(
                                label = "Videos",
                                selected = postMediaTab == PostMediaTab.VIDEOS,
                                accent = accentColor,
                                onClick = { postMediaTab = PostMediaTab.VIDEOS }
                            )
                        }

                        PagedPostGrid(
                            items = postItems,
                            columns = settings.gridColumns.coerceIn(1, 4),
                            cornerRadiusDp = settings.cardCornerRadiusDp,
                            layoutStyle = settings.favoritesLayoutStyle,
                            imageQuality = settings.imageQuality,
                            onPostClick = { post ->
                                val index = (0 until postItems.itemCount)
                                    .firstOrNull { postItems[it]?.id == post.id } ?: 0
                                viewModel.openFeedAt(FeedSource.POSTS_TAB, index)
                            },
                            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                            onDownload = { post -> viewModel.downloadPost(post) },
                            emptyContent = {
                                Text(
                                    if (postMediaTab == PostMediaTab.VIDEOS)
                                        "No favourited videos yet."
                                    else "No favourited images yet. Tap the heart on any image to save it here.",
                                    color = Color(0xFF8A8D91),
                                    modifier = Modifier.padding(24.dp)
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (openFeedSource == FeedSource.POSTS_TAB) {
                        PostFeedScreen(
                            startIndex = openFeedIndex,
                            items = postItems,
                            onDismiss = viewModel::closeFeed,
                            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                            onDownload = { post -> viewModel.downloadPost(post) },
                            getPostDetail = { post -> viewModel.getPostDetail(post) },
                            onNavigateToArtist = onNavigateToArtist
                        )
                    }
                }
                FavoriteSection.MY_POISON -> {
                    val batches by viewModel.tagBatches.collectAsState()
                    val isBatchSearchActive by viewModel.isTagSearchActive.collectAsState()
                    if (isBatchSearchActive) {
                        BatchSearchResults(viewModel)
                    } else {
                        BatchesTabContent(
                            batches = batches,
                            accentColor = accentColor,
                            onSearchBatch = { viewModel.searchBatch(it) },
                            onAddTags = { batch, tags -> viewModel.addTagsToBatch(batch, tags) },
                            onRemoveTag = { batch, tag -> viewModel.removeTagFromBatch(batch, tag) },
                            onRename = { batch, name -> viewModel.renameBatch(batch, name) },
                            onDelete = { viewModel.deleteBatch(it) },
                            onExport = { exportLauncher.launch("my_poison_batches.txt") },
                            onImport = { importLauncher.launch("text/plain") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                FavoriteSection.ARTISTS -> {
                    val artists by viewModel.favoriteArtists.collectAsState()
                    ArtistsPillRow(artists, onOpenArtist = onOpenArtist, onRemove = viewModel::removeFavoriteArtist)
                }
                // Characters / Series / Tags are favourite tags filtered by category.
                else -> {
                    // FIXED: tapping a tag activates the shared tag search, but only
                    // MY_POISON ever rendered the results — so taps here looked dead.
                    // Now these sections swap to the results grid the same way.
                    val isTagSearchActive by viewModel.isTagSearchActive.collectAsState()
                    if (isTagSearchActive) {
                        BatchSearchResults(viewModel)
                    } else {
                        val allTags by viewModel.favoriteTags.collectAsState()
                        val filtered = when (section) {
                            FavoriteSection.CHARACTERS -> allTags.filter { it.category == TagCategory.CHARACTER }
                            FavoriteSection.SERIES -> allTags.filter { it.category == TagCategory.COPYRIGHT }
                            else -> allTags.filter {
                                it.category == TagCategory.GENERAL || it.category == TagCategory.META
                            }
                        }
                        FavoriteTagsList(
                            tags = filtered,
                            emptyText = "Nothing here yet.",
                            onTagClick = { tag ->
                                viewModel.searchBatch(
                                    com.example.mediabrowser.domain.model.TagBatch(
                                        id = -1, name = tag.displayName,
                                        tags = listOf(tag.tagName), createdAt = 0, updatedAt = 0
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (openFeedSource == FeedSource.POSTS_TAB || openFeedSource == FeedSource.TAGS_SEARCH) {
        // Section content may host a feed via BatchSearchResults; nothing extra needed here.
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchSearchResults(
    viewModel: FavoritesViewModel
) {
    val searchTags by viewModel.searchTags.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val openFeedSource by viewModel.openFeedSource.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val resultItems = viewModel.tagSearchResults.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize()) {
        // Active batch tags with a back action that returns to the batch list.
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
            IconButton(onClick = { viewModel.clearTagSearch() }) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Back to batches", tint = Color.White)
            }
        }

        PagedPostGrid(
            items = resultItems,
            columns = settings.gridColumns.coerceIn(2, 4),
            cornerRadiusDp = settings.cardCornerRadiusDp,
            layoutStyle = settings.favoritesLayoutStyle,
                    imageQuality = settings.imageQuality,
            onPostClick = { post ->
                val index = (0 until resultItems.itemCount)
                    .firstOrNull { resultItems[it]?.id == post.id } ?: 0
                viewModel.openFeedAt(FeedSource.TAGS_SEARCH, index)
            },
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            onDownload = { post -> viewModel.downloadPost(post) },
            emptyContent = {
                Text(text = "No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
            },
            onBack = { viewModel.clearTagSearch() },
            modifier = Modifier.fillMaxSize()
        )

        if (openFeedSource == FeedSource.TAGS_SEARCH) {
            PostFeedScreen(
                startIndex = openFeedIndex,
                items = resultItems,
                onDismiss = viewModel::closeFeed,
                onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                onDownload = { post -> viewModel.downloadPost(post) },
                getPostDetail = { post -> viewModel.getPostDetail(post) },
                onNavigateToArtist = {}
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsTabContent(
    viewModel: FavoritesViewModel,
    onNavigateToArtist: (ArtistProfile) -> Unit
) {
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
                    imageQuality = settings.imageQuality,
                onPostClick = { post ->
                    val index = (0 until tagSearchPagingItems.itemCount)
                        .firstOrNull { tagSearchPagingItems[it]?.id == post.id } ?: 0
                    viewModel.openFeedAt(FeedSource.TAGS_SEARCH, index)
                },
                onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            onDownload = { post -> viewModel.downloadPost(post) },
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
                    getPostDetail = { post -> viewModel.getPostDetail(post) },
                    onNavigateToArtist = onNavigateToArtist
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
                copyValue = tag.tagName,
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
/**
 * Renders a list of favourite tags as solid category-colored pills (matching the
 * redesign). Tapping a pill runs a search for that tag via the batch-search feed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteTagsList(
    tags: List<FavoriteTag>,
    emptyText: String,
    onTagClick: (FavoriteTag) -> Unit
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    if (tags.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = emptyText, color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
        }
        return
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        tags.forEach { tag ->
            TagChip(
                label = tag.displayName,
                category = tag.category,
                onClick = { onTagClick(tag) },
                // Long-press copies the raw API tag name.
                onLongClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(tag.tagName))
                    android.widget.Toast.makeText(context, "Copied: ${tag.tagName}", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

private enum class PostMediaTab { IMAGES, VIDEOS }

@Composable
private fun PostMediaTabPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) accent else Color(0x22FFFFFF),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF111111) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}