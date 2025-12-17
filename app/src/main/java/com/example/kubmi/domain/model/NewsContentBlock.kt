package com.example.kubmi.domain.model

/**
 * Lightweight, storage-friendly representation of a news article content.
 * We store blocks as JSON (via Gson) in Room, so this is intentionally simple (no polymorphism).
 */
data class NewsContentBlock(
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val alt: String? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
    }
}


