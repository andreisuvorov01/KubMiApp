package com.example.kubmi.data.repository

import com.example.kubmi.data.static.AboutStaticProvider
import com.example.kubmi.domain.model.AboutPageContent
import com.example.kubmi.domain.repository.AboutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AboutRepositoryImpl @Inject constructor(
) : AboutRepository {

    private val staticContent = AboutStaticProvider.content

    override fun getAboutContent(): Flow<AboutPageContent> = flowOf(staticContent)

    override suspend fun refreshAboutContent(): AboutPageContent = staticContent
}