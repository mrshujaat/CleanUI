package com.example.mediabrowser.domain.model

/**
 * Full detail payload shown on the Post Detail modal/screen. Superset of
 * [Post] with additional metadata (source, uploader, structured tags).
 */
data class PostDetail(
    val id: Long,
    val fileUrl: String,
    val previewUrl: String,
    val fileType: MediaType,
    val fileSizeBytes: Long?,
    val width: Int,
    val height: Int,
    val score: Int,
    val favoriteCount: Int,
    val rating: ContentRating,
    val source: String?,
    val uploader: String?,
    val createdAt: Long,
    val tags: List<TagInfo>,
    val relatedPostIds: List<Long> = emptyList(),
    val isFavorite: Boolean = false
)

enum class ContentRating {
    GENERAL,
    SENSITIVE,
    RESTRICTED,
    UNKNOWN
}

/**
 * A tag with its category, used for color-coded tag chips in the UI.
 * Type/classification (AI generated, 2D, 3D, etc.) is represented as a
 * META-category tag rather than a separate field, by design.
 */
data class TagInfo(
    val name: String,
    val category: TagCategory,
    val postCount: Int = 0
)

enum class TagCategory {
    GENERAL,
    ARTIST,
    CHARACTER,
    COPYRIGHT,
    META
}
