package com.example.mediabrowser.domain.model

data class TagSuggestion(
    val name: String,
    val displayName: String,
    val category: TagCategory,
    val postCount: Int,
    /**
     * True for results from the native autocomplete endpoint (the r34.app-style
     * ranked matches) — these render in orange and appear first. False for the
     * supplementary prefix-match results (cat → cat_girl, cat_ears, …).
     */
    val isPrimary: Boolean = false
)