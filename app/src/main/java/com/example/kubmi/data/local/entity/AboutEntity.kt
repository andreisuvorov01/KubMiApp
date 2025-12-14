package com.example.kubmi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(tableName = "about")
data class AboutEntity(
    @PrimaryKey val id: Int = 0,
    val history: String,
    val management: String, // JSON string
    val faculties: String, // JSON string
    val contacts: String, // JSON string
    val achievements: String, // JSON string
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson = Gson()
        
        fun create(
            history: String,
            management: List<com.example.kubmi.domain.model.ManagementPerson>,
            faculties: List<com.example.kubmi.domain.model.AboutFaculty>,
            contacts: com.example.kubmi.domain.model.ContactInfo,
            achievements: List<com.example.kubmi.domain.model.Achievement>
        ): AboutEntity {
            return AboutEntity(
                history = history,
                management = gson.toJson(management),
                faculties = gson.toJson(faculties),
                contacts = gson.toJson(contacts),
                achievements = gson.toJson(achievements)
            )
        }
        
        fun AboutEntity.toDomain(): com.example.kubmi.domain.model.AboutContent {
            return com.example.kubmi.domain.model.AboutContent(
                history = this.history,
                management = gson.fromJson(this.management, Array<com.example.kubmi.domain.model.ManagementPerson>::class.java).toList(),
                faculties = gson.fromJson(this.faculties, Array<com.example.kubmi.domain.model.AboutFaculty>::class.java).toList(),
                contacts = gson.fromJson(this.contacts, com.example.kubmi.domain.model.ContactInfo::class.java),
                achievements = gson.fromJson(this.achievements, Array<com.example.kubmi.domain.model.Achievement>::class.java).toList()
            )
        }
    }
}