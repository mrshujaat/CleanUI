package com.example.mediabrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mediabrowser.data.local.entity.FavoriteTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTagDao {

    @Query("SELECT * FROM favorite_tags ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteTagEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tags WHERE tagName = :tagName)")
    suspend fun isFavorite(tagName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: FavoriteTagEntity)

    @Query("DELETE FROM favorite_tags WHERE tagName = :tagName")
    suspend fun deleteByName(tagName: String)
}
