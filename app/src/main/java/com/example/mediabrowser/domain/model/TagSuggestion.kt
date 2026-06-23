package com.example.mediabrowser.domain.model

data class TagSuggestion(
    val name: String,
    val displayName: String,
    val category: TagCategory,
    val postCount: Int
)
