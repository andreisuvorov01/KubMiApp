package com.example.kubmi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val date: String,
    val imageUrl: String?,
    val fullText: String? = null,
    val contentBlocksJson: String? = null,
    val isPdfSlide: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
