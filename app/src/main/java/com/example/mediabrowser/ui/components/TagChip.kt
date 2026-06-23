package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
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

/**
 * Fully rounded, flat-colored tag pill with a small category icon plus
 * white text, sized for readability (16sp, semi-bold, generous padding).
 */
@Composable
fun TagChip(
    label: String,
    category: TagCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = colorForCategory(category)
    val pillShape = RoundedCornerShape(50)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        // FIXED: Re-ordered modifiers to clip the touch ripples cleanly to the rounded background
        // and expand the active hit box across the entire padding boundary.
        modifier = modifier
            .padding(end = 10.dp, bottom = 10.dp) // 1. Outer margins
            .clip(pillShape)                      // 2. Bound interaction ripples
            .background(color, pillShape)         // 3. Render color fill
            .clickable(onClick = onClick)         // 4. Capture touch events fully
            .padding(horizontal = 18.dp, vertical = 12.dp) // 5. Safe inner text spacing
    ) {
        Icon(
            imageVector = iconForCategory(category),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun colorForCategory(category: TagCategory): Color = when (category) {
    TagCategory.ARTIST -> Color(0xFF5C7DB8)      // muted blue
    TagCategory.CHARACTER -> Color(0xFF6FA37A)   // muted green
    TagCategory.COPYRIGHT -> Color(0xFF8E6FB5)   // muted purple
    TagCategory.META -> Color(0xFFB57550)        // muted orange/brown
    TagCategory.GENERAL -> Color(0xFF3D4654)     // muted slate gray
}

fun iconForCategory(category: TagCategory): ImageVector = when (category) {
    TagCategory.ARTIST -> Icons.Filled.Edit
    TagCategory.CHARACTER -> Icons.Filled.Category
    TagCategory.COPYRIGHT -> Icons.Filled.Copyright
    TagCategory.META -> Icons.Filled.Info
    TagCategory.GENERAL -> Icons.Filled.Sell
}