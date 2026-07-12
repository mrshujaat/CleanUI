package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Derived affinity score for a single tag. Updated incrementally every time the
 * user interacts with a post carrying this tag, so generating the feed never has
 * to recompute anything — it just reads the top rows by score.
 *
 * `score` is time-decayed: when we add a new interaction we also decay the old
 * score toward zero, so recent taste outweighs stale taste without us having to
 * store and replay the whole history.
 */
@Entity(tableName = "tag_affinity")
data class TagAffinityEntity(
    @PrimaryKey val tag: String,
    val score: Double,
    val lastUpdated: Long
)