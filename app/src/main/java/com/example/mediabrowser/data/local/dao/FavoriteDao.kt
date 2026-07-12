package com.example.mediabrowser.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mediabrowser.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun pagingSource(): PagingSource<Int, FavoriteEntity>

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT postId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE postId = :postId)")
    suspend fun isFavorite(postId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE postId = :postId")
    suspend fun deleteById(postId: Long)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
