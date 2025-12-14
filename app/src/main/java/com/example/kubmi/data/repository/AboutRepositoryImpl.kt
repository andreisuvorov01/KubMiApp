package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.AboutDao
import com.example.kubmi.data.local.entity.AboutEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.AboutContent
import com.example.kubmi.domain.model.AboutFaculty
import com.example.kubmi.domain.model.Achievement
import com.example.kubmi.domain.model.ContactInfo
import com.example.kubmi.domain.model.ManagementPerson
import com.example.kubmi.domain.repository.AboutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AboutRepositoryImpl @Inject constructor(
    private val webScraper: WebScraper,
    private val aboutDao: AboutDao
) : AboutRepository {
    
    override fun getAboutContent(): Flow<AboutContent?> = 
        aboutDao.getAboutContent().map { entity ->
            entity?.toDomain()
        }
    
    override suspend fun refreshAboutContent(): AboutContent {
        // Scrape fresh data from the website
        val history = webScraper.scrapeHistory()
        val management = webScraper.scrapeManagement()
        val faculties = webScraper.scrapeFaculties()
        val contacts = webScraper.scrapeContacts()
        val achievements = webScraper.scrapeAchievements()
        
        // Create domain model
        val aboutContent = AboutContent(
            history = history,
            management = management,
            faculties = faculties,
            contacts = contacts,
            achievements = achievements
        )
        
        // Save to database
        val aboutEntity = AboutEntity.create(
            history = history,
            management = management,
            faculties = faculties,
            contacts = contacts,
            achievements = achievements
        )
        aboutDao.insertAboutContent(aboutEntity)
        
        return aboutContent
    }
    
    override suspend fun clearCache() {
        aboutDao.clearAboutContent()
    }
    
    // Extension function to convert entity to domain model
    private fun AboutEntity.toDomain(): AboutContent {
        return AboutContent(
            history = this.history,
            management = try {
                com.google.gson.Gson().fromJson(this.management, Array<ManagementPerson>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            },
            faculties = try {
                com.google.gson.Gson().fromJson(this.faculties, Array<AboutFaculty>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            },
            contacts = try {
                com.google.gson.Gson().fromJson(this.contacts, ContactInfo::class.java)
            } catch (e: Exception) {
                ContactInfo("", "", "", "")
            },
            achievements = try {
                com.google.gson.Gson().fromJson(this.achievements, Array<Achievement>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
}