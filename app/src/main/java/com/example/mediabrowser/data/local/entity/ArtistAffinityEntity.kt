package com.example.mediabrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Derived affinity score for a single artist/creator. Same incremental,
 * time-decayed model as TagAffinityEntity. Used by the hybrid feed to pull posts
 * from the artists the user engages with most.
 */
@Entity(tableName = "artist_affinity")
data class ArtistAffinityEntity(
    @PrimaryKey val artist: String,
    val score: Double,
    val lastUpdated: Long
)