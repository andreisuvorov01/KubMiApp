package com.example.kubmi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProcessingUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "ImageProcessingUtils"
    private val TARGET_WIDTH = 3840 // 4K width

    /**
     * Downloads an image, upscales it to 4K if needed, and saves it locally.
     * Returns the local file path with file:// scheme or null if failed.
     */
    suspend fun downloadAndProcessImage(imageUrl: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "news_images").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        // If file already exists, return it
        if (outputFile.exists() && outputFile.length() > 0) {
            return@withContext "file://${outputFile.absolutePath}"
        }

        try {
            Log.d(TAG, "Downloading image: $imageUrl")
            val request = Request.Builder().url(imageUrl).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to download image: ${response.code}")
                return@withContext null
            }

            val inputStream: InputStream = response.body?.byteStream() ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null

            // Upscale to 4K
            val processedBitmap = upscaleTo4K(originalBitmap)
            
            // Save to local storage
            FileOutputStream(outputFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (originalBitmap != processedBitmap) {
                originalBitmap.recycle()
            }
            processedBitmap.recycle()

            Log.d(TAG, "Successfully processed and saved image: ${outputFile.absolutePath}")
            return@withContext "file://${outputFile.absolutePath}"

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image $imageUrl: ${e.message}")
            return@withContext null
        }
    }

    private fun upscaleTo4K(original: Bitmap): Bitmap {
        if (original.width >= TARGET_WIDTH) return original

        val aspectRatio = original.height.toFloat() / original.width.toFloat()
        val targetHeight = (TARGET_WIDTH * aspectRatio).toInt()

        Log.d(TAG, "Upscaling image from ${original.width}x${original.height} to ${TARGET_WIDTH}x$targetHeight")
        
        return Bitmap.createScaledBitmap(original, TARGET_WIDTH, targetHeight, true)
    }

    /**
     * Cleans up images that are no longer needed.
     */
    fun cleanOldImages(keepFileNames: Set<String>) {
        val outputDir = File(context.filesDir, "news_images")
        if (!outputDir.exists()) return

        outputDir.listFiles()?.forEach { file ->
            if (!keepFileNames.contains(file.name)) {
                Log.d(TAG, "Deleting old news image: ${file.name}")
                file.delete()
            }
        }
    }
}
