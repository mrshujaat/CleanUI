package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val fileUrl: String,
    val fileName: String,
    val localUri: String?,
    val mediaType: String,
    val status: String,
    val progress: Int = 0,
    val createdAt: Long,
    val errorMessage: String? = null,
    val fileSizeBytes: Long = 0,
    // Snapshot of the original post's metadata, so tapping a downloaded
    // item later can open the same full-featured detail modal (tags,
    // artist, favorite toggle) without needing the original Post in memory.
    val width: Int = 0,
    val height: Int = 0,
    val score: Int = 0,
    val tagsSnapshot: String = "" // space-delimited, same format as Post.tags
)