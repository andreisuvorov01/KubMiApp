package com.example.kubmi.di

import com.example.kubmi.data.repository.AboutRepositoryImpl
import com.example.kubmi.data.repository.NewsRepositoryImpl
import com.example.kubmi.data.repository.ScheduleRepositoryImpl
import com.example.kubmi.domain.repository.AboutRepository
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindAboutRepository(impl: AboutRepositoryImpl): AboutRepository
}


