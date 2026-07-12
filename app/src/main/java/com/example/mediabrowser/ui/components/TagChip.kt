package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediabrowser.domain.model.TagCategory
import com.example.mediabrowser.ui.theme.TagArtistDefault
import com.example.mediabrowser.ui.theme.TagCharacterDefault
import com.example.mediabrowser.ui.theme.TagCopyrightDefault
import com.example.mediabrowser.ui.theme.TagGeneralDefault
import com.example.mediabrowser.ui.theme.TagMetaDefault

/**
 * Solid-fill pill matching the redesign PDF: a fully-saturated category color
 * background with white text, fully rounded. No icon (the PDF pills are clean
 * solid pills). Colors come straight from the PDF-matched palette in Color.kt.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TagChip(
    label: String,
    category: TagCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val fill = backgroundColorForCategory(category)
    val pillShape = RoundedCornerShape(50)
    // White text on dark fills, near-black on light fills (e.g. the yellow
    // artist pill), matching the PDF and keeping text readable.
    val textColor = if (isLightColor(fill)) Color(0xFF111111) else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .clip(pillShape)
            .background(fill, pillShape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Relative luminance check — true if a color is light enough to need dark text. */
fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.6
}

/**
 * Text/foreground color. With solid pills the text is white, but some callers
 * use this for non-pill contexts (e.g. colored section labels), so it returns
 * the category's vivid color.
 */
@Composable
fun foregroundColorForCategory(category: TagCategory): Color = when (category) {
    TagCategory.ARTIST -> TagArtistDefault
    TagCategory.CHARACTER -> TagCharacterDefault
    TagCategory.COPYRIGHT -> TagCopyrightDefault
    TagCategory.META -> TagMetaDefault
    TagCategory.GENERAL -> TagGeneralDefault
}

/** Solid pill fill — the exact PDF category colors. */
@Composable
fun backgroundColorForCategory(category: TagCategory): Color = when (category) {
    TagCategory.ARTIST -> TagArtistDefault
    TagCategory.CHARACTER -> TagCharacterDefault
    TagCategory.COPYRIGHT -> TagCopyrightDefault
    TagCategory.META -> TagMetaDefault
    TagCategory.GENERAL -> TagGeneralDefault
}

/** Kept for backward compatibility with any call sites using a single solid color. */
@Composable
fun colorForCategory(category: TagCategory): Color = backgroundColorForCategory(category)

fun iconForCategory(category: TagCategory): ImageVector = when (category) {
    TagCategory.ARTIST -> Icons.Filled.Edit
    TagCategory.CHARACTER -> Icons.Filled.Category
    TagCategory.COPYRIGHT -> Icons.Filled.Copyright
    TagCategory.META -> Icons.Filled.Info
    TagCategory.GENERAL -> Icons.Filled.Sell
}