package com.example.kubmi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_cache",
    indices = [
        Index(value = ["ownerType", "ownerTitle"]),
        Index(value = ["timestamp"])
    ]
)
data class ScheduleCacheEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerTitle: String,
    val dataJson: String,
    val sourceUrl: String?,
    val timestamp: Long
)
