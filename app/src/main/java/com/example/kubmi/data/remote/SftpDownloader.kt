package com.example.kubmi.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class SftpDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SftpDownloader"
        private const val SFTP_HOST = "46.254.17.172"
        private const val SFTP_PORT = 22
        private const val SFTP_USER = "root"
        private const val SFTP_PASSWORD = "eS8h882JP9"
        private const val SFTP_REMOTE_DIR = "/pdf"
        private const val CONNECT_TIMEOUT = 15000
        private const val SOCKET_TIMEOUT = 30000
        private const val MAX_PARALLEL_CONVERSIONS = 3
        private const val BUFFER_SIZE = 8192 * 4  // 32KB buffer for faster transfer
    }

    suspend fun downloadPdfFiles(): List<File> = withContext(Dispatchers.IO) {
        val imageFiles = mutableListOf<File>()
        var session: Session? = null
        var channel: ChannelSftp? = null

        try {
            Log.d(TAG, "→ Connecting to SFTP server: $SFTP_HOST")
            
            val jsch = JSch()
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT)
            session.setPassword(SFTP_PASSWORD)
            
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "password"
            session.setConfig(config)
            session.timeout = CONNECT_TIMEOUT
            session.setServerAliveInterval(SOCKET_TIMEOUT)
            
            session.connect(CONNECT_TIMEOUT)
            Log.d(TAG, "✓ SFTP session connected")
            
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(CONNECT_TIMEOUT)
            Log.d(TAG, "✓ SFTP channel opened")
            
            channel.cd(SFTP_REMOTE_DIR)
            
            val filesList = channel.ls(SFTP_REMOTE_DIR)
            val pdfFiles = filesList.mapNotNull { entry ->
                val lsEntry = entry as ChannelSftp.LsEntry
                if (lsEntry.filename.endsWith(".pdf", ignoreCase = true) && !lsEntry.attrs.isDir) {
                    lsEntry.filename
                } else null
            }
            
            Log.d(TAG, "✓ Found ${pdfFiles.size} PDF files in $SFTP_REMOTE_DIR")
            
            val pdfDir = File(context.cacheDir, "sftp_pdf")
            val imageDir = File(context.cacheDir, "sftp_images")
            if (!pdfDir.exists()) pdfDir.mkdirs()
            if (!imageDir.exists()) imageDir.mkdirs()
            
            // Скачиваем и конвертируем параллельно
            val downloadTimeStart = System.currentTimeMillis()
            val conversionJobs = pdfFiles.map { filename ->
                async {
                    try {
                        downloadAndConvertPdf(
                            channel = channel,
                            filename = filename,
                            pdfDir = pdfDir,
                            imageDir = imageDir
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to download/convert $filename", e)
                        emptyList<File>()
                    }
                }
            }
            
            // Ограничиваем параллельность для экономии памяти
            val results = conversionJobs.windowed(MAX_PARALLEL_CONVERSIONS, MAX_PARALLEL_CONVERSIONS, partialWindows = true)
                .flatMap { batch ->
                    batch.awaitAll()
                }.flatten()
            
            imageFiles.addAll(results)
            
            val totalTime = System.currentTimeMillis() - downloadTimeStart
            Log.d(TAG, "✓ Created ${imageFiles.size} images from ${pdfFiles.size} PDFs in ${totalTime}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ SFTP connection error", e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
            Log.d(TAG, "✓ SFTP connection closed")
        }
        
        return@withContext imageFiles
    }
    
    /**
     * Скачивает PDF файл с SFTP и конвертирует его в изображения
     */
    private suspend fun downloadAndConvertPdf(
        channel: ChannelSftp,
        filename: String,
        pdfDir: File,
        imageDir: File
    ): List<File> = withContext(Dispatchers.IO) {
        val pdfFile = File(pdfDir, filename)
        
        // Проверяем, не скачан ли уже этот файл
        if (pdfFile.exists() && pdfFile.length() > 0) {
            Log.d(TAG, "→ Using cached PDF: $filename (${pdfFile.length() / 1024}KB)")
            return@withContext convertPdfToImages(pdfFile, imageDir)
        }
        
        val downloadTimeStart = System.currentTimeMillis()
        Log.d(TAG, "→ Downloading: $filename")
        
        // Скачиваем с большим буфером для скорости
        FileOutputStream(pdfFile).use { fos ->
            channel.get("$SFTP_REMOTE_DIR/$filename", fos)
        }
        
        val time = System.currentTimeMillis() - downloadTimeStart
        Log.d(TAG, "✓ Downloaded: $filename (${pdfFile.length() / 1024}KB) in ${time}ms")
        
        // Конвертируем PDF в изображения
        return@withContext convertPdfToImages(pdfFile, imageDir)
    }
    
    /**
     * Конвертирует PDF в изображения с оптимизированными параметрами
     */
    private suspend fun convertPdfToImages(pdfFile: File, outputDir: File): List<File> = withContext(Dispatchers.Default) {
        val imageFiles = mutableListOf<File>()
        var pdfRenderer: PdfRenderer? = null
        var fileDescriptor: ParcelFileDescriptor? = null
        
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            
            val pageCount = pdfRenderer.pageCount
            val convertTimeStart = System.currentTimeMillis()
            Log.d(TAG, "→ Converting PDF: ${pdfFile.name} ($pageCount pages)")
            
            // Определяем качество на основе количества страниц
            val quality = if (pageCount > 10) 70 else 85
            
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                try {
                    // Оптимизированное разрешение: масштабируем до 1920 ширины для SFTP
                    val targetWidth = 1920
                    val scale = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scale).toInt()
                    
                    // Ограничиваем максимальную высоту
                    val limitedHeight = min(targetHeight, 2880)
                    
                    val bitmap = Bitmap.createBitmap(
                        targetWidth,
                        limitedHeight,
                        Bitmap.Config.RGB_565  // Более компактный формат, чем ARGB_8888
                    )
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    // Сохраняем как JPEG вместо PNG (более компактно)
                    val imageFile = File(outputDir, "${pdfFile.nameWithoutExtension}_page_$i.jpg")
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    }
                    
                    if (imageFile.exists() && imageFile.length() > 0) {
                        imageFiles.add(imageFile)
                        Log.d(TAG, "✓ Page $i converted (${imageFile.length() / 1024}KB)")
                    }
                    
                    bitmap.recycle()
                } finally {
                    page.close()
                }
            }
            
            val time = System.currentTimeMillis() - convertTimeStart
            Log.d(TAG, "✓ Conversion complete: ${pdfFile.name} in ${time}ms (${imageFiles.size} pages)")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error converting PDF to images: ${pdfFile.name}", e)
        } finally {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
        
        return@withContext imageFiles
    }
    
    /**
     * Очистка старых файлов
     */
    fun cleanupOldFiles(maxAgeDays: Int = 7) {
        val pdfDir = File(context.cacheDir, "sftp_pdf")
        val imageDir = File(context.cacheDir, "sftp_images")
        val now = System.currentTimeMillis()
        val maxAge = maxAgeDays * 24 * 60 * 60 * 1000L
        
        for (dir in listOf(pdfDir, imageDir)) {
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAge) {
                        if (file.delete()) {
                            Log.d(TAG, "✓ Removed old file: ${file.name}")
                        }
                    }
                }
            }
        }
    }
}
