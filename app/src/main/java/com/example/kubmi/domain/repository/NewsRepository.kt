package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.News
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getAllNews(): Flow<List<News>>
    fun observeNewsById(id: String): Flow<News?>
    suspend fun getNewsById(id: String): News?
    suspend fun getAllNewsSync(): List<News>
    fun getPdfSlides(): Flow<List<News>>
    suspend fun refreshNews(forceNetwork: Boolean = false)
    suspend fun refreshNewsArticle(id: String)
}