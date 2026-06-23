package com.example.mediabrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mediabrowser.data.local.entity.FavoriteArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteArtistDao {

    @Query("SELECT * FROM favorite_artists ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteArtistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_artists WHERE artistName = :artistName)")
    suspend fun isFavorite(artistName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artist: FavoriteArtistEntity)

    @Query("DELETE FROM favorite_artists WHERE artistName = :artistName")
    suspend fun deleteByName(artistName: String)
}
