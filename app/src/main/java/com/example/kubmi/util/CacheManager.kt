package com.example.kubmi.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun clearAllCache() {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                deleteDir(cacheDir)
            }
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
    
    fun clearImageCache() {
        try {
            val imageCacheDir = File(context.cacheDir, "images")
            if (imageCacheDir.exists()) {
                deleteDir(imageCacheDir)
            }
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
    
    fun getCacheSize(): Long {
        return getDirSize(context.cacheDir)
    }
    
    fun clearWebViewCache() {
        try {
            val webViewCacheDir = File(context.cacheDir, "webview")
            if (webViewCacheDir.exists()) {
                deleteDir(webViewCacheDir)
            }
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
    
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }
    
    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getDirSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        }
        return size
    }
}