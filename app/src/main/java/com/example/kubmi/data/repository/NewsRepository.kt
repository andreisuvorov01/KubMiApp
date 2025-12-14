package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.entity.NewsEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.News
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
    override fun getAllNews(): Flow<List<News>> = newsDao.getAllNews().map { entities ->
        entities.map { it.toDomain() }
    }

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
                newsDao.deleteAll()
                newsDao.insertAll(news.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing news")
            throw e
        }
    }

    private fun NewsEntity.toDomain() = News(id, title, description, content, date, imageUrl)
    private fun News.toEntity() = NewsEntity(id, title, description, content, date, imageUrl)
}
