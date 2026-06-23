package com.example.mediabrowser.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Real Archive API post detail response shape */
@Serializable
data class ArchivePostDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("preview_url") val previewUrl: String,
    @SerialName("sample_url") val sampleUrl: String,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
    @SerialName("score") val score: Int,
    @SerialName("rating") val rating: String,
    @SerialName("owner") val owner: String? = null,
    @SerialName("tags") val tags: String // Space-separated string from the JSON response
)