package com.example.kubmi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import kotlin.math.min

@Singleton
class PdfToImageConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PdfToImageConverter"
        private const val MAX_PARALLEL_RENDERS = 4
        private const val TARGET_WIDTH = 3840  // 4K resolution
        private const val CACHE_DIR = "screensaver_slides"
        private const val HIGH_QUALITY = 90
        private const val MEDIUM_QUALITY = 75
        private const val LOW_QUALITY = 60
    }
    
    /**
     * Скачивает PDF по ссылке, конвертирует каждую страницу в JPG с параллельной обработкой
     * и возвращает список путей к созданным изображениям.
     * Если изображения уже существуют, возвращает их без повторной конвертации.
     * 
     * Оптимизации:
     * - Параллельный рендер до 4 страниц одновременно
     * - Адаптивное сжатие в зависимости от размера
     * - Кэширование результатов
     * - Быстрый рендер для предпросмотра
     */
    suspend fun convertPdfUrlToImages(pdfUrl: String, fastMode: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val urlHash = pdfUrl.hashCode().toString()
        val outputDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        
        // Быстрая проверка: если файлы уже есть, возвращаем их
        val existingFiles = outputDir.listFiles { _, name ->
            name.startsWith("slide_${urlHash}_") && name.endsWith(".jpg")
        }?.sortedBy { file ->
            file.name.substringAfterLast("_").substringBefore(".").toIntOrNull() ?: 0
        }
        
        if (!existingFiles.isNullOrEmpty()) {
            Log.d(TAG, "✓ Found ${existingFiles.size} cached slides for $pdfUrl")
            return@withContext existingFiles.map { it.absolutePath }
        }

        Log.d(TAG, "→ Downloading and converting PDF: $pdfUrl")
        val imagePaths = mutableListOf<String>()
        var tempFile: File? = null
        
        try {
            // 1. Скачиваем PDF
            tempFile = File.createTempFile("temp_news_", ".pdf", context.cacheDir)
            val downloadTimeStart = System.currentTimeMillis()
            
            val request = Request.Builder().url(pdfUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "✗ Failed to download PDF: ${response.code} for $pdfUrl")
                    throw IOException("Failed to download PDF: ${response.code}")
                }
                
                response.body?.byteStream()?.use { input ->
                    tempFile!!.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if ((tempFile?.length() ?: 0L) == 0L) {
                Log.e(TAG, "✗ Downloaded PDF is empty: $pdfUrl")
                throw IOException("Downloaded PDF is empty")
            }

            val downloadTime = System.currentTimeMillis() - downloadTimeStart
            val fileSize = tempFile?.length() ?: 0L
            Log.d(TAG, "✓ PDF downloaded (${fileSize / 1024}KB) in ${downloadTime}ms")

            // 2. Открываем PDF и определяем качество на основе размера
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            
            val pageCount = renderer.pageCount
            val quality = determineQuality(fileSize, pageCount, fastMode)
            Log.d(TAG, "→ Converting $pageCount pages at quality=$quality (fast=$fastMode)")

            // 3. Рендерим страницы параллельно
            val renderTimeStart = System.currentTimeMillis()
            val renderJobs = (0 until pageCount).map { pageIndex ->
                async {
                    try {
                        renderPageToImage(
                            pageIndex = pageIndex,
                            renderer = renderer,
                            outputDir = outputDir,
                            urlHash = urlHash,
                            quality = quality,
                            fastMode = fastMode
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to render page $pageIndex", e)
                        null
                    }
                }
            }
            
            // Ждём завершения всех рендеров, обрабатываем максимум 4 одновременно
            val results = renderJobs.windowed(MAX_PARALLEL_RENDERS, MAX_PARALLEL_RENDERS, partialWindows = true)
                .flatMap { batch ->
                    batch.awaitAll()
                }.filterNotNull()
            
            imagePaths.addAll(results)
            
            val renderTime = System.currentTimeMillis() - renderTimeStart
            Log.d(TAG, "✓ Converted ${imagePaths.size}/$pageCount pages in ${renderTime}ms (avg ${renderTime / pageCount}ms/page)")
            
            renderer.close()
            pfd.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error in convertPdfUrlToImages for $pdfUrl", e)
            Timber.e(e, "Error converting PDF to images: $pdfUrl")
        } finally {
            tempFile?.delete()
        }
        
        return@withContext imagePaths
    }
    
    /**
     * Параллельно рендерит одну страницу PDF в изображение
     */
    private suspend fun renderPageToImage(
        pageIndex: Int,
        renderer: PdfRenderer,
        outputDir: File,
        urlHash: String,
        quality: Int,
        fastMode: Boolean
    ): String? = withContext(Dispatchers.Default) {
        var page = renderer.openPage(pageIndex)
        return@withContext try {
            // Рассчитываем размер на основе режима
            val (targetWidth, targetHeight) = if (fastMode) {
                // Для быстрого предпросмотра - 1920x1080
                val width = 1920
                val height = (1920 * page.height.toFloat() / page.width.toFloat()).toInt()
                width to height
            } else {
                // Для полного качества - 4K (3840x2160)
                val width = TARGET_WIDTH
                val height = (TARGET_WIDTH * page.height.toFloat() / page.width.toFloat()).toInt()
                width to height
            }

            // Ограничиваем максимальную высоту
            val limitedHeight = min(targetHeight, targetWidth * 2)
            
            Log.d(TAG, "→ Page $pageIndex: rendering ${targetWidth}x$limitedHeight @ quality=$quality")

            val bitmap = Bitmap.createBitmap(
                targetWidth,
                limitedHeight,
                Bitmap.Config.RGB_565  // Используем RGB_565 вместо ARGB_8888 для быстрости
            )
            
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            // Рендерим страницу
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            // Сохраняем как JPG с адаптивным качеством
            val imageFile = File(outputDir, "slide_${urlHash}_$pageIndex.jpg")
            val saveSuccess = FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            bitmap.recycle()
            
            if (saveSuccess && imageFile.exists() && imageFile.length() > 0) {
                Log.d(TAG, "✓ Page $pageIndex saved (${imageFile.length() / 1024}KB)")
                imageFile.absolutePath
            } else {
                Log.e(TAG, "✗ Failed to save page $pageIndex")
                null
            }
        } finally {
            page.close()
        }
    }
    
    /**
     * Определяет качество сжатия в зависимости от размера файла и количества страниц
     */
    private fun determineQuality(fileSize: Long, pageCount: Int, fastMode: Boolean): Int {
        if (fastMode) return LOW_QUALITY
        
        val avgPageSize = fileSize / pageCount
        return when {
            avgPageSize > 500 * 1024 -> LOW_QUALITY      // Более 500KB на странице
            avgPageSize > 200 * 1024 -> MEDIUM_QUALITY   // Более 200KB на странице
            else -> HIGH_QUALITY                          // Менее 200KB на странице
        }
    }

    /**
     * Удаляет старые слайды, которые больше не нужны.
     * @param keepHashes список хешей URL, которые нужно оставить
     */
    fun cleanOldSlides(keepHashes: Set<String>) {
        val outputDir = File(context.filesDir, CACHE_DIR)
        if (!outputDir.exists()) return
        
        outputDir.listFiles { _, name ->
            if (!name.startsWith("slide_") || !name.endsWith(".jpg")) return@listFiles false
            val hash = name.substringAfter("slide_").substringBefore("_")
            !keepHashes.contains(hash)
        }?.forEach { file ->
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "✓ Removed old cache: ${file.name}")
            }
        }
    }

    /**
     * Очистка всех слайдов
     */
    fun clearCache() {
        val outputDir = File(context.filesDir, CACHE_DIR)
        if (outputDir.exists()) {
            val count = outputDir.listFiles()?.size ?: 0
            outputDir.deleteRecursively()
            Log.d(TAG, "✓ Cache cleared ($count files)")
        }
    }
    
    /**
     * Получить информацию о кэше
     */
    fun getCacheInfo(): CacheInfo {
        val outputDir = File(context.filesDir, CACHE_DIR)
        if (!outputDir.exists()) {
            return CacheInfo(0, 0)
        }
        
        val files = outputDir.listFiles() ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        
        return CacheInfo(files.size, totalSize)
    }
    
    data class CacheInfo(
        val fileCount: Int,
        val totalSize: Long
    ) {
        fun getSizeInMB(): Float = totalSize / (1024f * 1024f)
    }
}
