package com.example.kubmi.domain.model

data class News(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val date: String,
    val imageUrl: String? = null,
    /**
     * Full article blocks (text/images) parsed from the news page.
     * Empty when only the panel preview has been scraped.
     */
    val contentBlocks: List<NewsContentBlock> = emptyList(),
    /**
     * Plain full text of the article (best-effort). Null/empty when not fetched yet.
     */
    val fullText: String? = null
)
