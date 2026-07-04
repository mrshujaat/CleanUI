package com.example.mediabrowser.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediabrowser.domain.model.TagBatch

/**
 * "My Poison" — saved tag batches, styled to the redesign:
 *  - Batch name is a white heading ABOVE each block.
 *  - The block is a bordered rounded rectangle holding the tags as plain
 *    space-separated text (not pills) plus a full-width white action button.
 *  - The button reads "ADD NEW TAGS"; tapping it reveals an input and switches
 *    the button to "SAVE BATCH". Saving merges the tags and reverts the button.
 *  - IMPORT / EXPORT text actions sit in the screen header.
 *
 * Long-press a batch name to rename; the trailing icons handle search/delete.
 */
@Composable
fun BatchesTabContent(
    batches: List<TagBatch>,
    accentColor: Color,
    onSearchBatch: (TagBatch) -> Unit,
    onAddTags: (TagBatch, List<String>) -> Unit,
    onRemoveTag: (TagBatch, String) -> Unit,
    onRename: (TagBatch, String) -> Unit,
    onDelete: (TagBatch) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // IMPORT / EXPORT row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IMPORT",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onImport() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "EXPORT",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onExport() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (batches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No batches yet.\nSave a set of search tags as a batch to find it here.",
                    color = Color(0xFF8A8D91),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(batches, key = { it.id }) { batch ->
                    BatchBlock(
                        batch = batch,
                        onSearchBatch = { onSearchBatch(batch) },
                        onAddTags = { tags -> onAddTags(batch, tags) },
                        onRemoveTag = { tag -> onRemoveTag(batch, tag) },
                        onRename = { name -> onRename(batch, name) },
                        onDelete = { onDelete(batch) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BatchBlock(
    batch: TagBatch,
    onSearchBatch: () -> Unit,
    onAddTags: (List<String>) -> Unit,
    onRemoveTag: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRemoveMode by remember { mutableStateOf(false) }

    fun commitAdd() {
        val newTags = addText.split(" ", ",", "\t")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (newTags.isNotEmpty()) onAddTags(newTags)
        addText = ""
        adding = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Batch name heading ABOVE the block, with small trailing actions.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = batch.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showRename = true }
            )
            IconButton(onClick = onSearchBatch, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Search, contentDescription = "Search this batch", tint = Color.White)
            }
            IconButton(onClick = { showRemoveMode = !showRemoveMode }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit tags", tint = Color(0xFFB0B3B7))
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete batch", tint = Color(0xFFE57373))
            }
        }

        // The bordered block.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF3A3D42), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tags as plain wrapping text. In remove-mode each tag gets a small ×.
            if (batch.tags.isEmpty()) {
                Text("No tags yet.", color = Color(0xFF6B6E72), fontSize = 14.sp)
            } else if (showRemoveMode) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    batch.tags.forEach { tag ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tag.replace('_', ' '), color = Color(0xFFC8CBCF), fontSize = 15.sp)
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove $tag",
                                tint = Color(0xFFE57373),
                                modifier = Modifier
                                    .padding(start = 3.dp)
                                    .size(15.dp)
                                    .clickable { onRemoveTag(tag) }
                            )
                        }
                    }
                }
            } else {
                // Plain space-separated text that wraps, matching the mockup.
                Text(
                    text = batch.tags.joinToString("   ") { it.replace('_', ' ') },
                    color = Color(0xFFC8CBCF),
                    fontSize = 15.sp,
                    lineHeight = 26.sp
                )
            }

            if (adding) {
                OutlinedTextField(
                    value = addText,
                    onValueChange = { addText = it },
                    placeholder = { Text("Add tags (space separated)", color = Color(0xFF6B6E72)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitAdd() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Full-width white pill button: ADD NEW TAGS <-> SAVE BATCH.
            WhitePillButton(
                text = if (adding) "SAVE BATCH" else "ADD NEW TAGS",
                onClick = { if (adding) commitAdd() else adding = true }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${batch.name}\"?") },
            text = { Text("This batch and its tags will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = Color(0xFFE57373)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showRename) {
        var renameText by remember { mutableStateOf(batch.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename batch") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (renameText.isNotBlank()) { onRename(renameText.trim()); showRename = false }
                    })
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { if (renameText.isNotBlank()) { onRename(renameText.trim()); showRename = false } },
                    enabled = renameText.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}

/** Full-width white pill button with dark text, matching the mockup's buttons. */
@Composable
private fun WhitePillButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF111111),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Dialog to name a new batch when saving from search — styled to the PDF. */
@Composable
fun NameBatchDialog(
    initialTags: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Name this batch",
                color = Color(0xFF111111),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "${initialTags.size} ${if (initialTags.size == 1) "tag" else "tags"} will be saved",
                color = Color(0xFF8A8D91),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.size(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Genshin Impact", color = Color(0xFFB0B3B7)) },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111111),
                    unfocusedTextColor = Color(0xFF111111),
                    focusedBorderColor = Color(0xFF8A8D91),
                    unfocusedBorderColor = Color(0xFFB0B3B7),
                    cursorColor = Color(0xFF111111)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Red Cancel.
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE53935), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text("Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                // Teal Save.
                Box(
                    modifier = Modifier
                        .background(
                            if (name.isNotBlank()) Color(0xFF4DB6A4) else Color(0xFFB0B3B7),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = name.isNotBlank()) { onConfirm(name.trim()) }
                        .padding(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text("Save", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}