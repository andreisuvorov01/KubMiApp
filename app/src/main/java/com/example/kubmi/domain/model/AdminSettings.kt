package com.example.kubmi.domain.model

data class AdminSettings(
    val dataRefreshInterval: Long = 30, // minutes
    val apiEndpoint: String = "https://kubmi.ru",
    val enableAutoStart: Boolean = true
)