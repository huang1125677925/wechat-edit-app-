package com.wechat.editor.model

/** Mirrors JSON from https://suyxh.github.io/ai-news-aggregator/data/latest-24h.json */

data class AiNewsFeed(
    val generatedAt: String,
    val windowHours: Int,
    val totalItems: Int,
    val items: List<AiNewsItem>
)

data class AiNewsItem(
    val id: String,
    val siteName: String,
    val source: String,
    val title: String,
    val url: String,
    val publishedAt: String?,
    val titleZh: String?,
    val titleBilingual: String?
) {
    /** Prefer bilingual / Chinese title when present. */
    fun displayTitle(): String {
        val bi = titleBilingual?.trim().orEmpty()
        if (bi.isNotEmpty()) return bi
        val zh = titleZh?.trim().orEmpty()
        if (zh.isNotEmpty()) return zh
        return title.trim()
    }
}
