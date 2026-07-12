package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per user interaction with a post. This is the raw history that feeds
 * the affinity scores. Kept lightweight (a CSV of tags rather than a join table)
 * because we only ever read it to derive affinities, and a WorkManager job prunes
 * rows older than 30 days so this never grows unbounded.
 */
@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val tagsCsv: String,        // space-separated tags from the post
    val artistId: String?,      // artist/creator tag if known, else null
    val type: String,           // InteractionType name: VIEW / SEARCH / FAVORITE / DOWNLOAD / LIKE
    val weight: Int,            // point value of this interaction
    val timestamp: Long         // epoch millis, used for time-decay + cleanup
)