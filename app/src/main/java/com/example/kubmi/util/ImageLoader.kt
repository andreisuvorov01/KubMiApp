package com.example.kubmi.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing a configured Coil ImageLoader with proper caching.
 * 
 * This implementation follows the official Coil documentation for best practices:
 * https://coil-kt.github.io/coil/getting_started/
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    /**
     * Provides a singleton ImageLoader with optimized caching configuration.
     * 
     * Features:
     * - Memory cache with 25% of available memory
     * - Disk cache with 50MB limit
     * - SVG support
     * - Proper cache policies
     * 
     * @param context Application context for cache directories
     * @return Configured ImageLoader instance
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .crossfade(true) // Enable crossfade animation
            .allowHardware(true) // Allow hardware bitmaps for better performance
            .respectCacheHeaders(false) // Use our cache policy instead of HTTP headers
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .components {
                add(SvgDecoder.Factory())
            }
            .logger(DebugLogger())
            .build()
    }
}