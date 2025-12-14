package com.example.kubmi.di

import android.content.Context
import com.example.kubmi.util.AdminLogger
import com.example.kubmi.util.CacheManager
import com.example.kubmi.util.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {
    
    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SecurePreferences {
        return SecurePreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideCacheManager(@ApplicationContext context: Context): CacheManager {
        return CacheManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAdminLogger(@ApplicationContext context: Context): AdminLogger {
        return AdminLogger(context)
    }
}