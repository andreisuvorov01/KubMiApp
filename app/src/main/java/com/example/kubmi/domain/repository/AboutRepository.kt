package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.AboutPageContent
import kotlinx.coroutines.flow.Flow

interface AboutRepository {
    fun getAboutContent(): Flow<AboutPageContent>
    suspend fun refreshAboutContent(): AboutPageContent
}