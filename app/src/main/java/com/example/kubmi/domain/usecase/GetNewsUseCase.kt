package com.example.kubmi.domain.usecase

import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.model.News
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNewsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(): Flow<List<News>> = newsRepository.getAllNews()
        .map { newsList ->
            newsList.sortedByDescending { it.timestamp }
        }
}