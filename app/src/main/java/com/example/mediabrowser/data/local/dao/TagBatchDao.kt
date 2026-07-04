package com.example.mediabrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mediabrowser.data.local.entity.TagBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagBatchDao {

    @Query("SELECT * FROM tag_batches ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TagBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: TagBatchEntity): Long

    @Update
    suspend fun update(batch: TagBatchEntity)

    @Delete
    suspend fun delete(batch: TagBatchEntity)

    @Query("DELETE FROM tag_batches WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tag_batches WHERE id = :id")
    suspend fun getById(id: Long): TagBatchEntity?

    /** Used by import to replace the whole set in one shot. */
    @Query("DELETE FROM tag_batches")
    suspend fun clearAll()
}