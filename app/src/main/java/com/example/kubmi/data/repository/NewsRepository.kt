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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private fun News.toEntity() = NewsEntity(
        id = id,
        title = title,
        description = description,
        content = content,
        date = date,
        imageUrl = imageUrl,
        timestamp = date.toNewsTimestamp()
    )

    private fun String.toNewsTimestamp(): Long {
        val normalized = trim()
        if (normalized.isBlank()) return System.currentTimeMillis()
        val ruMonths = mapOf(
            "января" to "01", "февраля" to "02", "марта" to "03", "апреля" to "04",
            "мая" to "05", "июня" to "06", "июля" to "07", "августа" to "08",
            "сентября" to "09", "октября" to "10", "ноября" to "11", "декабря" to "12"
        )
        val replaced = ruMonths.entries.fold(normalized.lowercase(Locale.ROOT)) { acc, (month, number) ->
            acc.replace(month, number)
        }
        val token = Regex("\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[./]\\d{1,2}[./]\\d{4}|\\d{1,2}\\s+\\d{2}\\s+\\d{4}")
            .find(replaced)?.value ?: replaced
        val patterns = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/MM/yyyy"),
            DateTimeFormatter.ofPattern("d MM yyyy"),
            DateTimeFormatter.ofPattern("dd MM yyyy")
        )
        patterns.forEach { pattern ->
            runCatching { return LocalDate.parse(token, pattern).toEpochDay() * 86_400_000L }
        }
        Timber.w("Unable to parse news date for cache ordering: %s", this)
        return System.currentTimeMillis()
    }
}
