package com.example.mediabrowser.domain.model

/** A locally-favorited artist, shown in Favorites > Artists tab. */
data class FavoriteArtist(
    val artistName: String,
    val displayName: String,
    val postCount: Int,
    val savedAt: Long
)
