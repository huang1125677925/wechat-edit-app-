package com.wechat.editor.model

/**
 * Resolves [LayoutSettings] for an [ArticleTemplate]. Shared by the editor and template-picker preview
 * so the dialog shows the same styling as after applying a template.
 */
object TemplateLayoutProvider {

    fun layoutForTemplate(template: ArticleTemplate): LayoutSettings {
        PreviewStylePresets.layoutForHuashengTemplate(template)?.let { return it }
        return when (template) {
            ArticleTemplate.DEFAULT -> LayoutSettings()
            ArticleTemplate.ELEGANT -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 2.0f,
                primaryColor = "#C0392B",
                textColor = "#2C3E50",
                h1Size = 24,
                h2Size = 20,
                quoteStyle = QuoteStyle.ITALIC
            )
            ArticleTemplate.TECH -> LayoutSettings(
                baseFontSize = 15,
                lineHeight = 1.6f,
                primaryColor = "#2980B9",
                textColor = "#2C3E50",
                codeStyle = CodeStyle.GITHUB
            )
            ArticleTemplate.BUSINESS -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.8f,
                primaryColor = "#1A252F",
                textColor = "#2C3E50",
                subtitleColor = "#555555",
                paragraphSpacing = 16
            )
            ArticleTemplate.LIFE -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.9f,
                primaryColor = "#E74C3C",
                textColor = "#333333",
                backgroundColor = "#FFFEF9"
            )
            ArticleTemplate.EDUCATION -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.8f,
                primaryColor = "#27AE60",
                textColor = "#2C3E50",
                quoteStyle = QuoteStyle.BACKGROUND
            )
            else -> LayoutSettings()
        }
    }
}
