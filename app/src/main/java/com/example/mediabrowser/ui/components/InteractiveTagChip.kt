package com.example.mediabrowser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mediabrowser.domain.model.TagCategory

/**
 * A [TagChip] that, when tapped, opens [TagActionMenu] instead of
 * immediately navigating — matching the "tap opens menu first" behavior
 * decided for tag/artist interactions throughout the app.
 */
@Composable
fun InteractiveTagChip(
    label: String,
    category: TagCategory,
    isFavorite: Boolean,
    onOpenInNewTab: () -> Unit,
    onAddToSearch: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // FIXED: Chained the incoming modifier to the Box so layout positions and margins 
    // are preserved, ensuring the DropdownMenu anchor resolves to the proper screen coordinates.
    Box(modifier = modifier) {
        TagChip(
            label = label,
            category = category,
            onClick = { menuExpanded = true }
        )
        TagActionMenu(
            expanded = menuExpanded,
            isFavorite = isFavorite,
            onDismiss = { menuExpanded = false },
            onOpenInNewTab = onOpenInNewTab,
            onAddToSearch = onAddToSearch,
            onToggleFavorite = onToggleFavorite
        )
    }
}