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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.LayoutStyle
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.domain.model.TagInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailPageStandalone(
    detail: PostDetail,
    onDismiss: () -> Unit,
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: DetailPageViewModel = hiltViewModel()
) {
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val navigateToArtist by viewModel.navigateToArtist.collectAsState()

    LaunchedEffect(navigateToArtist) {
        navigateToArtist?.let { profile ->
            onNavigateToArtist(profile)
            viewModel.clearArtistNavigation()
        }
    }

    val onTagAction: (String, TagCategory, TagModalAction) -> Unit = { name, category, action ->
        when (action) {
            TagModalAction.OPEN_IN_NEW_TAB -> viewModel.openTagInline(name, category)
            TagModalAction.ADD_TO_SEARCH -> viewModel.addTag(name)
            TagModalAction.TOGGLE_FAVORITE -> viewModel.toggleTagOrArtistFavorite(name, category)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        if (isSearchActive) {
            FeedInlineSearchBody(viewModel = viewModel, onNavigateToArtist = onNavigateToArtist)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { DetailSections(detail = detail, onTagAction = onTagAction) }
            }

            Divider(color = Color(0xFF2A2D2F))
            // DISMISS right-aligned, matching the PDF.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.reset(); onDismiss() }) {
                    Text("DISMISS", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineDetailContent(
    detail: PostDetail,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
    ) {
        DetailSections(detail = detail, onTagAction = onTagAction)
        // Trailing clearance so the LAST section (Meta) always scrolls fully
        // above the gesture/navigation bar, regardless of what container hosts
        // this content (dialogs can report zero nav-bar insets to their children).
        Spacer(
            modifier = Modifier.height(
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 72.dp
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedInlineSearchBody(
    viewModel: DetailPageViewModel,
    onNavigateToArtist: (ArtistProfile) -> Unit
) {
    val searchTags by viewModel.searchTags.collectAsState()
    val queryText by viewModel.queryText.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val results = viewModel.searchResults.collectAsLazyPagingItems()

    var openResultIndex by remember { mutableStateOf<Int?>(null) }

    InlineSearchBar(
        searchTags = searchTags,
        queryText = queryText,
        onQueryChanged = viewModel::onQueryChanged,
        onTagRemoved = viewModel::removeTag,
        onSubmit = viewModel::submitSearch
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PagedPostGrid(
            items = results,
            columns = settings.gridColumns.coerceIn(1, 4),
            cornerRadiusDp = settings.cardCornerRadiusDp,
            layoutStyle = settings.homeLayoutStyle,
            onPostClick = { post ->
                val index = results.itemSnapshotList.indexOfFirst { it?.id == post.id }.coerceAtLeast(0)
                openResultIndex = index
            },
            onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
            emptyContent = {
                Text("No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
            },
            modifier = Modifier.fillMaxSize()
        )

        openResultIndex?.let { index ->
            FeedScreenContent(
                startIndex = index,
                items = results,
                onDismiss = { openResultIndex = null },
                onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                onDownload = { post -> viewModel.downloadPost(post) },
                getPostDetail = { post -> viewModel.getPostDetail(post) },
                onNavigateToArtist = onNavigateToArtist,
                detailViewModel = viewModel,
                showFloatingSearchBar = false,
                forceShowFeed = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSections(
    detail: PostDetail,
    onTagAction: (String, TagCategory, TagModalAction) -> Unit
) {
    Column {
        val artistTags = detail.tags.filter { it.category == TagCategory.ARTIST }
        if (artistTags.isNotEmpty()) {
            SectionLabel(if (artistTags.size > 1) "Artists" else "Artist")
            TagSectionOrPlaceholder(tags = artistTags, emptyText = "", onTagAction = onTagAction)
        }

        val characterTags = detail.tags.filter { it.category == TagCategory.CHARACTER }
        if (characterTags.isNotEmpty()) {
            SectionLabel("Character")
            TagSectionOrPlaceholder(tags = characterTags, emptyText = "", onTagAction = onTagAction)
        }

        val copyrightTags = detail.tags.filter { it.category == TagCategory.COPYRIGHT }
        if (copyrightTags.isNotEmpty()) {
            SectionLabel("Series")
            TagSectionOrPlaceholder(tags = copyrightTags, emptyText = "", onTagAction = onTagAction)
        }

        val generalTags = detail.tags.filter { it.category == TagCategory.GENERAL }
        if (generalTags.isNotEmpty()) {
            SectionLabel("Tags")
            TagSectionOrPlaceholder(tags = generalTags, emptyText = "", onTagAction = onTagAction)
        }

        val metaTags = detail.tags.filter { it.category == TagCategory.META }
        if (metaTags.isNotEmpty()) {
            SectionLabel("Meta")
            TagSectionOrPlaceholder(tags = metaTags, emptyText = "", onTagAction = onTagAction)
        }

        detail.source?.let { source ->
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingSearchPillBar(
    searchTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit,
    // Optional live-typed query. When these are non-null the pill bar becomes
    // an editable input — typing adds text after the pills, hitting space
    // commits a tag, IME Search fires onSubmit. Passing null preserves the
    // original pills-only behavior for callers that don't want typing.
    queryText: androidx.compose.ui.text.input.TextFieldValue? = null,
    onQueryChanged: ((androidx.compose.ui.text.input.TextFieldValue) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF1A1D1F).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                searchTags.forEach { tag ->
                    SearchTagPill(tag = tag, onRemove = { onTagRemoved(tag) })
                }

                if (queryText != null && onQueryChanged != null) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = queryText,
                        onValueChange = onQueryChanged,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSubmit() }
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (queryText.text.isEmpty() && searchTags.isEmpty()) {
                                Text(
                                    "Add more tags...",
                                    color = Color(0xFF6B6E72),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .widthIn(min = 40.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onSubmit,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .background(Color(0xFF1A1D1F).copy(alpha = 0.95f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineSearchBar(
    searchTags: List<String>,
    queryText: androidx.compose.ui.text.input.TextFieldValue,
    onQueryChanged: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
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
                searchTags.forEach { tag ->
                    SearchTagPill(tag = tag, onRemove = { onTagRemoved(tag) })
                }

                androidx.compose.foundation.text.BasicTextField(
                    value = queryText,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        if (queryText.text.isEmpty() && searchTags.isEmpty()) {
                            Text(
                                "Add more tags...",
                                color = Color(0xFF6B6E72),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .widthIn(min = 40.dp)
                )
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
                    copyValue = tag.name,
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