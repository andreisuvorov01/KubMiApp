package com.example.kubmi.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.entity.NewsEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.util.ImageProcessingUtils
import com.example.kubmi.util.ParserCache
import com.example.kubmi.domain.model.News
import com.example.kubmi.domain.model.NewsContentBlock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.net.UnknownHostException
import java.net.URI
import javax.inject.Inject

import com.example.kubmi.domain.repository.NewsRepository as DomainNewsRepository

class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao,
    private val webScraper: WebScraper,
    private val parserCache: ParserCache,
    private val imageUtils: ImageProcessingUtils
) : DomainNewsRepository {
    private val gson = Gson()
    private val refreshMutex = Mutex()

    override fun getAllNews(): Flow<List<News>> = newsDao.getAllNews().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeNewsById(id: String): Flow<News?> =
        newsDao.observeNewsById(id).map { it?.toDomain() }

    override suspend fun getNewsById(id: String): News? = newsDao.getNewsById(id)?.toDomain()

    override suspend fun getAllNewsSync(): List<News> = withContext(Dispatchers.IO) {
        newsDao.getAllNewsSync().map { it.toDomain() }
    }

    override fun getPdfSlides(): Flow<List<News>> = newsDao.getPdfSlides().map { entities ->
        entities.map { it.toDomain() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun refreshNews(forceNetwork: Boolean) {
        // Use NonCancellable to ensure that if started, it finishes DB operations
        withContext(NonCancellable) {
            refreshMutex.withLock {
                try {
                    Log.d("NewsRepository", "refreshNews execution started (forceNetwork=$forceNetwork)")
                    
                    withContext(Dispatchers.IO) {
                        // 1. Check existing regular news
                        val regularNewsInDb = newsDao.getAllNewsSync()
                        
                        if (!forceNetwork && regularNewsInDb.isNotEmpty()) {
                            Log.d("NewsRepository", "Regular news found in DB, skipping network refresh")
                        } else {
                            // SCRAPE REGULAR NEWS
                            val regularNews = try { 
                                webScraper.scrapeNews() 
                            } catch (e: Exception) { 
                                if (e is UnknownHostException) {
                                    Log.w("NewsRepository", "Offline: Could not resolve host for news")
                                } else {
                                    Timber.e(e, "Error scraping regular news")
                                }
                                emptyList<News>() 
                            }

                            val now = System.currentTimeMillis()
                            if (regularNews.isNotEmpty()) {
                                val mergedRegular = regularNews.map { scraped ->
                                    val existing = newsDao.getNewsById(scraped.id)
                                    
                                    var finalImageUrl: String? = null
                                    
                                    // 1. Try to download the new image from scraped data
                                    if (scraped.imageUrl != null && scraped.imageUrl.startsWith("http")) {
                                        val fileName = "news_${scraped.id.hashCode()}.jpg"
                                        val localPath = imageUtils.downloadAndProcessImage(scraped.imageUrl, fileName)
                                        if (localPath != null) {
                                            finalImageUrl = localPath
                                        }
                                    }
                                    
                                    // 2. If download failed or scraped image was null, fallback to existing local image
                                    if (finalImageUrl == null && existing?.imageUrl?.startsWith("file://") == true) {
                                        finalImageUrl = existing.imageUrl
                                    }
                                    
                                    // 3. Last fallback: use the scraped remote URL if we have nothing else
                                    if (finalImageUrl == null) {
                                        finalImageUrl = scraped.imageUrl
                                    }

                                    scraped.copy(
                                        imageUrl = finalImageUrl,
                                        fullText = existing?.fullText,
                                        contentBlocks = existing?.contentBlocksJson?.let { 
                                            try {
                                                gson.fromJson<List<NewsContentBlock>>(it, object : TypeToken<List<NewsContentBlock>>() {}.type)
                                            } catch (_: Exception) { emptyList() }
                                        } ?: emptyList(),
                                        isPdfSlide = false
                                    )
                                }

                                // Cleanup old news images only if we actually have news to show
                                val coverImageFiles = mergedRegular.mapNotNull { 
                                    if (it.imageUrl?.startsWith("file://") == true) it.imageUrl.substringAfterLast("/") else null 
                                }.toSet()
                                val articleImageFiles = regularNewsInDb.flatMap { entity ->
                                    try {
                                        val blocks = gson.fromJson<List<NewsContentBlock>>(entity.contentBlocksJson ?: "", object : TypeToken<List<NewsContentBlock>>() {}.type)
                                        blocks.filter { it.type == NewsContentBlock.TYPE_IMAGE && it.imageUrl?.startsWith("file://") == true }
                                              .mapNotNull { it.imageUrl?.substringAfterLast("/") }
                                    } catch (_: Exception) { emptyList() }
                                }.toSet()
                                val allKeepFiles = coverImageFiles + articleImageFiles
                                if (allKeepFiles.isNotEmpty()) {
                                    imageUtils.cleanOldImages(allKeepFiles)
                                }

                                // Save regular news
                                newsDao.insertAll(mergedRegular.mapIndexed { index, item -> item.toEntity().copy(timestamp = now - index) })
                            }
                        }

                        // 2. Final update Parser Cache for UI
                        val finalRegularNews = newsDao.getAllNewsSync().map { it.toDomain() }
                        if (finalRegularNews.isNotEmpty()) {
                            parserCache.writeNews(finalRegularNews)
                            Log.d("NewsRepository", "Parser cache updated with ${finalRegularNews.size} items")
                        }
                    }
                } catch (e: Exception) {
                    if (e !is UnknownHostException) {
                        Log.e("NewsRepository", "refreshNews error", e)
                    }
                } finally {
                    Log.d("NewsRepository", "refreshNews execution finished")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun refreshNewsArticle(id: String) {
        if (id.isBlank()) return
        withContext(NonCancellable + Dispatchers.IO) {
            val existing = newsDao.getNewsById(id)
            try {
                val scraped = webScraper.scrapeNewsArticle(id)
                
                val processedBlocks = scraped.blocks.map { block ->
                    if (block.type == NewsContentBlock.TYPE_IMAGE && block.imageUrl != null && block.imageUrl.startsWith("http")) {
                        val remoteUrl = block.imageUrl
                        val fileName = "article_${id.hashCode()}_${remoteUrl.hashCode()}.jpg"
                        val localPath = imageUtils.downloadAndProcessImage(remoteUrl, fileName)
                        if (localPath != null) {
                            block.copy(imageUrl = localPath, remoteImageUrl = remoteUrl)
                        } else {
                            block.copy(remoteImageUrl = remoteUrl)
                        }
                    } else {
                        block
                    }
                }

                val blocksJson = if (processedBlocks.isNotEmpty()) gson.toJson(processedBlocks) else null
                val fullText = scraped.fullText.takeIf { it.isNotBlank() }
                val title = scraped.title.takeIf { it.isNotBlank() }

                val updated = if (existing != null) {
                    var finalCoverUrl = existing.imageUrl
                    
                    // If existing cover is not local, or if we found a new cover URL, try to download it
                    val newCoverUrl = scraped.coverImageUrl
                    if (!newCoverUrl.isNullOrBlank() && (finalCoverUrl == null || !finalCoverUrl.startsWith("file://"))) {
                        val fileName = "news_${id.hashCode()}.jpg"
                        val localPath = imageUtils.downloadAndProcessImage(newCoverUrl, fileName)
                        if (localPath != null) {
                            finalCoverUrl = localPath
                        }
                    }

                    existing.copy(
                        title = title ?: existing.title,
                        content = fullText ?: existing.content,
                        fullText = fullText ?: existing.fullText,
                        contentBlocksJson = blocksJson ?: existing.contentBlocksJson,
                        imageUrl = finalCoverUrl,
                        isPdfSlide = false
                    )
                } else {
                    var finalCoverUrl: String? = null
                    val newCoverUrl = scraped.coverImageUrl
                    if (!newCoverUrl.isNullOrBlank()) {
                        val fileName = "news_${id.hashCode()}.jpg"
                        finalCoverUrl = imageUtils.downloadAndProcessImage(newCoverUrl, fileName) ?: newCoverUrl
                    }

                    NewsEntity(
                        id = id,
                        title = title ?: id,
                        description = "",
                        content = fullText.orEmpty(),
                        date = "",
                        imageUrl = finalCoverUrl,
                        fullText = fullText,
                        contentBlocksJson = blocksJson,
                        isPdfSlide = false
                    )
                }
                newsDao.upsert(updated)
                
                // Also update the parser cache so the main list shows the new image/content immediately
                val allNewsInDb = newsDao.getAllNewsSync().map { it.toDomain() }
                if (allNewsInDb.isNotEmpty()) {
                    parserCache.writeNews(allNewsInDb)
                }
                
                Log.d("NewsRepository", "Article updated and cache refreshed: $id")
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    Log.w("NewsRepository", "Offline: Could not resolve host for article $id")
                } else {
                    Timber.e(e, "Error refreshing news article: %s", id)
                }
                // Scrape failed — repair any stale cached images referenced by existing blocks
                try {
                    repairStaleArticleImages(id, existing)
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun repairStaleArticleImages(id: String, existing: NewsEntity?) {
        val json = existing?.contentBlocksJson ?: return
        val existingBlocks = try {
            gson.fromJson<List<NewsContentBlock>>(json, object : TypeToken<List<NewsContentBlock>>() {}.type)
        } catch (_: Exception) { return }

        var repaired = false
        val repairedBlocks = existingBlocks.map { block ->
            if (block.type == NewsContentBlock.TYPE_IMAGE && block.imageUrl?.startsWith("file://") == true) {
                try {
                    val file = File(URI(block.imageUrl))
                    if (!file.exists()) {
                        val remoteUrl = block.remoteImageUrl ?: return@map block
                        if (!remoteUrl.startsWith("http")) return@map block
                        val fileName = "article_${id.hashCode()}_${remoteUrl.hashCode()}.jpg"
                        val localPath = imageUtils.downloadAndProcessImage(remoteUrl, fileName)
                        if (localPath != null) {
                            repaired = true
                            return@map block.copy(imageUrl = localPath, remoteImageUrl = remoteUrl)
                        }
                    }
                } catch (_: Exception) {}
            }
            block
        }
        if (repaired) {
            newsDao.upsert(existing.copy(contentBlocksJson = gson.toJson(repairedBlocks)))
            Log.d("NewsRepository", "Repaired stale images for article: $id")
        }
    }

    private fun NewsEntity.toDomain(): News {
        val blocks: List<NewsContentBlock> = try {
            val json = contentBlocksJson
            if (json.isNullOrBlank()) emptyList()
            else gson.fromJson(json, object : TypeToken<List<NewsContentBlock>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
        return News(id, title, description, content, date, imageUrl, blocks, fullText, isPdfSlide)
    }

    private fun News.toEntity(): NewsEntity {
        val blocksJson = if (contentBlocks.isNotEmpty()) gson.toJson(contentBlocks) else null
        return NewsEntity(id, title, description, content, date, imageUrl, fullText, blocksJson, isPdfSlide)
    }
}
