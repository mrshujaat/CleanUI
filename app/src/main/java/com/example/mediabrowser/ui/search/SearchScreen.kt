package com.example.mediabrowser.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.TagSuggestion
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.queryText.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor)
    ) { paddingValues ->
        when {
            suggestions.isNotEmpty() && query.isNotBlank() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
                ) {
                    SearchHeader(
                        query = query,
                        onQueryChanged = viewModel::onQueryChanged,
                        selectedTags = selectedTags,
                        onTagRemoved = viewModel::onTagRemoved,
                        onSubmit = viewModel::onSearchSubmit
                    )
                    TagSuggestionsList(suggestions, viewModel::onTagSelected)
                }
            }
            isSearchActive -> {
                val results = viewModel.searchResults.collectAsLazyPagingItems()
                PagedPostGrid(
                    items = results,
                    columns = settings.gridColumns.coerceIn(1, 4),
                    cornerRadiusDp = settings.cardCornerRadiusDp,
                    layoutStyle = settings.homeLayoutStyle,
                    onPostClick = { post ->
                        val index = (0 until results.itemCount).firstOrNull { results[it]?.id == post.id } ?: 0
                        viewModel.openFeedAt(index)
                    },
                    onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                    headerContent = {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            SearchHeader(
                                query = query,
                                onQueryChanged = viewModel::onQueryChanged,
                                selectedTags = selectedTags,
                                onTagRemoved = viewModel::onTagRemoved,
                                onSubmit = viewModel::onSearchSubmit
                            )
                            Text(
                                text = "Results",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                            Text(
                                text = "Tags & filters",
                                color = Color(0xFF8A8D91),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    },
                    emptyContent = {
                        Text("No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
                    },
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                )

                openFeedIndex?.let { index ->
                    PostFeedScreen(
                        startIndex = index,
                        items = results,
                        onDismiss = viewModel::closeFeed,
                        onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                        onDownload = { post -> viewModel.downloadPost(post) },
                        getPostDetail = { post -> viewModel.getPostDetail(post) }
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
                ) {
                    SearchHeader(
                        query = query,
                        onQueryChanged = viewModel::onQueryChanged,
                        selectedTags = selectedTags,
                        onTagRemoved = viewModel::onTagRemoved,
                        onSubmit = viewModel::onSearchSubmit
                    )
                    SearchHint()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    selectedTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Text(
        text = "Search",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
    TagSearchField(
        query = query,
        onQueryChanged = onQueryChanged,
        selectedTags = selectedTags,
        onTagRemoved = onTagRemoved,
        onSubmit = onSubmit
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    selectedTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF121315), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FlowRow(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                selectedTags.forEach { tag ->
                    TagPill(tag = tag, onRemove = { onTagRemoved(tag) })
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty() && selectedTags.isEmpty()) {
                            Text("Search for tags: car, model, jacket", color = Color(0xFF6B6E72), fontSize = 15.sp)
                        }
                        innerTextField()
                    },
                    modifier = Modifier.padding(vertical = 4.dp).widthIn(min = 40.dp)
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
private fun TagPill(tag: String, onRemove: () -> Unit) {
    val color = colorForTagHash(tag)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color.copy(alpha = 0.25f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = tag.replace('_', ' '), color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove $tag",
            tint = color,
            modifier = Modifier.padding(start = 4.dp).size(14.dp).clickable { onRemove() }
        )
    }
}

private fun colorForTagHash(tag: String): Color {
    val palette = listOf(
        Color(0xFFEF5DA8), Color(0xFF5DA8EF), Color(0xFF5DEFA8),
        Color(0xFFEFA85D), Color(0xFFB05DEF), Color(0xFFEF5D5D),
        Color(0xFF5DEFEF), Color(0xFFD4EF5D)
    )
    val index = kotlin.math.abs(tag.hashCode()) % palette.size
    return palette[index]
}

@Composable
private fun TagSuggestionsList(
    suggestions: List<TagSuggestion>,
    onTagSelected: (TagSuggestion) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(suggestions, key = { it.name }) { suggestion ->
            ListItem(
                headlineContent = { Text(suggestion.displayName, color = Color.White) },
                supportingContent = { Text("${suggestion.postCount} posts", color = Color(0xFF8A8D91)) },
                modifier = Modifier.fillMaxWidth().clickable { onTagSelected(suggestion) }.padding(horizontal = 4.dp)
            )
            Divider(color = Color(0xFF2A2D2F))
        }
    }
}

@Composable
private fun SearchHint() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Start typing to search for tags", color = Color(0xFF8A8D91), modifier = Modifier.padding(top = 24.dp))
    }
}