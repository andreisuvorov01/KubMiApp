package com.example.kubmi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.dao.ScheduleDao
import com.example.kubmi.data.local.dao.AboutDao
import com.example.kubmi.data.local.entity.NewsEntity
import com.example.kubmi.data.local.entity.ScheduleEntity
import com.example.kubmi.data.local.entity.AboutEntity
import com.example.kubmi.data.local.migration.Migrations

@Database(
    entities = [NewsEntity::class, ScheduleEntity::class, AboutEntity::class],
    version = 4,
    exportSchema = false
)
abstract class KubMiDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun aboutDao(): AboutDao
    
    companion object {
        val MIGRATION_1_2 = Migrations.MIGRATION_1_2
        val MIGRATION_2_3 = Migrations.MIGRATION_2_3
        val MIGRATION_3_4 = Migrations.MIGRATION_3_4
    }
}
