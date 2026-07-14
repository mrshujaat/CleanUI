package com.example.mediabrowser.ui.search

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.TagSuggestion
import com.example.mediabrowser.ui.components.PagedPostGrid
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.queryText.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val openPost by viewModel.openPost.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))
    var showSaveBatchDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor, androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        // The search header is rendered ONCE here, never inside a conditional
        // branch — so the text field instance is stable and never loses focus when
        // suggestions appear/disappear or results load. Only the content BELOW the
        // header swaps based on state.
        val showSuggestions = suggestions.isNotEmpty() && query.text.isNotBlank()

        // The header now FLOATS over the content on a transparent layer: results
        // scroll behind it, scrolling down hides it, scrolling up brings it back.
        var headerVisible by remember { mutableStateOf(true) }
        var headerHeightPx by remember { mutableStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val headerHeightDp = with(density) { headerHeightPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ---- Content layer (fills the screen; scrolls under the header) ----
            when {
                showSuggestions -> {
                    Column(
                        modifier = Modifier
                            .padding(top = headerHeightDp)
                            .padding(horizontal = 16.dp)
                    ) {
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
                        imageQuality = settings.imageQuality,
                        onPostClick = { post ->
                            viewModel.recordView(post)
                            // Open ONLY this post in HD with details expanded — not
                            // the scroll feed. Further searches drill down from there.
                            viewModel.openPost(post)
                        },
                        onToggleFavorite = { post -> viewModel.toggleFavorite(post) },
                        onDownload = { post -> viewModel.downloadPost(post) },
                        emptyContent = {
                            Text("No results found.", color = Color(0xFF8A8D91), modifier = Modifier.padding(24.dp))
                        },
                        onBack = { viewModel.backToTagEditing() },
                        // First grid item reserves the header's height so results
                        // start below the floating bar, then scroll behind it.
                        headerContent = {
                            androidx.compose.foundation.layout.Spacer(Modifier.height(headerHeightDp))
                        },
                        onScrollDirectionChanged = { scrolledDown -> headerVisible = !scrolledDown },
                        modifier = Modifier.fillMaxSize()
                    )

                    openPost?.let { post ->
                        com.example.mediabrowser.ui.components.SearchDrillDownDialog(
                            post = post,
                            // Seed the drill-down's pill bar with the current search,
                            // so added tags accumulate on top of it.
                            baseTags = selectedTags,
                            onDismiss = viewModel::closePost,
                            onNavigateToArtist = onNavigateToArtist
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = headerHeightDp)
                            .padding(horizontal = 16.dp)
                    ) {
                        SearchHint()
                    }
                }
            }

            // ---- Floating header layer (transparent — content shows through) ----
            // Always shown while typing/suggestions or before a search; while
            // browsing results it follows scroll direction.
            androidx.compose.animation.AnimatedVisibility(
                visible = headerVisible || !isSearchActive || showSuggestions,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) +
                    androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) +
                    androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { headerHeightPx = it.size.height }
                ) {
                    SearchHeader(
                        query = query,
                        onQueryChanged = viewModel::onQueryChanged,
                        selectedTags = selectedTags,
                        onTagRemoved = viewModel::onTagRemoved,
                        onSubmit = viewModel::onSearchSubmit,
                        onClear = viewModel::clearSearch,
                        onPasteTags = viewModel::pasteTags,
                        onAddAsBatch = { showSaveBatchDialog = true }
                    )
                    if (selectedTags.isNotEmpty() && !showSuggestions) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .border(1.dp, Color(0xFF8A8D91), RoundedCornerShape(50))
                                .clickable { showSaveBatchDialog = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Save Batch", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

        if (showSaveBatchDialog) {
            com.example.mediabrowser.ui.favorites.NameBatchDialog(
                initialTags = selectedTags,
                onConfirm = { name ->
                    viewModel.saveCurrentTagsAsBatch(name)
                    showSaveBatchDialog = false
                    android.widget.Toast.makeText(
                        context,
                        "Saved batch \"$name\"",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { showSaveBatchDialog = false }
            )
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHeader(
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    selectedTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onPasteTags: (String) -> Unit,
    onAddAsBatch: () -> Unit
) {
    Text(
        text = "Search",
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp)
    )
    TagSearchField(
        query = query,
        onQueryChanged = onQueryChanged,
        selectedTags = selectedTags,
        onTagRemoved = onTagRemoved,
        onSubmit = onSubmit,
        onClear = onClear,
        onPasteTags = onPasteTags,
        onAddAsBatch = onAddAsBatch
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TagSearchField(
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    selectedTags: List<String>,
    onTagRemoved: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onPasteTags: (String) -> Unit,
    onAddAsBatch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val tagScrollState = androidx.compose.foundation.rememberScrollState()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var boxMenuOpen by remember { mutableStateOf(false) }

    // Local text state so typing is instant and the field NEVER loses focus to a
    // ViewModel round-trip (the "drops after 1-2 letters" bug). In the normal case
    // the VM just echoes our value back, so they stay equal. They differ only when
    // the VM transforms the text (e.g. a comma committed a tag and reset the field
    // to the remainder) — in that case we sync the VM's value down. Done in an
    // effect (not raw composition) to avoid any recomposition churn.
    var localValue by remember { mutableStateOf(query) }
    androidx.compose.runtime.LaunchedEffect(query.text) {
        if (query.text != localValue.text) {
            localValue = query
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                // Solid dark fill so the bar reads as a floating black pill on top
                // of the results grid — transparency showed the grid images through
                // and looked broken. Border kept for definition.
                .background(Color(0xFF0B0C0E), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF8A8D91), RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = { focusRequester.requestFocus() },
                    onLongClick = { boxMenuOpen = true }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Long-press on empty space → Copy / Paste / Add as Batch menu.
            androidx.compose.material3.DropdownMenu(
                expanded = boxMenuOpen,
                onDismissRequest = { boxMenuOpen = false }
            ) {
                if (selectedTags.isNotEmpty()) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Copy all (${selectedTags.size} tags)") },
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(selectedTags.joinToString(" ")))
                            android.widget.Toast.makeText(context, "Copied ${selectedTags.size} tags", android.widget.Toast.LENGTH_SHORT).show()
                            boxMenuOpen = false
                        }
                    )
                }
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Paste") },
                    onClick = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isBlank()) {
                            android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onPasteTags(pasted)
                            android.widget.Toast.makeText(context, "Tags pasted", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        boxMenuOpen = false
                    }
                )
                if (selectedTags.isNotEmpty()) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Add as Batch") },
                        onClick = {
                            boxMenuOpen = false
                            onAddAsBatch()
                        }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Pills AND the text field live in one FlowRow, so the cursor sits
                // right after the last pill on the same line and only wraps when the
                // row actually runs out of width — no premature new line.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 96.dp)
                        .verticalScroll(tagScrollState)
                ) {
                    selectedTags.forEach { tag ->
                        TagPill(
                            tag = tag,
                            allTags = selectedTags,
                            onRemove = { onTagRemoved(tag) },
                            onPasteTags = onPasteTags,
                            onAddAsBatch = onAddAsBatch
                        )
                    }

                    BasicTextField(
                        value = localValue,
                        onValueChange = { newValue ->
                            localValue = newValue          // instant, keeps focus
                            onQueryChanged(newValue)       // notify VM for suggestions/commits
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (localValue.text.isEmpty() && selectedTags.isEmpty()) {
                                Text("eg. naruto_uzumaki, use comma to separate tags", color = Color(0xFF6B6E72), fontSize = 15.sp)
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .widthIn(min = 60.dp)
                            .focusRequester(focusRequester)
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Clear (×) pinned to the box's vertical center and right edge, so it
            // lines up with the circular arrow. Always visible.
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = Color(0xFF8A8D91),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
                    .clickable { onClear() }
            )
        }

        // Fixed-size circular arrow, vertically centered next to the bar.
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(56.dp)
                .border(1.dp, Color(0xFF8A8D91), androidx.compose.foundation.shape.CircleShape)
                .clickable {
                    onSubmit()
                    focusRequester.requestFocus()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagPill(
    tag: String,
    allTags: List<String>,
    onRemove: () -> Unit,
    onPasteTags: (String) -> Unit,
    onAddAsBatch: () -> Unit
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        // Solid purple pill with white text. Long-press opens a Copy / Select all menu.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    com.example.mediabrowser.ui.theme.SelectedTagPurple,
                    RoundedCornerShape(50)
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuOpen = true }
                )
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(text = tag, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove $tag",
                tint = Color.White,
                modifier = Modifier.padding(start = 6.dp).size(14.dp).clickable { onRemove() }
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Copy \"$tag\"") },
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(tag))
                    android.widget.Toast.makeText(context, "Copied: $tag", android.widget.Toast.LENGTH_SHORT).show()
                    menuOpen = false
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Select all") },
                onClick = {
                    val joined = allTags.joinToString(" ")
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(joined))
                    android.widget.Toast.makeText(context, "Copied ${allTags.size} tags", android.widget.Toast.LENGTH_SHORT).show()
                    menuOpen = false
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Paste") },
                onClick = {
                    val pasted = clipboard.getText()?.text.orEmpty()
                    if (pasted.isBlank()) {
                        android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onPasteTags(pasted)
                        android.widget.Toast.makeText(context, "Tags pasted", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    menuOpen = false
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Add as Batch") },
                onClick = {
                    menuOpen = false
                    onAddAsBatch()
                }
            )
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTagSelected(suggestion) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = suggestion.displayName,
                    // Primary results (from the r34.app-style autocomplete) are
                    // highlighted in the PDF's exact orange; supplementary
                    // prefix-matches stay white.
                    color = if (suggestion.isPrimary)
                        com.example.mediabrowser.ui.theme.SuggestionPrimaryOrange
                    else Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "${suggestion.postCount} posts",
                    color = Color(0xFF8A8D91),
                    fontSize = 12.sp
                )
            }
        }
    }
}
@Composable
private fun SearchHint() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Start typing to search for tags", color = Color(0xFF8A8D91), modifier = Modifier.padding(top = 24.dp))
    }
}