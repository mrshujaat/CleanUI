package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val postId: Long,
    val thumbnailUrl: String,
    val previewUrl: String,
    val fileUrl: String,
    val fileType: String,
    val width: Int,
    val height: Int,
    val score: Int,
    val tags: String,
    val savedAt: Long
)
