package com.example.mediabrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mediabrowser.data.local.entity.InteractionEntity

@Dao
interface InteractionDao {

    @Insert
    suspend fun insert(interaction: InteractionEntity)

    /** Delete history older than the given cutoff (used by the 30-day cleanup worker). */
    @Query("DELETE FROM interactions WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM interactions")
    suspend fun count(): Int

    @Query("DELETE FROM interactions")
    suspend fun clearAll()
}