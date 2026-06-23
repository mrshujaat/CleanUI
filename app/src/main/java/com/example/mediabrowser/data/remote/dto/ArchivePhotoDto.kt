package com.example.mediabrowser.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Real Archive API photo response shape: GET /index.php?s=post&q=index&json=1 */
@Serializable
data class ArchivePhotoDto(
    @SerialName("id") val id: Long,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
    @SerialName("tags") val tags: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("preview_url") val previewUrl: String,
    @SerialName("sample_url") val sampleUrl: String,
    @SerialName("score") val score: Int,
    @SerialName("owner") val owner: String? = null,
    @SerialName("rating") val rating: String? = null
)