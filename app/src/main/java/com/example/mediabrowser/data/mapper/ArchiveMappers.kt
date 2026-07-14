package com.example.mediabrowser.data.mapper

import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post

/**
 * @param cdnBase When non-blank AND the API returned blank URLs, construct the
 *   image URLs from directory+image (TBIB/Realbooru/etc. only send those, which
 *   is why their grids were all grey boxes — file_url came back empty). Standard
 *   Gelbooru path layout: full at /images/<dir>/<image>, thumb at
 *   /thumbnails/<dir>/thumbnail_<imageBaseName>.jpg.
 */
fun ArchivePostDto.toPost(isFavorite: Boolean, cdnBase: String = ""): Post {
    val tagList = this.tags.split(" ").filter { it.isNotBlank() }

    // Construct URLs from directory+image when the API omits full URLs.
    val constructedFull: String
    val constructedPreview: String
    if (cdnBase.isNotBlank() && this.fileUrl.isBlank() && !this.image.isNullOrBlank() && this.directory != null) {
        val base = cdnBase.trimEnd('/')
        val dir = this.directory
        val img = this.image
        val thumbBase = img.substringBeforeLast('.')
        constructedFull = "$base/images/$dir/$img"
        // Booru thumbnails are jpg regardless of the source extension.
        constructedPreview = "$base/thumbnails/$dir/thumbnail_$thumbBase.jpg"
    } else {
        constructedFull = ""
        constructedPreview = ""
    }

    // MediaType from the actual filename (fileUrl, or the raw `image` field).
    val filename = (this.fileUrl.ifBlank { this.image.orEmpty() }).lowercase()
    val isVideo = filename.endsWith(".mp4") || filename.endsWith(".webm")
    val isGif = filename.endsWith(".gif")

    // Robust URL selection with construction fallback.
    val effectiveFile = this.fileUrl.ifBlank { constructedFull }
    val effectivePreview = this.previewUrl
        .ifBlank { constructedPreview }
        .ifBlank { this.sampleUrl }
        .ifBlank { effectiveFile }
    val effectiveSample = this.sampleUrl.ifBlank { effectiveFile.ifBlank { effectivePreview } }

    return Post(
        id = this.id,
        previewUrl = effectivePreview,
        thumbnailUrl = effectivePreview,
        sampleUrl = effectiveSample,
        fileUrl = effectiveFile,
        width = this.width,
        height = this.height,
        score = this.score,
        fileType = when {
            isVideo -> MediaType.VIDEO
            isGif -> MediaType.GIF
            else -> MediaType.IMAGE
        },
        tags = tagList,
        isFavorite = isFavorite
    )
}
