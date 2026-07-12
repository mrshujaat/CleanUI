package com.example.mediabrowser.domain.model

/**
 * Domain representation of a saved tag batch ("My Poison").
 * [tags] is the parsed list; the entity stores it space-delimited.
 */
data class TagBatch(
    val id: Long,
    val name: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)