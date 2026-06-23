package com.example.mediabrowser.domain.model

/** A locally-favorited tag (non-artist), shown in Favorites > Tags tab. */
data class FavoriteTag(
    val tagName: String,
    val displayName: String,
    val category: TagCategory,
    val postCount: Int,
    val savedAt: Long
)
