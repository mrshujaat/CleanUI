package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Popup menu shown when a tag or artist chip is tapped. Restyled with a
 * dark rounded surface, outlined icons, and clear label typography.
 */
@Composable
fun TagActionMenu(
    expanded: Boolean,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onAddToSearch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: (() -> Unit)? = null
) {
    val menuShape = RoundedCornerShape(16.dp)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // FIXED: Added explicit clip boundary layer before applying background 
        // to prevent item focus states and click ripples from breaking out of rounded corners.
        modifier = Modifier
            .clip(menuShape)
            .background(Color(0xFF1A1D1F), menuShape)
    ) {
        MenuRow(
            icon = Icons.Outlined.OpenInNew,
            label = "Open in new tab",
            onClick = { onOpenInNewTab(); onDismiss() }
        )
        MenuRow(
            icon = Icons.Outlined.Add,
            label = "Add to search",
            onClick = { onAddToSearch(); onDismiss() }
        )
        MenuRow(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            label = if (isFavorite) "Remove favourite" else "Favourite",
            iconTint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
            onClick = { onToggleFavorite(); onDismiss() }
        )
        if (onCopy != null) {
            MenuRow(
                icon = Icons.Outlined.ContentCopy,
                label = "Copy tag",
                onClick = { onCopy(); onDismiss() }
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = iconTint)
        },
        onClick = onClick
    )
}