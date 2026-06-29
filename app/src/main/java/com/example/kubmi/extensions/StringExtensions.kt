package com.example.kubmi.extensions

/**
 * Extension function для безопасной проверки, является ли строка названием группы
 */
fun String.isGroupName(): Boolean {
    if (this.isEmpty()) return false
    
    // Общие паттерны для названий групп:
    // - Содержит цифры и буквы
    // - Может начинаться с буквы или цифры
    // - Обычно длина от 3 до 10 символов
    val patterns = listOf(
        Regex("[A-Z]{1,2}\\d{2,3}[A-Z]*", RegexOption.IGNORE_CASE), // Буквы-цифры-буквы
        Regex("\\d{2,3}[A-Z]{1,2}", RegexOption.IGNORE_CASE), // Цифры-буквы
        Regex("[A-Z]{1,2}-?\\d{2,3}", RegexOption.IGNORE_CASE), // Буквы-цифры (может быть с тире)
        Regex("\\d{2,3}-?[A-Z]{1,2}", RegexOption.IGNORE_CASE) // Цифры-буквы (может быть с тире)
    )
    
    return patterns.any { it.matches(this) } && this.length in 3..10
}

/**
 * Extension function для безопасной проверки, является ли строка именем преподавателя
 */
fun String.isTeacherName(): Boolean {
    if (this.isEmpty()) return false
    
    // Общие паттерны для ФИО преподавателей:
    // - Состоит из 2-3 слов
    // - Начинается с заглавной буквы
    // - Может содержать инициалы (одна буква с точкой)
    val namePattern = Regex("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ]\\.[А-ЯЁ]\\.|[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ]\\.|[A-Z][a-z]+\\s+[A-Z]\\.[A-Z]\\.|[A-Z][a-z]+\\s+[A-Z][a-z]+\\s+[A-Z]\\.", RegexOption.IGNORE_CASE)
    
    val words = this.trim().split("\\s+".toRegex())
    
    // Проверяем общий паттерн
    if (namePattern.containsMatchIn(this)) {
        return true
    }
    
    // Альтернативная проверка: 2-3 слова, начинающихся с заглавной буквы
    if (words.size in 2..4 && words.all { it.firstOrNull()?.isUpperCase() == true }) {
        return true
    }
    
    return false
}

/**
 * Extension function для безопасного получения подстроки до разделителя
 */
fun String.substringBeforeSafe(delimiter: String, defaultValue: String = this): String {
    return if (this.contains(delimiter)) {
        this.substringBefore(delimiter)
    } else {
        defaultValue
    }
}

/**
 * Extension function для безопасного получения подстроки после разделителя
 */
fun String.substringAfterSafe(delimiter: String, defaultValue: String = ""): String {
    return if (this.contains(delimiter)) {
        this.substringAfter(delimiter)
    } else {
        defaultValue
    }
}