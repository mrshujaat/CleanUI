package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_artists")
data class FavoriteArtistEntity(
    @PrimaryKey val artistName: String,
    val displayName: String,
    val postCount: Int = 0,
    val savedAt: Long
)
