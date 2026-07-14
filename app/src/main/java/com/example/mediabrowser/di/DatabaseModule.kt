package com.example.mediabrowser.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.mediabrowser.data.local.AppDatabase
import com.example.mediabrowser.data.local.dao.DownloadDao
import com.example.mediabrowser.data.local.dao.FavoriteArtistDao
import com.example.mediabrowser.data.local.dao.FavoriteDao
import com.example.mediabrowser.data.local.dao.FavoriteTagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideFavoriteArtistDao(db: AppDatabase): FavoriteArtistDao = db.favoriteArtistDao()

    @Provides
    fun provideFavoriteTagDao(db: AppDatabase): FavoriteTagDao = db.favoriteTagDao()

    @Provides
    fun provideTagBatchDao(db: AppDatabase): com.example.mediabrowser.data.local.dao.TagBatchDao =
        db.tagBatchDao()

    @Provides
    fun provideInteractionDao(db: AppDatabase): com.example.mediabrowser.data.local.dao.InteractionDao =
        db.interactionDao()

    @Provides
    fun provideAffinityDao(db: AppDatabase): com.example.mediabrowser.data.local.dao.AffinityDao =
        db.affinityDao()

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        // Lets Coil decode a frame from video files (local .mp4) so downloaded
        // videos can show a real thumbnail. Requires the coil-video dependency.
        .components {
            add(coil.decode.VideoFrameDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder(context)
                // Large memory cache so images that scrolled off-screen stay decoded
                // and reappear instantly when you scroll back (the main complaint).
                .maxSizePercent(0.50)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(2048L * 1024 * 1024)
                .build()
        }
        // Don't re-validate cached images against the server — serve straight from
        // cache. Without this, fast scrolling fires conditional requests that get
        // cancelled mid-flight, leaving cells blank. This is the key fix.
        .respectCacheHeaders(false)
        // RGB_565 uses half the memory of ARGB_8888, so roughly twice as many images
        // fit in the memory cache. For opaque photos the quality difference is minimal.
        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
        .crossfade(200)
        // Speed knobs — the biggest levers on scroll-time responsiveness:
        //  - dispatcher(Dispatchers.IO): more threads decoding in parallel
        //  - fetcherDispatcher/decoderDispatcher: dedicated IO for network/decode
        //  - allowHardware(true): hardware bitmaps skip CPU copy on draw
        .dispatcher(kotlinx.coroutines.Dispatchers.IO)
        .fetcherDispatcher(kotlinx.coroutines.Dispatchers.IO)
        .decoderDispatcher(kotlinx.coroutines.Dispatchers.Default)
        .allowHardware(true)
        // Debug logger was leaking CPU on every decode; off for release feel.
        .build()
}