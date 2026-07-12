package com.example.mediabrowser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mediabrowser.data.local.dao.AffinityDao
import com.example.mediabrowser.data.local.dao.DownloadDao
import com.example.mediabrowser.data.local.dao.FavoriteArtistDao
import com.example.mediabrowser.data.local.dao.FavoriteDao
import com.example.mediabrowser.data.local.dao.FavoriteTagDao
import com.example.mediabrowser.data.local.dao.InteractionDao
import com.example.mediabrowser.data.local.dao.TagBatchDao
import com.example.mediabrowser.data.local.entity.ArtistAffinityEntity
import com.example.mediabrowser.data.local.entity.DownloadEntity
import com.example.mediabrowser.data.local.entity.FavoriteArtistEntity
import com.example.mediabrowser.data.local.entity.FavoriteEntity
import com.example.mediabrowser.data.local.entity.FavoriteTagEntity
import com.example.mediabrowser.data.local.entity.InteractionEntity
import com.example.mediabrowser.data.local.entity.TagAffinityEntity
import com.example.mediabrowser.data.local.entity.TagBatchEntity

@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        FavoriteArtistEntity::class,
        FavoriteTagEntity::class,
        TagBatchEntity::class,
        InteractionEntity::class,
        TagAffinityEntity::class,
        ArtistAffinityEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteArtistDao(): FavoriteArtistDao
    abstract fun favoriteTagDao(): FavoriteTagDao
    abstract fun tagBatchDao(): TagBatchDao
    abstract fun interactionDao(): InteractionDao
    abstract fun affinityDao(): AffinityDao

    companion object {
        const val DATABASE_NAME = "media_browser.db"
    }
}