package com.example.kubmi.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create the about table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `about` (
                    `id` INTEGER NOT NULL,
                    `history` TEXT NOT NULL,
                    `management` TEXT NOT NULL,
                    `faculties` TEXT NOT NULL,
                    `contacts` TEXT NOT NULL,
                    `achievements` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }
    
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add indices to schedule table
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_group` ON `schedule` (`group`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_teacher` ON `schedule` (`teacher`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_time` ON `schedule` (`dayOfWeek`, `timeSlot`)")
        }
    }
}