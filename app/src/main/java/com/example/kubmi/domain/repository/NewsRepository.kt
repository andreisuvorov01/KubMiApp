package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.News
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getAllNews(): Flow<List<News>>
    suspend fun getNewsById(id: String): News?
    suspend fun refreshNews()
}