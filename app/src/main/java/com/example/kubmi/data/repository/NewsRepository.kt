package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.entity.NewsEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.News
import com.example.kubmi.domain.model.NewsContentBlock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

import com.example.kubmi.domain.repository.NewsRepository as DomainNewsRepository

class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao,
    private val webScraper: WebScraper
) : DomainNewsRepository {
    private val gson = Gson()

    override fun getAllNews(): Flow<List<News>> = newsDao.getAllNews().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeNewsById(id: String): Flow<News?> =
        newsDao.observeNewsById(id).map { it?.toDomain() }

    override suspend fun getNewsById(id: String): News? = newsDao.getNewsById(id)?.toDomain()

    override suspend fun refreshNews() {
        try {
            withContext(Dispatchers.IO) {
                val news = webScraper.scrapeNews()
                if (news.isEmpty()) {
                    // Important: do NOT wipe previously cached news if scraping failed / returned nothing.
                    Timber.w("refreshNews(): scraped 0 items; keeping existing cached news")
                    return@withContext
                }
                
                // Merge preview news with cached details (fullText/contentBlocksJson) so that
                // opening an already-read article keeps its full content after refresh.
                val now = System.currentTimeMillis()
                val merged = ArrayList<NewsEntity>(news.size)
                for (scraped in news) {
                    val existing = newsDao.getNewsById(scraped.id)
                    val scrapedEntity = scraped.toEntity()
                    
                    val finalEntity = if (existing == null) {
                        scrapedEntity.copy(timestamp = now)
                    } else {
                        scrapedEntity.copy(
                            // Keep better/older fields when scraper preview is missing them.
                            title = scrapedEntity.title.ifBlank { existing.title },
                            description = scrapedEntity.description.ifBlank { existing.description },
                            date = scrapedEntity.date.ifBlank { existing.date },
                            imageUrl = scrapedEntity.imageUrl ?: existing.imageUrl,
                            // Preserve full article fields.
                            fullText = existing.fullText,
                            contentBlocksJson = existing.contentBlocksJson,
                            // Preserve full text in `content` if we had it.
                            content = if (!existing.fullText.isNullOrBlank()) existing.content else scrapedEntity.content,
                            timestamp = now
                        )
                    }
                    
                    merged.add(finalEntity)
                }
                
                newsDao.insertAll(merged)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing news")
            throw e
        }
    }

    override suspend fun refreshNewsArticle(id: String) {
        if (id.isBlank()) return
        try {
            withContext(Dispatchers.IO) {
                val existing = newsDao.getNewsById(id)
                val scraped = webScraper.scrapeNewsArticle(id)

                val blocksJson = if (scraped.blocks.isNotEmpty()) gson.toJson(scraped.blocks) else null
                val fullText = scraped.fullText.takeIf { it.isNotBlank() }
                val title = scraped.title.takeIf { it.isNotBlank() }

                val updated = if (existing != null) {
                    existing.copy(
                        title = title ?: existing.title,
                        // Keep preview fields, but update content with full text for fallback screens.
                        content = fullText ?: existing.content,
                        fullText = fullText ?: existing.fullText,
                        contentBlocksJson = blocksJson ?: existing.contentBlocksJson
                    )
                } else {
                    // Fallback: create minimal record if user opened an URL not present in DB yet.
                    NewsEntity(
                        id = id,
                        title = title ?: id,
                        description = "",
                        content = fullText.orEmpty(),
                        date = "",
                        imageUrl = null,
                        fullText = fullText,
                        contentBlocksJson = blocksJson
                    )
                }

                newsDao.upsert(updated)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing news article: %s", id)
            throw e
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

        return News(
            id = id,
            title = title,
            description = description,
            content = content,
            date = date,
            imageUrl = imageUrl,
            contentBlocks = blocks,
            fullText = fullText
        )
    }

    private fun News.toEntity(): NewsEntity {
        val blocksJson = if (contentBlocks.isNotEmpty()) gson.toJson(contentBlocks) else null
        return NewsEntity(
            id = id,
            title = title,
            description = description,
            content = content,
            date = date,
            imageUrl = imageUrl,
            fullText = fullText,
            contentBlocksJson = blocksJson
        )
    }
}
