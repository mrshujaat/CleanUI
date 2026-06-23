package com.example.mediabrowser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mediabrowser.data.local.dao.DownloadDao
import com.example.mediabrowser.data.local.dao.FavoriteArtistDao
import com.example.mediabrowser.data.local.dao.FavoriteDao
import com.example.mediabrowser.data.local.dao.FavoriteTagDao
import com.example.mediabrowser.data.local.entity.DownloadEntity
import com.example.mediabrowser.data.local.entity.FavoriteArtistEntity
import com.example.mediabrowser.data.local.entity.FavoriteEntity
import com.example.mediabrowser.data.local.entity.FavoriteTagEntity

@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        FavoriteArtistEntity::class,
        FavoriteTagEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteArtistDao(): FavoriteArtistDao
    abstract fun favoriteTagDao(): FavoriteTagDao

    companion object {
        const val DATABASE_NAME = "media_browser.db"
    }
}