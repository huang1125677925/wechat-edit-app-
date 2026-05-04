package com.wechat.editor.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Article(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val htmlContent: String = "",
    val coverImageUrl: String = "",
    val digest: String = "",
    val author: String = "",
    val template: ArticleTemplate = ArticleTemplate.DEFAULT,
    val layoutSettings: LayoutSettings = LayoutSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE)
        return sdf.format(Date(updatedAt))
    }

    fun getWordCount(): Int {
        return content.replace(Regex("<[^>]*>"), "").trim().length
    }
}

enum class ArticleTemplate(val displayName: String, val description: String) {
    DEFAULT("默认样式", "简洁清晰的默认排版"),
    ELEGANT("优雅文艺", "适合文学、情感类文章"),
    TECH("科技简约", "适合科技、资讯类文章"),
    BUSINESS("商务专业", "适合商业、职场类文章"),
    LIFE("生活休闲", "适合生活、美食类文章"),
    EDUCATION("教育学习", "适合教育、知识类文章")
}

data class LayoutSettings(
    val fontFamily: String = "system",
    val baseFontSize: Int = 16,
    val lineHeight: Float = 1.75f,
    val paragraphSpacing: Int = 12,
    val contentMaxWidth: Int = 677,
    val primaryColor: String = "#1AAD19",
    val textColor: String = "#333333",
    val subtitleColor: String = "#666666",
    val backgroundColor: String = "#FFFFFF",
    val h1Size: Int = 22,
    val h2Size: Int = 20,
    val h3Size: Int = 18,
    val quoteStyle: QuoteStyle = QuoteStyle.LEFT_BORDER,
    val codeStyle: CodeStyle = CodeStyle.DARK
)

enum class QuoteStyle(val displayName: String) {
    LEFT_BORDER("左边框"),
    BACKGROUND("背景色"),
    ITALIC("斜体")
}

enum class CodeStyle(val displayName: String) {
    DARK("深色主题"),
    LIGHT("浅色主题"),
    GITHUB("GitHub风格")
}

data class TextStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val fontSize: Int = 16,
    val color: String = "#333333",
    val backgroundColor: String = "transparent",
    val alignment: TextAlignment = TextAlignment.LEFT,
    val headingLevel: Int = 0
)

enum class TextAlignment(val cssValue: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
    JUSTIFY("justify")
}
