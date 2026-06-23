package com.example.mediabrowser.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw wire-format model for a post from the Archive API list/search endpoint.
 * Aligns perfectly with your existing schema while mapping the new API keys.
 */
@Serializable
data class ArchivePostDto(
    @SerialName("id") val id: Long,
    @SerialName("preview_url") val previewUrl: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("sample_url") val sampleUrl: String,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
    @SerialName("score") val score: Int,
    @SerialName("tags") val tags: String,
    @SerialName("directory") val directory: String? = null,
    @SerialName("image") val image: String? = null
)