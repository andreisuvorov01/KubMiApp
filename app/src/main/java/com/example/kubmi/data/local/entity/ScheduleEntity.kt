package com.example.kubmi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "schedule",
    indices = [
        androidx.room.Index(value = ["group"], name = "idx_schedule_group"),
        androidx.room.Index(value = ["teacher"], name = "idx_schedule_teacher"),
        androidx.room.Index(value = ["dayOfWeek", "timeSlot"], name = "idx_schedule_time")
    ]
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: String,
    val timeSlot: String,
    val subject: String,
    val type: String,
    val room: String,
    val teacher: String,
    val group: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
