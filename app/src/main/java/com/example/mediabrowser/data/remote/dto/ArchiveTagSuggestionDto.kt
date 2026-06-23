package com.example.mediabrowser.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveTagSuggestionDto(
    @SerialName("name") val name: String,
    @SerialName("type") val category: String = "general",
    @SerialName("count") val postCount: Int = 0
)