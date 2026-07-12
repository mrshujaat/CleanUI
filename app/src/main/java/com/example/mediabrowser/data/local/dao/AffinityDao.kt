package com.example.mediabrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mediabrowser.data.local.entity.ArtistAffinityEntity
import com.example.mediabrowser.data.local.entity.TagAffinityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the derived affinity scores. The feed only ever calls the
 * `topTags` / `topArtists` reads, which are a single indexed ORDER BY — instant.
 * Score updates go through `upsertTag` / `upsertArtist`, called off the main
 * thread whenever an interaction is recorded.
 */
@Dao
interface AffinityDao {

    // --- Tags ---

    @Query("SELECT * FROM tag_affinity WHERE tag = :tag")
    suspend fun getTag(tag: String): TagAffinityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(entity: TagAffinityEntity)

    @Query("SELECT * FROM tag_affinity ORDER BY score DESC LIMIT :limit")
    suspend fun topTags(limit: Int): List<TagAffinityEntity>

    @Query("SELECT * FROM tag_affinity ORDER BY score DESC")
    fun observeTopTags(): Flow<List<TagAffinityEntity>>

    @Query("SELECT * FROM tag_affinity")
    suspend fun allTags(): List<TagAffinityEntity>

    // --- Artists ---

    @Query("SELECT * FROM artist_affinity WHERE artist = :artist")
    suspend fun getArtist(artist: String): ArtistAffinityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtist(entity: ArtistAffinityEntity)

    @Query("SELECT * FROM artist_affinity ORDER BY score DESC LIMIT :limit")
    suspend fun topArtists(limit: Int): List<ArtistAffinityEntity>

    @Query("SELECT * FROM artist_affinity")
    suspend fun allArtists(): List<ArtistAffinityEntity>

    // --- Maintenance / backup ---

    @Query("DELETE FROM tag_affinity")
    suspend fun clearTags()

    @Query("DELETE FROM artist_affinity")
    suspend fun clearArtists()
}