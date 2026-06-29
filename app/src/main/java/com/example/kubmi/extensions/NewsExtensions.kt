package com.example.kubmi.extensions

import com.example.kubmi.domain.model.News

/**
 * Extension function для безопасного получения краткого описания новости
 */
fun News.getShortDescription(maxLength: Int = 100): String {
    return if (description.length > maxLength) {
        description.take(maxLength) + "..."
    } else {
        description
    }
}

/**
 * Extension function для форматирования даты новости
 */
fun News.getFormattedDate(): String {
    // Предполагаем формат даты "dd.MM.yyyy", можно адаптировать под разные форматы
    return date.split(".").reversed().joinToString("/") // Меняем на "yyyy/MM/dd"
}

/**
 * Extension function для проверки, содержит ли новость изображение
 */
fun News.hasImage(): Boolean = !imageUrl.isNullOrEmpty()