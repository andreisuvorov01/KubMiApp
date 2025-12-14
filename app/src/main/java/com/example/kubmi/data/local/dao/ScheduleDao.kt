package com.example.kubmi.data.local.dao

import androidx.room.Dao

@Dao
interface ScheduleDao {
    // No longer storing detailed schedules in Room. Only index pages are cached.
    // Keep this interface for Hilt to inject, even if it's empty for now.
}
