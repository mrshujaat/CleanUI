package com.example.mediabrowser.domain.model

/**
 * Lightweight representation of a single media post, used in feeds / grids
 * (home, search results, favorites). Keep this model small since hundreds
 * of these may be held in memory at once during paging.
 */
data class Post(
    val id: Long,
    val thumbnailUrl: String,
    val previewUrl: String,
    val sampleUrl: String,
    val fileUrl: String,
    val fileType: MediaType,
    val width: Int,
    val height: Int,
    val score: Int,
    val tags: List<String>,
    val isFavorite: Boolean = false
)

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO
}