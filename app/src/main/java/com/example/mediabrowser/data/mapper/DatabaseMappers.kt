package com.example.mediabrowser.data.mapper

import com.example.mediabrowser.data.local.entity.FavoriteEntity
import com.example.mediabrowser.data.local.entity.DownloadEntity
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.PostDetail
import com.example.mediabrowser.domain.model.DownloadStatus
import com.example.mediabrowser.domain.model.ContentRating
import com.example.mediabrowser.domain.model.TagInfo
import com.example.mediabrowser.domain.model.TagCategory // Import your project's TagCategory enum

// 1. Map Favorite Database Entity back to a UI Post model
fun FavoriteEntity.toDomain(): Post {
    return Post(
        id = this.postId, 
        previewUrl = this.previewUrl,
        thumbnailUrl = this.thumbnailUrl,
        fileUrl = this.fileUrl,
        width = this.width,
        height = this.height,
        score = this.score,
        fileType = MediaType.valueOf(this.fileType), 
        tags = this.tags.split(" ").filter { it.isNotBlank() },
        isFavorite = true
    )
}

// 2. Map a UI Post model into a Favorite Database Entity
fun Post.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        postId = this.id, 
        previewUrl = this.previewUrl,
        thumbnailUrl = this.thumbnailUrl,
        fileUrl = this.fileUrl,
        width = this.width,
        height = this.height,
        score = this.score,
        fileType = this.fileType.name, 
        tags = this.tags.joinToString(" "),
        savedAt = System.currentTimeMillis()
    )
}

// 3. Map Download Database Entity to domain DownloadItem
fun DownloadEntity.toDomain(): DownloadItem {
    return DownloadItem(
        postId = this.postId, 
        fileUrl = this.fileUrl,
        localUri = this.localUri, 
        fileName = this.fileName,
        mediaType = MediaType.valueOf(this.mediaType), 
        status = DownloadStatus.valueOf(this.status),   
        progress = this.progress,
        createdAt = this.createdAt,
        fileSizeBytes = 0L, 
        width = this.width,
        height = this.height,
        score = this.score,
        tags = emptyList() 
    )
}

// 4. Map DownloadItem to explicit PostDetail domain model
fun DownloadItem.toPostDetail(): PostDetail {
    return PostDetail(
        id = this.postId,
        fileUrl = this.localUri ?: this.fileUrl,
        previewUrl = this.localUri ?: this.fileUrl,
        width = this.width,
        height = this.height,
        score = this.score,
        rating = ContentRating.entries.firstOrNull() ?: ContentRating.valueOf("GENERAL"), // Safe fallback dynamically fetching first enum value
        fileType = this.mediaType,
        fileSizeBytes = this.fileSizeBytes,
        favoriteCount = 0,
        source = null,
        uploader = null,
        createdAt = this.createdAt,
        tags = this.tags.map { TagInfo(name = it, category = TagCategory.GENERAL, postCount = 0) } // Maps string category cleanly to Enum property setup
    )
}

// Append to the bottom of DatabaseMappers.kt
fun PostDetail.toFavoriteEntityFromDetail(): FavoriteEntity {
    return FavoriteEntity(
        postId = this.id,
        previewUrl = this.previewUrl,
        thumbnailUrl = this.previewUrl,
        fileUrl = this.fileUrl,
        width = this.width,
        height = this.height,
        score = this.score,
        fileType = this.fileType.name,
        tags = this.tags.joinToString(" ") { it.name },
        savedAt = System.currentTimeMillis()
    )
}