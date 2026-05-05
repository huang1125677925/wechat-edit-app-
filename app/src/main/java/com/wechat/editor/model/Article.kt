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
    /** Inspired by huasheng_editor `wechat-default`. */
    CLASSIC_WECHAT("经典", "公众号常用蓝边标题与清晰正文"),
    /** Inspired by huasheng_editor `hische-editorial` (杂志风). */
    HS_MAGAZINE("杂志风", "衬线大字标题、暖色纸张感背景"),
    /** Inspired by huasheng_editor `wechat-tech`. */
    HS_TECH("技术", "蓝绿点缀、代码块对比强"),
    /** Inspired by developer-blog aesthetics (warm docs + monospace). */
    HS_AI_CODER("AI Coder", "适合 AI / 开发类长文与代码片段"),
    /** Inspired by huasheng_editor `wechat-deepread`. */
    HS_DEEP_READ("深度阅读", "高对比、留白舒适的精读版式"),
    /** Inspired by huasheng_editor `wechat-anthropic` (Claude). */
    HS_CLAUDE("Claude", "暖灰底、陶土色点缀"),
    /** Inspired by huasheng_editor `wechat-nyt`. */
    HS_NYT("纽约时报", "Georgia 大标题、新闻纸质感"),
    /** Inspired by huasheng_editor `wechat-medium`. */
    HS_MEDIUM("Medium", "Georgia 标题、接近 Medium 长文"),
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
    val firstLineIndent: Boolean = false,
    val contentMaxWidth: Int = 677,
    val primaryColor: String = "#1AAD19",
    val textColor: String = "#333333",
    val subtitleColor: String = "#666666",
    val backgroundColor: String = "#FFFFFF",
    val h1Size: Int = 22,
    val h2Size: Int = 20,
    val h3Size: Int = 18,
    val h1Style: H1Style = H1Style.UNDERLINE_BORDER,
    val h2Style: H2Style = H2Style.LEFT_BORDER,
    val h3Style: H3Style = H3Style.THIN_LEFT_BORDER,
    val quoteStyle: QuoteStyle = QuoteStyle.LEFT_BORDER,
    val codeStyle: CodeStyle = CodeStyle.DARK,
    /**
     * Appended inside the preview `<style>` block after base rules. Use more specific selectors
     * (e.g. `.article-content h1`) to refine presets inspired by third-party style packs.
     */
    val extraPreviewCss: String = ""
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

/** Visual style preset for H1 headings. */
enum class H1Style(val displayName: String, val description: String) {
    UNDERLINE_BORDER("底部边框", "粗体 + 下划线边框（默认）"),
    BACKGROUND_BLOCK("背景色块", "全宽色块背景，白色文字"),
    LEFT_ACCENT("左侧色块", "左侧粗条 + 颜色文字"),
    CENTERED_LINE("居中分割线", "居中显示，上下装饰线"),
    PLAIN_BOLD("简洁粗体", "仅加粗，无额外装饰")
}

/** Visual style preset for H2 headings. */
enum class H2Style(val displayName: String, val description: String) {
    LEFT_BORDER("左边框", "左侧竖线装饰（默认）"),
    DOT_PREFIX("圆点前缀", "实心圆点 + 颜色文字"),
    UNDERLINE("下划虚线", "文字 + 下方虚线"),
    BACKGROUND_LIGHT("浅色背景", "圆角浅色背景色块"),
    PLAIN_COLOR("纯色文字", "仅变色，无其他装饰")
}

/** Visual style preset for H3 headings. */
enum class H3Style(val displayName: String, val description: String) {
    THIN_LEFT_BORDER("细左边框", "细竖线 + 正文色文字（默认）"),
    ARROW_PREFIX("箭头前缀", "▶ 符号前缀"),
    BOLD_SUBTITLE("加粗副标题", "加粗、略小，无装饰线"),
    ITALIC_COLOR("斜体颜色", "斜体 + 主色"),
    CIRCLE_BULLET("圆圈序号", "○ 前缀 + 正文色")
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
