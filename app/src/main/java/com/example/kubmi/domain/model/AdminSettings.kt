package com.example.kubmi.domain.model

data class AdminSettings(
    val dataRefreshInterval: Long = 30, // minutes
    val apiEndpoint: String = "https://kubmi.ru",
    val enableMarquee: Boolean = true,
    val marqueeText: String = "Добро пожаловать в КубМИ!",
    val enableAutoStart: Boolean = true
)