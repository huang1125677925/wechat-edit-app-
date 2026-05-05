package com.wechat.editor.model

/**
 * Layout presets inspired by the open-source [huasheng_editor](https://github.com/alchaincyf/huasheng_editor)
 * `styles.js` themes (classic default, magazine/editorial, tech, deep read, Claude, NYT, Medium, AI dev).
 */
object PreviewStylePresets {

    private const val FF_SYSTEM =
        """-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif"""

    private const val FF_SERIF =
        """Georgia, "Times New Roman", "Songti SC", "SimSun", serif"""

    private const val FF_SONG =
        """"Songti SC", "SimSun", Georgia, serif"""

    fun layoutForHuashengTemplate(template: ArticleTemplate): LayoutSettings? = when (template) {
        ArticleTemplate.CLASSIC_WECHAT -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 16,
            lineHeight = 1.8f,
            paragraphSpacing = 16,
            firstLineIndent = false,
            contentMaxWidth = 740,
            primaryColor = "#3498db",
            textColor = "#3f3f3f",
            subtitleColor = "#666666",
            backgroundColor = "#FFFFFF",
            h1Size = 24,
            h2Size = 22,
            h3Size = 20,
            h1Style = H1Style.UNDERLINE_BORDER,
            h2Style = H2Style.LEFT_BORDER,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.DARK,
            extraPreviewCss = """
                .article-content blockquote { background-color: #fafafa !important; border-left: 3px solid #999 !important; color: #666 !important; }
                .article-content pre { border-radius: 8px; }
            """.trimIndent()
        )
        ArticleTemplate.HS_MAGAZINE -> LayoutSettings(
            fontFamily = "song",
            baseFontSize = 17,
            lineHeight = 1.85f,
            paragraphSpacing = 20,
            firstLineIndent = false,
            contentMaxWidth = 720,
            primaryColor = "#c9302c",
            textColor = "#2c2c2c",
            subtitleColor = "#6b5344",
            backgroundColor = "#fffef9",
            h1Size = 32,
            h2Size = 26,
            h3Size = 21,
            h1Style = H1Style.PLAIN_BOLD,
            // Magazine H2 look (double horizontal rules) is applied in [extraPreviewCss].
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.ITALIC,
            codeStyle = CodeStyle.LIGHT,
            extraPreviewCss = """
                .article-title { font-family: "Bodoni MT", Didot, Georgia, serif !important; font-weight: 400 !important; color: #c9302c !important; text-align: center; letter-spacing: -0.02em; }
                .article-author { font-family: $FF_SONG !important; color: #6b5344 !important; border-bottom-color: #e0dcd0 !important; }
                .article-content { font-family: "Crimson Text", Garamond, Georgia, serif !important; text-align: justify; letter-spacing: 0.02em; }
                .article-content h1 { font-family: "Bodoni MT", Didot, Georgia, serif !important; font-weight: 400 !important; color: #c9302c !important; text-align: center; }
                .article-content h2 { font-family: "Bodoni MT", Didot, Georgia, serif !important; font-weight: 400 !important; border-top: 1px solid #c9302c; border-bottom: 1px solid #c9302c; padding: 10px 0; text-align: center; }
                .article-content blockquote { background: transparent !important; border-left: none !important; text-align: center; font-style: italic; font-family: "Bodoni MT", Didot, Georgia, serif !important; color: #2c2c2c !important; }
                .article-content code { font-family: "Courier Prime", "Courier New", monospace !important; color: #c9302c !important; border: 1px solid #e0e0e0; }
            """.trimIndent()
        )
        ArticleTemplate.HS_TECH -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 16,
            lineHeight = 1.8f,
            paragraphSpacing = 18,
            contentMaxWidth = 740,
            primaryColor = "#0066cc",
            textColor = "#3a3a3a",
            subtitleColor = "#666666",
            backgroundColor = "#FFFFFF",
            h1Size = 26,
            h2Size = 22,
            h3Size = 20,
            h1Style = H1Style.UNDERLINE_BORDER,
            h2Style = H2Style.LEFT_BORDER,
            h3Style = H3Style.THIN_LEFT_BORDER,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.DARK,
            pasteStrongColor = "#1a1a1a",
            pasteStrongBackgroundColor = "#fff3cd",
            extraPreviewCss = """
                .article-content h1 { color: #1a1a1a !important; border-bottom: 3px solid #0066cc !important; padding-bottom: 12px; }
                .article-content h2 { border-left: 5px solid #00a67d !important; padding-left: 16px; background: linear-gradient(to right, #f0f9ff 0%, transparent 100%); }
                .article-content h3 { border-left: 3px solid #ff9800 !important; }
                .article-content strong { color: #1a1a1a !important; background-color: #fff3cd !important; padding: 2px 4px; border-radius: 8px; }
                .article-content blockquote { background-color: #f5f9fc !important; border-left: 3px solid #2196f3 !important; }
                .article-content code { font-family: "Fira Code", Consolas, Monaco, monospace !important; background-color: #ffe6e6 !important; color: #d63031 !important; border-radius: 8px; }
                .article-content hr { height: 2px; background: linear-gradient(to right, transparent, #0066cc, transparent); border: none !important; }
            """.trimIndent()
        )
        ArticleTemplate.HS_AI_CODER -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 16,
            lineHeight = 1.8f,
            paragraphSpacing = 18,
            contentMaxWidth = 700,
            primaryColor = "#C2410C",
            textColor = "#1A1A1A",
            subtitleColor = "#6B6B6B",
            backgroundColor = "#FAFAF9",
            h1Size = 28,
            h2Size = 22,
            h3Size = 18,
            h1Style = H1Style.UNDERLINE_BORDER,
            h2Style = H2Style.LEFT_BORDER,
            h3Style = H3Style.THIN_LEFT_BORDER,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.GITHUB,
            extraPreviewCss = """
                .article-content h1 { border-bottom-color: #C2410C !important; color: #1A1A1A !important; }
                .article-content h2 { border-top: 3px solid #C2410C; padding-top: 16px; border-left: none !important; padding-left: 0 !important; }
                .article-content h3 { border-left-color: #C2410C !important; }
                .article-content strong { color: #C2410C !important; }
                .article-content a { color: #C2410C !important; }
                .article-content blockquote { background-color: #FFF7ED !important; border-left: 4px solid #C2410C !important; border-radius: 0 4px 4px 0; }
                .article-content code { font-family: "JetBrains Mono", "SF Mono", Consolas, monospace !important; background-color: #F5F5F0 !important; color: #C2410C !important; border: 1px solid #E5E5E5; }
            """.trimIndent()
        )
        ArticleTemplate.HS_DEEP_READ -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 17,
            lineHeight = 1.8f,
            paragraphSpacing = 20,
            contentMaxWidth = 680,
            primaryColor = "#0066cc",
            textColor = "#1a1a1a",
            subtitleColor = "#4a4a4a",
            backgroundColor = "#FFFFFF",
            h1Size = 26,
            h2Size = 22,
            h3Size = 19,
            h1Style = H1Style.PLAIN_BOLD,
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.GITHUB,
            extraPreviewCss = """
                .article-content h1, .article-content h2 { letter-spacing: -0.02em; color: #0a0a0a !important; }
                .article-content h2 { color: #0a0a0a !important; }
                .article-content blockquote { background-color: #f8f9fa !important; border-left: 4px solid #0a0a0a !important; font-style: normal; }
                .article-content pre { border: 1px solid #e1e4e8; }
            """.trimIndent()
        )
        ArticleTemplate.HS_CLAUDE -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 17,
            lineHeight = 1.8f,
            paragraphSpacing = 20,
            contentMaxWidth = 700,
            primaryColor = "#C15F3C",
            textColor = "#2b2b2b",
            subtitleColor = "#5a5a5a",
            backgroundColor = "#faf9f7",
            h1Size = 32,
            h2Size = 26,
            h3Size = 22,
            h1Style = H1Style.PLAIN_BOLD,
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.DARK,
            pasteStrongColor = "#C15F3C",
            pasteStrongBackgroundColor = "rgba(193, 95, 60, 0.08)",
            extraPreviewCss = """
                .article-title { color: #C15F3C !important; letter-spacing: -0.02em; }
                .article-content h1 { color: #C15F3C !important; letter-spacing: -0.02em; }
                .article-content h2 { color: #C15F3C !important; }
                .article-content strong { color: #C15F3C !important; background-color: rgba(193, 95, 60, 0.08) !important; padding: 2px 6px; border-radius: 3px; }
                .article-content a { color: #C15F3C !important; border-bottom: 1px solid rgba(193, 95, 60, 0.4); }
                .article-content blockquote { background: linear-gradient(135deg, rgba(193, 95, 60, 0.06) 0%, rgba(157, 200, 141, 0.06) 100%) !important; border-left: 4px solid #C15F3C !important; border-radius: 6px; font-style: italic; }
                .article-content code { font-family: "SF Mono", Consolas, Monaco, monospace !important; background-color: rgba(193, 95, 60, 0.08) !important; color: #C15F3C !important; border: 1px solid rgba(193, 95, 60, 0.15); border-radius: 6px; }
                .article-content hr { height: 2px; background: linear-gradient(to right, transparent, rgba(193, 95, 60, 0.3), rgba(157, 200, 141, 0.3), transparent); border: none !important; max-width: 200px; margin-left: auto; margin-right: auto; }
            """.trimIndent()
        )
        ArticleTemplate.HS_NYT -> LayoutSettings(
            fontFamily = "serif",
            baseFontSize = 18,
            lineHeight = 1.8f,
            paragraphSpacing = 20,
            contentMaxWidth = 680,
            primaryColor = "#326891",
            textColor = "#121212",
            subtitleColor = "#333333",
            backgroundColor = "#FFFFFF",
            h1Size = 42,
            h2Size = 32,
            h3Size = 24,
            h1Style = H1Style.UNDERLINE_BORDER,
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.LIGHT,
            extraPreviewCss = """
                .article-title { font-family: Georgia, serif !important; color: #000 !important; letter-spacing: -0.02em; border-bottom: 1px solid #000 !important; padding-bottom: 16px; }
                .article-content { font-family: Georgia, "Times New Roman", serif !important; }
                .article-content h1 { font-family: Georgia, serif !important; color: #000 !important; letter-spacing: -0.02em; border-bottom: 1px solid #000 !important; }
                .article-content h2, .article-content h3 { font-family: Georgia, serif !important; color: #000 !important; }
                .article-content h3 { color: #121212 !important; }
                .article-content strong { color: #000 !important; }
                .article-content a { color: #326891 !important; }
                .article-content blockquote { background-color: #f7f7f7 !important; border-left: 5px solid #121212 !important; font-style: italic; font-family: Georgia, serif !important; }
                .article-content hr { max-width: 100px; margin-left: auto; margin-right: auto; background-color: #ddd !important; }
            """.trimIndent()
        )
        ArticleTemplate.HS_MEDIUM -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 17,
            lineHeight = 1.75f,
            paragraphSpacing = 20,
            contentMaxWidth = 680,
            primaryColor = "#242424",
            textColor = "#242424",
            subtitleColor = "#757575",
            backgroundColor = "#FFFFFF",
            h1Size = 28,
            h2Size = 24,
            h3Size = 20,
            h1Style = H1Style.PLAIN_BOLD,
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.LIGHT,
            extraPreviewCss = """
                .article-content h1, .article-content h2, .article-content h3 { font-family: Georgia, "Times New Roman", serif !important; letter-spacing: -0.02em; color: #242424 !important; }
                .article-content { letter-spacing: -0.003em; }
                .article-content a { color: #242424 !important; border-bottom: 1px solid #242424; }
                .article-content blockquote { background: transparent !important; border-left: 3px solid #242424 !important; font-style: italic; font-family: Georgia, "Times New Roman", serif !important; padding: 0 20px !important; }
                .article-content code { font-family: Menlo, Monaco, "Courier New", monospace !important; color: #d73a49 !important; }
                .article-content hr { max-width: 300px; margin-left: auto; margin-right: auto; background-color: #e6e6e6 !important; }
            """.trimIndent()
        )
        ArticleTemplate.WECHAT_CYBER_ZEN -> LayoutSettings(
            fontFamily = "system",
            baseFontSize = 16,
            lineHeight = 1.75f,
            paragraphSpacing = 20,
            firstLineIndent = false,
            contentMaxWidth = 677,
            primaryColor = "#C0504D",
            textColor = "#3E3E3E",
            subtitleColor = "#888888",
            backgroundColor = "#F7F7F7",
            h1Size = 24,
            h2Size = 21,
            h3Size = 18,
            h1Style = H1Style.PLAIN_BOLD,
            h2Style = H2Style.PLAIN_COLOR,
            h3Style = H3Style.BOLD_SUBTITLE,
            quoteStyle = QuoteStyle.LEFT_BORDER,
            codeStyle = CodeStyle.LIGHT,
            pasteTitleColor = "#C0504D",
            pasteLinkColor = "#576B95",
            pasteStrongColor = "#3E3E3E",
            pasteCodeBackground = "#E8F4F8",
            pasteCodeForeground = "#333333",
            extraPreviewCss = """
                .article-title { color: #C0504D !important; font-weight: bold !important; }
                .article-content h1, .article-content h2 { color: #C0504D !important; font-weight: bold !important; }
                .article-content h3 { color: #3E3E3E !important; }
                .article-content a { color: #576B95 !important; text-decoration: none !important; border-bottom: none !important; }
                .article-content strong { color: #3E3E3E !important; font-weight: bold !important; }
                .article-content code { font-family: "SF Mono", "PingFang SC", Consolas, monospace !important; background-color: #E8F4F8 !important; color: #333333 !important; border: none !important; padding: 2px 6px !important; border-radius: 4px !important; }
                .article-content blockquote { background-color: #fafafa !important; border-left: 3px solid #C0504D !important; color: #5a5a5a !important; padding: 10px 14px !important; }
                .article-content .cyber-zen-em { color: #C0504D !important; font-weight: bold !important; }
                .article-content .cyber-zen-note {
                    border-bottom: 1px dashed #A9D18E;
                    padding-bottom: 2px;
                    box-decoration-break: clone;
                    -webkit-box-decoration-break: clone;
                }
                .article-content hr { border: none !important; border-top: 1px solid #e8e8e8 !important; margin: 28px 0 !important; }
            """.trimIndent()
        )
        else -> null
    }

    /** CSS `font-family` value for [LayoutSettings.fontFamily] token. */
    fun bodyFontStack(token: String): String = when (token) {
        "serif" -> FF_SERIF
        "song" -> FF_SONG
        else -> FF_SYSTEM
    }
}
