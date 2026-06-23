package com.example.mediabrowser.data.mapper

import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post

fun ArchivePostDto.toPost(isFavorite: Boolean): Post {
    val tagList = this.tags.split(" ").filter { it.isNotBlank() }
    
    val isVideo = this.fileUrl.endsWith(".mp4", ignoreCase = true) || 
                  this.fileUrl.endsWith(".webm", ignoreCase = true)

    return Post(
        id = this.id,
        previewUrl = this.previewUrl,
        thumbnailUrl = this.previewUrl, // Maps previewUrl directly into your required thumbnailUrl field
        fileUrl = this.fileUrl,
        width = this.width,
        height = this.height,
        score = this.score,
        fileType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
        tags = tagList,
        isFavorite = isFavorite
    )
}