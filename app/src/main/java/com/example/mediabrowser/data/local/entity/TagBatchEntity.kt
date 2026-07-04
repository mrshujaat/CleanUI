package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named batch of tags — the "My Poison" feature. Users save a set of search
 * tags under a name, then later re-run that whole search with one tap, or keep
 * adding tags to it over time.
 *
 * Tags are stored as a single space-delimited string (the same convention the
 * rest of the app uses for tag lists), since Room can't persist a List directly
 * without a type converter and this keeps the schema simple.
 */
@Entity(tableName = "tag_batches")
data class TagBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tags: String,
    val createdAt: Long,
    val updatedAt: Long
)