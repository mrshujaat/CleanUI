package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tags")
data class FavoriteTagEntity(
    @PrimaryKey val tagName: String,
    val displayName: String,
    val category: String,
    val postCount: Int = 0,
    val savedAt: Long
)
