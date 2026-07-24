package com.example.kubmi.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import java.io.BufferedOutputStream
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
        private const val TARGET_WIDTH = 1280
        private const val JPEG_QUALITY = 80
        private const val MIN_PDF_SIZE = 512L
        private const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 1 day
    }

    suspend fun downloadPdfFiles(): List<File> = withContext(Dispatchers.IO) {
        val pdfDir = File(context.cacheDir, "sftp_pdf").also { it.mkdirs() }
        val imageDir = File(context.cacheDir, "sftp_images").also { it.mkdirs() }

        val cachedImages = imageDir.listFiles { f -> f.extension == "jpg" }
            ?.sortedBy { it.name }
            ?.takeIf { it.isNotEmpty() }
        val cacheAge = cachedImages?.minOfOrNull { System.currentTimeMillis() - it.lastModified() } ?: Long.MAX_VALUE
        if (cachedImages != null && cacheAge < CACHE_MAX_AGE_MS) {
            Log.d(TAG, "✓ Using ${cachedImages.size} cached images (age ${cacheAge / 60000}min)")
            return@withContext cachedImages
        }
        if (cachedImages != null) {
            Log.d(TAG, "Cache expired (age ${cacheAge / 3600000}h), re-downloading")
            cachedImages.forEach { it.delete() }
        }

        // Отдельная сессия только для получения списка файлов
        val pdfEntries = listRemoteFiles() ?: return@withContext emptyList()
        Log.d(TAG, "✓ Found ${pdfEntries.size} PDF files")

        // Каждый файл качается через полностью независимую сессию
        val downloadedPdfs = pdfEntries.map { (filename, remoteSize) ->
            async(Dispatchers.IO) {
                val pdfFile = File(pdfDir, filename)
                if (pdfFile.exists() && pdfFile.length() == remoteSize && isValidPdf(pdfFile)) {
                    Log.d(TAG, "→ Cached: $filename")
                    return@async pdfFile
                }
                downloadFileOwnSession(filename, pdfFile)
            }
        }.awaitAll().filterNotNull()

        if (downloadedPdfs.isEmpty()) return@withContext emptyList()

        // Конвертируем все PDF параллельно
        downloadedPdfs.map { pdf ->
            async(Dispatchers.Default) { convertPdfToImages(pdf, imageDir) }
        }.awaitAll().flatten()
    }

    private fun listRemoteFiles(): List<Pair<String, Long>>? {
        var session: Session? = null
        var channel: ChannelSftp? = null
        return try {
            session = createSession()
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(CONNECT_TIMEOUT)
            channel.ls(SFTP_REMOTE_DIR).mapNotNull { entry ->
                val e = entry as ChannelSftp.LsEntry
                if (e.filename.endsWith(".pdf", ignoreCase = true) && !e.attrs.isDir)
                    e.filename to e.attrs.size
                else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to list remote files", e)
            null
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    private fun downloadFileOwnSession(filename: String, dest: File): File? {
        val tmp = File(dest.parent, "${dest.name}.tmp")
        var session: Session? = null
        var channel: ChannelSftp? = null
        return try {
            session = createSession()
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(CONNECT_TIMEOUT)
            BufferedOutputStream(FileOutputStream(tmp), 65536).use { out ->
                channel.get("$SFTP_REMOTE_DIR/$filename", out)
            }
            if (!isValidPdf(tmp)) {
                Log.e(TAG, "✗ Invalid PDF after download: $filename")
                tmp.delete()
                return null
            }
            tmp.renameTo(dest)
            Log.d(TAG, "✓ Downloaded: $filename (${dest.length() / 1024}KB)")
            dest
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed: $filename", e)
            tmp.delete()
            dest.delete()
            null
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    private fun createSession(): Session {
        val jsch = JSch()
        val session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT)
        session.setPassword(SFTP_PASSWORD)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "password")
        session.setConfig("compression.s2c", "none")
        session.setConfig("compression.c2s", "none")
        session.timeout = CONNECT_TIMEOUT
        session.connect(CONNECT_TIMEOUT)
        return session
    }

    private fun isValidPdf(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_PDF_SIZE) return false
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(5)
                stream.read(header)
                header.toString(Charsets.ISO_8859_1) == "%PDF-"
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun convertPdfToImages(pdfFile: File, outputDir: File): List<File> {
        val imageFiles = mutableListOf<File>()
        var pdfRenderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null
        try {
            fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fd)
            for (i in 0 until pdfRenderer.pageCount) {
                val imageFile = File(outputDir, "${pdfFile.nameWithoutExtension}_page_$i.jpg")
                if (imageFile.exists() && imageFile.length() > 0) {
                    imageFiles.add(imageFile)
                    continue
                }
                val page = pdfRenderer.openPage(i)
                try {
                    val scale = TARGET_WIDTH.toFloat() / page.width.toFloat()
                    val h = min((page.height * scale).toInt(), TARGET_WIDTH * 2)
                    
                    // Проверяем валидность размеров
                    if (TARGET_WIDTH <= 0 || h <= 0 || page.width <= 0 || page.height <= 0) {
                        Log.w(TAG, "Invalid dimensions for page $i: ${page.width}x${page.height} -> ${TARGET_WIDTH}x$h")
                        continue
                    }
                    
                    val bitmap = Bitmap.createBitmap(TARGET_WIDTH, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    FileOutputStream(imageFile).use { 
                        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) 
                    }
                    bitmap.recycle()
                    if (imageFile.length() > 0) imageFiles.add(imageFile)
                } finally {
                    page.close()
                }
            }
            Log.d(TAG, "✓ Converted ${pdfFile.name}: ${imageFiles.size} pages")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Convert error: ${pdfFile.name}", e)
        } finally {
            pdfRenderer?.close()
            fd?.close()
        }
        return imageFiles
    }

    fun cleanupOldFiles(maxAgeDays: Int = 7) {
        val now = System.currentTimeMillis()
        val maxAge = maxAgeDays * 24 * 60 * 60 * 1000L
        listOf(File(context.cacheDir, "sftp_pdf"), File(context.cacheDir, "sftp_images")).forEach { dir ->
            dir.listFiles()?.forEach { if (now - it.lastModified() > maxAge) it.delete() }
        }
    }

    fun invalidateImageCache() {
        File(context.cacheDir, "sftp_images").listFiles()?.forEach { it.delete() }
    }
}
