package com.example.kubmi.presentation.screens.main

import app.cash.turbine.test
import com.example.kubmi.data.repository.NewsRepository
import com.example.kubmi.domain.model.News
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    @MockK
    private lateinit var newsRepository: NewsRepository

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = MainViewModel(newsRepository)
    }

    @Test
    fun `news flow emits repository data when available`() = runTest {
        // Given
        val testNews = listOf(
            News("1", "Test News", "Description", "Content", "2024-01-01")
        )
        every { newsRepository.getAllNews() } returns flowOf(testNews)

        // When & Then
        viewModel.news.test {
            val emittedNews = awaitItem()
            assertEquals(testNews, emittedNews)
            assertEquals(1, emittedNews.size)
            assertEquals("Test News", emittedNews[0].title)
        }
    }

    @Test
    fun `news flow emits empty list when no data available`() = runTest {
        // Given
        every { newsRepository.getAllNews() } returns flowOf(emptyList())

        // When & Then
        viewModel.news.test {
            val emittedNews = awaitItem()
            assertTrue(emittedNews.isEmpty())
        }
    }

    @Test
    fun `news flow handles repository errors gracefully`() = runTest {
        // Given - repository throws exception
        every { newsRepository.getAllNews() } throws RuntimeException("Network error")

        // When & Then - ViewModel should still emit empty list as initial value
        viewModel.news.test {
            val emittedNews = awaitItem()
            assertTrue(emittedNews.isEmpty())
        }
    }
}