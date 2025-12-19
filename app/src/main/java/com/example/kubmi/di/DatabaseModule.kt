package com.example.kubmi.di

import android.content.Context
import androidx.room.Room
import com.example.kubmi.data.local.KubMiDatabase
import com.example.kubmi.data.local.dao.NewsDao
import com.example.kubmi.data.local.dao.ScheduleDao
import com.example.kubmi.data.local.dao.AboutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KubMiDatabase {
        return Room.databaseBuilder(
            context,
            KubMiDatabase::class.java,
            "kubmi_database"
        ).addMigrations(
            KubMiDatabase.MIGRATION_1_2,
            KubMiDatabase.MIGRATION_2_3,
            KubMiDatabase.MIGRATION_3_4,
            KubMiDatabase.MIGRATION_4_5,
            KubMiDatabase.MIGRATION_5_6,
            KubMiDatabase.MIGRATION_6_7
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideNewsDao(database: KubMiDatabase): NewsDao = database.newsDao()

    @Provides
    fun provideScheduleDao(database: KubMiDatabase): ScheduleDao = database.scheduleDao()
    
    @Provides
    fun provideAboutDao(database: KubMiDatabase): AboutDao = database.aboutDao()
}
