package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.entity.NewsEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.News
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NewsRepositoryTest {

    @MockK
    private lateinit var newsDao: NewsDao

    @MockK
    private lateinit var webScraper: WebScraper

    private lateinit var newsRepository: NewsRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        newsRepository = NewsRepository(newsDao, webScraper)
    }

    @Test
    fun `getAllNews returns mapped domain objects`() = runTest {
        // Given
        val entity = NewsEntity(
            id = "1",
            title = "Test News",
            description = "Test Description",
            content = "Test Content",
            date = "2024-01-01",
            imageUrl = "https://example.com/image.jpg"
        )
        
        every { newsDao.getAllNews() } returns flowOf(listOf(entity))

        // When
        val result = newsRepository.getAllNews()

        // Then
        result.collect { newsItems ->
            assertEquals(1, newsItems.size)
            with(newsItems[0]) {
                assertEquals("1", id)
                assertEquals("Test News", title)
                assertEquals("Test Description", description)
                assertEquals("Test Content", content)
                assertEquals("2024-01-01", date)
                assertEquals("https://example.com/image.jpg", imageUrl)
            }
        }
    }

    @Test
    fun `getNewsById returns mapped domain object when found`() = runTest {
        // Given
        val entity = NewsEntity(
            id = "1",
            title = "Test News",
            description = "Test Description",
            content = "Test Content",
            date = "2024-01-01",
            imageUrl = "https://example.com/image.jpg"
        )
        
        every { newsDao.getNewsById("1") } returns entity

        // When
        val result = newsRepository.getNewsById("1")

        // Then
        with(result!!) {
            assertEquals("1", id)
            assertEquals("Test News", title)
            assertEquals("Test Description", description)
            assertEquals("Test Content", content)
            assertEquals("2024-01-01", date)
            assertEquals("https://example.com/image.jpg", imageUrl)
        }
    }

    @Test
    fun `getNewsById returns null when not found`() = runTest {
        // Given
        every { newsDao.getNewsById("nonexistent") } returns null

        // When
        val result = newsRepository.getNewsById("nonexistent")

        // Then
        assertNull(result)
    }

    @Test
    fun `refreshNews scrapes data and updates database`() = runTest {
        // Given
        val scrapedNews = listOf(
            News(
                id = "1",
                title = "Refreshed News",
                description = "Refreshed Description",
                content = "Refreshed Content",
                date = "2024-01-02",
                imageUrl = "https://example.com/refreshed.jpg"
            )
        )
        
        coEvery { webScraper.scrapeNews() } returns scrapedNews
        coEvery { newsDao.deleteAll() } returns Unit
        coEvery { newsDao.insertAll(any()) } returns Unit

        // When
        newsRepository.refreshNews()

        // Then
        // Verify interactions
        coVerify { webScraper.scrapeNews() }
        coVerify { newsDao.deleteAll() }
        coVerify { newsDao.insertAll(any()) }
    }
}