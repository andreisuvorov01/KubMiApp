package com.example.kubmi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfToImageConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Скачивает PDF по ссылке, конвертирует каждую страницу в JPG 
     * и возвращает список путей к созданным изображениям.
     * Если изображения уже существуют, возвращает их без повторной конвертации.
     */
    suspend fun convertPdfUrlToImages(pdfUrl: String): List<String> = withContext(Dispatchers.IO) {
        val urlHash = pdfUrl.hashCode().toString()
        val outputDir = File(context.filesDir, "screensaver_slides").apply { mkdirs() }
        
        // Быстрая проверка: если файлы уже есть, возвращаем их
        val existingFiles = outputDir.listFiles { _, name ->
            name.startsWith("slide_${urlHash}_") && name.endsWith(".jpg")
        }?.sortedBy { file ->
            file.name.substringAfterLast("_").substringBefore(".").toIntOrNull() ?: 0
        }
        
        if (!existingFiles.isNullOrEmpty()) {
            Log.d("ScreensaverDebug", "Found ${existingFiles.size} existing slides for $pdfUrl")
            return@withContext existingFiles.map { it.absolutePath }
        }

        Log.d("ScreensaverDebug", "Downloading and converting PDF: $pdfUrl")
        val imagePaths = mutableListOf<String>()
        var tempFile: File? = null
        
        try {
            // 1. Создаем временный файл для PDF
            tempFile = File.createTempFile("temp_news_", ".pdf", context.cacheDir)
            
            // 2. Скачиваем PDF
            val request = Request.Builder().url(pdfUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ScreensaverDebug", "Failed to download PDF: ${response.code} for $pdfUrl")
                    throw IOException("Failed to download PDF: ${response.code}")
                }
                
                response.body?.byteStream()?.use { input ->
                    tempFile!!.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if ((tempFile?.length() ?: 0L) == 0L) {
                Log.e("ScreensaverDebug", "Downloaded PDF is empty: $pdfUrl")
                throw IOException("Downloaded PDF is empty")
            }

            Log.d("ScreensaverDebug", "PDF downloaded, size: ${tempFile?.length()} bytes")

            // 3. Рендерим страницы
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            
            Log.d("ScreensaverDebug", "PdfRenderer opened, pages: ${renderer.pageCount}")

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                try {
                    // Calculate scale to reach 4K width (3840px)
                    val targetWidth = 3840
                    val scale = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scale).toInt()

                    Log.d("ScreensaverDebug", "Rendering PDF page $i at 4K resolution: ${targetWidth}x$targetHeight")

                    val bitmap = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val imageFile = File(outputDir, "slide_${urlHash}_$i.jpg")
                    val success = FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    
                    if (success && imageFile.exists() && imageFile.length() > 0) {
                        imagePaths.add(imageFile.absolutePath)
                        Log.d("ScreensaverDebug", "Page $i converted to ${imageFile.absolutePath}")
                    } else {
                        Log.e("ScreensaverDebug", "Failed to save rendered PDF page $i for $pdfUrl")
                        Timber.e("Failed to save rendered PDF page $i for $pdfUrl")
                    }
                    bitmap.recycle()
                } finally {
                    page.close()
                }
            }
            
            renderer.close()
            pfd.close()
            
        } catch (e: Exception) {
            Log.e("ScreensaverDebug", "Error in convertPdfUrlToImages for $pdfUrl", e)
            Timber.e(e, "Error converting PDF to images: $pdfUrl")
        } finally {
            tempFile?.delete()
        }
        
        return@withContext imagePaths
    }

    /**
     * Удаляет старые слайды, которые больше не нужны.
     * @param keepHashes список хешей URL, которые нужно оставить
     */
    fun cleanOldSlides(keepHashes: Set<String>) {
        val outputDir = File(context.filesDir, "screensaver_slides")
        if (!outputDir.exists()) return
        
        outputDir.listFiles { _, name ->
            if (!name.startsWith("slide_") || !name.endsWith(".jpg")) return@listFiles false
            val hash = name.substringAfter("slide_").substringBefore("_")
            !keepHashes.contains(hash)
        }?.forEach { it.delete() }
    }

    /**
     * Очистка всех слайдов
     */
    fun clearCache() {
        val outputDir = File(context.filesDir, "screensaver_slides")
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
    }
}
