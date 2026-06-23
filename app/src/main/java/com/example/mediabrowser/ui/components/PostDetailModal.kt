package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.mediabrowser.domain.model.LayoutStyle
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.domain.model.TagInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailPageStandalone(
    detail: PostDetail,
    onDismiss: () -> Unit,
    viewModel: DetailPageViewModel = hiltViewModel()
) {
    val searchTags by viewModel.searchTags.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val results = viewModel.searchResults.collectAsLazyPagingItems()

    val onTagAction: (String, TagCategory, TagModalAction) -> Unit = { name, category, action ->
        when (action) {
            TagModalAction.OPEN_IN_NEW_TAB -> viewModel.openTagInline(name)
            TagModalAction.ADD_TO_SEARCH -> viewModel.addTag(name)
            TagModalAction.TOGGLE_FAVORITE -> viewModel.toggleTagOrArtistFavorite(name, category)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C0E))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        InlineSearchBar(
            searchTags = searchTags,
            onTagRemoved = viewModel::removeTag,
            onSubmit = viewModel::submitSearch
        )

        if (isSearchActive) {
            PagedPostGrid(
                items = results,
                columns = 2,
                cornerRadiusDp = 16,
                layoutStyle = LayoutStyle.MASONRY,
                onPostClick = { },
                onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                emptyContent = {
                    Text("No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { SectionLabel("Artist") }
                item { ArtistOrPlaceholder(detail = detail, onTagAction = onTagAction) }
                item { SectionLabel("Copyright") }
                item {
                    TagSectionOrPlaceholder(
                        tags = detail.tags.filter { it.category == TagCategory.COPYRIGHT },
                        emptyText = "No copyright tags",
                        onTagAction = onTagAction
                    )
                }
                item { SectionLabel("Tags") }
                item {
                    TagSectionOrPlaceholder(
                        tags = detail.tags.filter { it.category == TagCategory.GENERAL || it.category == TagCategory.CHARACTER },
                        emptyText = "No tags",
                        onTagAction = onTagAction
                    )
                }
                item { SectionLabel("Meta") }
                item {
                    TagSectionOrPlaceholder(
                        tags = detail.tags.filter { it.category == TagCategory.META },
                        emptyText = "No meta tags",
                        onTagAction = onTagAction
                    )
                }
                detail.source?.let { source ->
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
                        ) {
                            Text(text = "Source: ", color = Color(0xFF8A8D91), style = MaterialTheme.typography.bodyMedium)
                            AssistChip(
                                onClick = { },
                                label = { Text(source, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        Divider(color = Color(0xFF2A2D2F))
        TextButton(
            onClick = { viewModel.reset(); onDismiss() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("DISMISS", color = Color.White)
        }
    }
}

// ... (Keep the rest of the private helper functions like InlineSearchBar, SearchTagPill, etc. unchanged)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineSearchBar(
    searchTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF1A1D1F), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (searchTags.isEmpty()) {
                    Text(
                        "Tap a tag below to add it here",
                        color = Color(0xFF6B6E72),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                searchTags.forEach { tag ->
                    SearchTagPill(tag = tag, onRemove = { onTagRemoved(tag) })
                }
            }
        }

        IconButton(onClick = onSubmit, modifier = Modifier.padding(start = 8.dp).size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(40.dp).padding(4.dp)
            )
        }
    }
}

@Composable
private fun SearchTagPill(tag: String, onRemove: () -> Unit) {
    val color = colorForCategory(TagCategory.GENERAL)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = tag.replace('_', ' '), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove $tag",
            tint = Color.White,
            modifier = Modifier.padding(start = 4.dp).size(14.dp).clickable { onRemove() }
        )
    }
}

@Composable
private fun ArtistOrPlaceholder(
    detail: PostDetail,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit
) {
    val artist = detail.uploader
    if (artist == null) {
        PlaceholderText("No artist")
    } else {
        FlowRowSection {
            InteractiveTagChip(
                label = artist,
                category = TagCategory.ARTIST,
                isFavorite = false,
                onOpenInNewTab = { onTagAction(artist, TagCategory.ARTIST, TagModalAction.OPEN_IN_NEW_TAB) },
                onAddToSearch = { onTagAction(artist, TagCategory.ARTIST, TagModalAction.ADD_TO_SEARCH) },
                onToggleFavorite = { onTagAction(artist, TagCategory.ARTIST, TagModalAction.TOGGLE_FAVORITE) }
            )
        }
    }
}

@Composable
private fun TagSectionOrPlaceholder(
    tags: List<TagInfo>,
    emptyText: String,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit
) {
    if (tags.isEmpty()) {
        PlaceholderText(emptyText)
    } else {
        FlowRowSection {
            tags.forEach { tag ->
                InteractiveTagChip(
                    label = tag.name.replace('_', ' '),
                    category = tag.category,
                    isFavorite = false,
                    onOpenInNewTab = { onTagAction(tag.name, tag.category, TagModalAction.OPEN_IN_NEW_TAB) },
                    onAddToSearch = { onTagAction(tag.name, tag.category, TagModalAction.ADD_TO_SEARCH) },
                    onToggleFavorite = { onTagAction(tag.name, tag.category, TagModalAction.TOGGLE_FAVORITE) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSection(content: @Composable () -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { content() }
}

@Composable
private fun PlaceholderText(text: String) {
    Text(
        text = text,
        color = Color(0xFF5A5D61),
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 26.sp,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp).padding(bottom = 8.dp)
    )
}

enum class TagModalAction {
    OPEN_IN_NEW_TAB,
    ADD_TO_SEARCH,
    TOGGLE_FAVORITE
}