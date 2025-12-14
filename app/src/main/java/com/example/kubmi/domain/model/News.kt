package com.example.kubmi.domain.model

data class News(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val date: String,
    val imageUrl: String? = null
)
