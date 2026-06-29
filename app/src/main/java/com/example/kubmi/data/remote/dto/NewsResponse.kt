package com.example.kubmi.data.remote.dto

data class NewsResponse(
    val news: List<NewsDto>
)

data class NewsDto(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val date: String,
    val imageUrl: String?
)
