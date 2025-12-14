package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.AboutContent
import kotlinx.coroutines.flow.Flow

interface AboutRepository {
    fun getAboutContent(): Flow<AboutContent?>
    suspend fun refreshAboutContent(): AboutContent
    suspend fun clearCache()
}