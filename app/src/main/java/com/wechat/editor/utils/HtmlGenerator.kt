package com.wechat.editor.utils

import com.wechat.editor.model.CodeStyle
import com.wechat.editor.model.H1Style
import com.wechat.editor.model.H2Style
import com.wechat.editor.model.H3Style
import com.wechat.editor.model.LayoutSettings
import com.wechat.editor.model.PreviewStylePresets
import com.wechat.editor.model.QuoteStyle

object HtmlGenerator {

    fun generateHtml(markdownContent: String, layout: LayoutSettings, title: String, author: String): String {
        val bodyContent = convertMarkdownToHtml(markdownContent)
        val css = generateCss(layout)
        val titleHtml = if (title.isNotBlank()) {
            "<h1 class=\"article-title\">$title</h1>"
        } else ""
        val authorHtml = if (author.isNotBlank()) {
            "<p class=\"article-author\">作者：$author</p>"
        } else ""

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(title)}</title>
    <style>$css</style>
</head>
<body>
    <div class="article-container">
        $titleHtml
        $authorHtml
        <div class="article-content">
            $bodyContent
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    /**
     * HTML fragment for pasting into the WeChat Official Account editor.
     * The backend strips or ignores document-level &lt;style&gt;; this output uses inline styles only.
     */
    fun generateWeChatPasteHtml(markdownContent: String, layout: LayoutSettings, title: String, author: String): String {
        val bodyContent = convertMarkdownToHtml(markdownContent)
        val styledBody = applyWeChatInlineStyles(bodyContent, layout)
        val ff = PreviewStylePresets.bodyFontStack(layout.fontFamily).replace("\"", "'")
        val titleColor = layout.pasteTitleColor ?: layout.textColor
        val titleBlock = if (title.isNotBlank()) {
            """<h1 style="font-size:${layout.h1Size}px;font-weight:bold;color:$titleColor;margin:0 0 8px;line-height:1.4;">${escapeHtml(title)}</h1>"""
        } else ""
        val authorBlock = if (author.isNotBlank()) {
            """<p style="font-size:14px;color:${layout.subtitleColor};margin:0 0 20px;padding-bottom:16px;border-bottom:1px solid #eeeeee;">作者：${escapeHtml(author)}</p>"""
        } else ""
        return buildString {
            append(
                """<section style="box-sizing:border-box;max-width:${layout.contentMaxWidth}px;margin:0 auto;padding:20px 16px 40px;font-family:$ff;background-color:${layout.backgroundColor};color:${layout.textColor};font-size:${layout.baseFontSize}px;line-height:${layout.lineHeight};">"""
            )
            append(titleBlock)
            append(authorBlock)
            append(styledBody)
            append("</section>")
        }
    }

    internal fun applyWeChatInlineStyles(html: String, layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val sc = layout.subtitleColor
        val ps = layout.paragraphSpacing
        val h4s = layout.h3Size - 2
        val bfs = layout.baseFontSize

        val quoteStyleAttr = when (layout.quoteStyle) {
            QuoteStyle.LEFT_BORDER ->
                "margin:16px 0;border-left:4px solid $pc;padding:8px 16px;background-color:#f9f9f9;color:$sc;"
            QuoteStyle.BACKGROUND ->
                "margin:16px 0;background-color:#f0f7f0;padding:12px 16px;border-radius:4px;color:$tc;border-left:none;"
            QuoteStyle.ITALIC ->
                "margin:16px 0;font-style:italic;padding:8px 16px;color:$sc;border:none;"
        }

        val (preBg, preFg) = when {
            layout.pasteCodeBackground != null && layout.pasteCodeForeground != null ->
                Pair(layout.pasteCodeBackground, layout.pasteCodeForeground)
            layout.codeStyle == CodeStyle.DARK -> Pair("#282c34", "#abb2bf")
            layout.codeStyle == CodeStyle.LIGHT -> Pair("#f5f5f5", "#333333")
            else -> Pair("#f6f8fa", "#24292e")
        }
        val preOpen =
            """<pre style="margin:16px 0;border-radius:6px;overflow-x:auto;background-color:$preBg;color:$preFg;">"""
        val codeBlockOpenStyle =
            "display:block;padding:16px;font-family:'Courier New',Courier,monospace;font-size:14px;line-height:1.5;background-color:transparent;color:inherit;"

        var s = html
        s = s.replace(Regex("<pre><code([^>]*)>")) { m ->
            val extra = m.groupValues[1]
            """$preOpen<code style="$codeBlockOpenStyle"$extra>"""
        }
        s = s.replace("<blockquote>", "<blockquote style=\"$quoteStyleAttr\">")
        s = s.replace("<hr>", "<hr style=\"border:none;border-top:1px solid #eeeeee;margin:24px 0;\" />")
        s = s.replace("<hr/>", "<hr style=\"border:none;border-top:1px solid #eeeeee;margin:24px 0;\" />")
        val indentStyle = if (layout.firstLineIndent) "text-indent:2em;" else ""
        s = s.replace(
            "<p>",
            "<p style=\"margin:0 0 ${ps}px;text-align:justify;$indentStyle\">"
        )
        s = s.replace("<h1>", "<h1 style=\"${h1InlineStyle(layout)}\">")
        s = s.replace("<h2>", "<h2 style=\"${h2InlineStyle(layout)}\">")
        s = s.replace("<h3>", "<h3 style=\"${h3InlineStyle(layout)}\">")
        s = s.replace(
            "<h4>",
            "<h4 style=\"font-size:${h4s}px;color:$tc;margin:14px 0 6px;font-weight:bold;padding:4px 10px;background-color:rgba(0,0,0,0.04);border-radius:4px;line-height:1.4;\">"
        )
        s = s.replace(
            "<h5>",
            "<h5 style=\"font-size:${bfs + 1}px;color:$sc;margin:12px 0 4px;font-weight:bold;text-transform:uppercase;letter-spacing:0.05em;line-height:1.4;\">"
        )
        s = s.replace(
            "<h6>",
            "<h6 style=\"font-size:${bfs}px;color:$sc;margin:10px 0 4px;font-weight:bold;font-style:italic;line-height:1.4;\">"
        )
        s = s.replace("<ul>", "<ul style=\"margin:8px 0 ${ps}px 24px;padding:0;\">")
        s = s.replace("<ol>", "<ol style=\"margin:8px 0 ${ps}px 24px;padding:0;\">")
        s = s.replace("<li>", "<li style=\"margin-bottom:6px;\">")
        val strongColor = layout.pasteStrongColor ?: tc
        s = s.replace("<strong>", "<strong style=\"font-weight:bold;color:$strongColor;\">")
        s = s.replace("<em>", "<em style=\"font-style:italic;\">")
        s = s.replace("<del>", "<del style=\"text-decoration:line-through;color:$sc;\">")
        s = s.replace("<u>", "<u style=\"text-decoration:underline;\">")
        val linkColor = layout.pasteLinkColor ?: pc
        s = s.replace(
            "<a href=",
            "<a style=\"color:$linkColor;text-decoration:none;\" href="
        )
        s = s.replace(
            "<img ",
            "<img style=\"max-width:100%;height:auto;border-radius:4px;display:block;margin:16px auto;\" "
        )
        s = s.replace(
            "<table>",
            "<table style=\"width:100%;border-collapse:collapse;margin:16px 0;font-size:14px;\">"
        )
        s = s.replace(
            "<th>",
            "<th style=\"border:1px solid #e0e0e0;padding:8px 12px;text-align:left;background-color:#f5f5f5;font-weight:bold;\">"
        )
        s = s.replace(
            "<td>",
            "<td style=\"border:1px solid #e0e0e0;padding:8px 12px;text-align:left;\">"
        )
        // Inline code only (not inside pre — those were rewritten to include style on opening code)
        val inlineCodeBg = layout.pasteCodeBackground ?: "#f0f0f0"
        val inlineCodeFg = layout.pasteCodeForeground ?: "#e74c3c"
        s = s.replace(
            "<code>",
            "<code style=\"font-family:'Courier New',Courier,monospace;font-size:14px;background-color:$inlineCodeBg;color:$inlineCodeFg;padding:2px 6px;border-radius:3px;\">"
        )
        return s
    }

    fun generateCss(layout: LayoutSettings): String {
        val quoteStyle = when (layout.quoteStyle) {
            QuoteStyle.LEFT_BORDER -> """
                border-left: 4px solid ${layout.primaryColor};
                padding: 8px 16px;
                background-color: #f9f9f9;
                color: ${layout.subtitleColor};
            """
            QuoteStyle.BACKGROUND -> """
                background-color: #f0f7f0;
                border-left: none;
                padding: 12px 16px;
                border-radius: 4px;
                color: ${layout.textColor};
            """
            QuoteStyle.ITALIC -> """
                font-style: italic;
                padding: 8px 16px;
                color: ${layout.subtitleColor};
                border: none;
            """
        }

        val codeStyle = when (layout.codeStyle) {
            CodeStyle.DARK -> "background:#282c34;color:#abb2bf;"
            CodeStyle.LIGHT -> "background:#f5f5f5;color:#333;"
            CodeStyle.GITHUB -> "background:#f6f8fa;color:#24292e;"
        }

        val bodyFf = PreviewStylePresets.bodyFontStack(layout.fontFamily)
        val extra = layout.extraPreviewCss.trim().let { if (it.isNotEmpty()) "\n$it" else "" }
        return """
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                font-family: $bodyFf;
                background-color: ${layout.backgroundColor};
                color: ${layout.textColor};
                font-size: ${layout.baseFontSize}px;
                line-height: ${layout.lineHeight};
            }
            .article-container {
                max-width: ${layout.contentMaxWidth}px;
                margin: 0 auto;
                padding: 20px 16px 40px;
            }
            .article-title {
                font-size: ${layout.h1Size}px;
                font-weight: bold;
                color: ${layout.textColor};
                margin-bottom: 8px;
                line-height: 1.4;
            }
            .article-author {
                font-size: 14px;
                color: ${layout.subtitleColor};
                margin-bottom: 20px;
                padding-bottom: 16px;
                border-bottom: 1px solid #eee;
            }
            .article-content p {
                margin-bottom: ${layout.paragraphSpacing}px;
                text-align: justify;
                ${if (layout.firstLineIndent) "text-indent: 2em;" else ""}
            }
            ${generateH1Css(layout)}
            ${generateH2Css(layout)}
            ${generateH3Css(layout)}
            h4 {
                font-size: ${layout.h3Size - 2}px;
                color: ${layout.textColor};
                margin: 14px 0 6px;
                font-weight: bold;
                padding: 4px 10px;
                background-color: rgba(0,0,0,0.04);
                border-radius: 4px;
                line-height: 1.4;
            }
            h5 {
                font-size: ${layout.baseFontSize + 1}px;
                color: ${layout.subtitleColor};
                margin: 12px 0 4px;
                font-weight: bold;
                text-transform: uppercase;
                letter-spacing: 0.05em;
                line-height: 1.4;
            }
            h6 {
                font-size: ${layout.baseFontSize}px;
                color: ${layout.subtitleColor};
                margin: 10px 0 4px;
                font-weight: bold;
                font-style: italic;
                line-height: 1.4;
            }
            strong { font-weight: bold; color: ${layout.textColor}; }
            em { font-style: italic; }
            u { text-decoration: underline; }
            del { text-decoration: line-through; color: ${layout.subtitleColor}; }
            a { color: ${layout.primaryColor}; text-decoration: none; }
            a:hover { text-decoration: underline; }
            blockquote {
                margin: 16px 0;
                $quoteStyle
            }
            pre {
                margin: 16px 0;
                border-radius: 6px;
                overflow-x: auto;
                $codeStyle
            }
            pre code {
                display: block;
                padding: 16px;
                font-family: 'Courier New', Courier, monospace;
                font-size: 14px;
                line-height: 1.5;
                background: transparent;
            }
            code {
                font-family: 'Courier New', Courier, monospace;
                font-size: 14px;
                background: #f0f0f0;
                color: #e74c3c;
                padding: 2px 6px;
                border-radius: 3px;
            }
            ul, ol { margin: 8px 0 ${layout.paragraphSpacing}px 24px; }
            li { margin-bottom: 6px; }
            hr {
                border: none;
                border-top: 1px solid #eee;
                margin: 24px 0;
            }
            img {
                max-width: 100%;
                height: auto;
                border-radius: 4px;
                display: block;
                margin: 16px auto;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 16px 0;
                font-size: 14px;
            }
            th, td {
                border: 1px solid #e0e0e0;
                padding: 8px 12px;
                text-align: left;
            }
            th { background-color: #f5f5f5; font-weight: bold; }
            tr:nth-child(even) { background-color: #fafafa; }
            $extra
        """.trimIndent()
    }

    private fun generateH1Css(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val size = layout.h1Size
        val plainBoldColor = layout.pasteTitleColor ?: tc
        return when (layout.h1Style) {
            H1Style.UNDERLINE_BORDER ->
                "h1 { font-size:${size}px; color:$pc; margin:28px 0 14px; font-weight:bold; padding-bottom:8px; border-bottom:3px solid $pc; line-height:1.35; }"
            H1Style.BACKGROUND_BLOCK ->
                "h1 { font-size:${size}px; color:#ffffff; background-color:$pc; margin:28px 0 14px; font-weight:bold; padding:10px 16px; border-radius:4px; line-height:1.35; }"
            H1Style.LEFT_ACCENT ->
                "h1 { font-size:${size}px; color:$pc; margin:28px 0 14px; font-weight:bold; padding-left:14px; border-left:6px solid $pc; line-height:1.35; }"
            H1Style.CENTERED_LINE ->
                "h1 { font-size:${size}px; color:$tc; margin:28px 0 14px; font-weight:bold; text-align:center; padding:10px 0; border-top:2px solid $pc; border-bottom:2px solid $pc; line-height:1.35; }"
            H1Style.PLAIN_BOLD ->
                "h1 { font-size:${size}px; color:$plainBoldColor; margin:28px 0 14px; font-weight:bold; line-height:1.35; }"
        }
    }

    private fun generateH2Css(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val size = layout.h2Size
        return when (layout.h2Style) {
            H2Style.LEFT_BORDER ->
                "h2 { font-size:${size}px; color:$pc; margin:22px 0 10px; font-weight:bold; padding-left:10px; border-left:4px solid $pc; line-height:1.4; }"
            H2Style.DOT_PREFIX ->
                "h2::before { content:'●'; color:$pc; margin-right:8px; font-size:${size - 4}px; } h2 { font-size:${size}px; color:$pc; margin:22px 0 10px; font-weight:bold; line-height:1.4; }"
            H2Style.UNDERLINE ->
                "h2 { font-size:${size}px; color:$tc; margin:22px 0 10px; font-weight:bold; padding-bottom:6px; border-bottom:1px dashed $pc; line-height:1.4; }"
            H2Style.BACKGROUND_LIGHT ->
                "h2 { font-size:${size}px; color:$pc; margin:22px 0 10px; font-weight:bold; padding:6px 12px; background-color:${pc}1A; border-radius:6px; line-height:1.4; }"
            H2Style.PLAIN_COLOR ->
                "h2 { font-size:${size}px; color:$pc; margin:22px 0 10px; font-weight:bold; line-height:1.4; }"
        }
    }

    private fun generateH3Css(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val sc = layout.subtitleColor
        val size = layout.h3Size
        return when (layout.h3Style) {
            H3Style.THIN_LEFT_BORDER ->
                "h3 { font-size:${size}px; color:$tc; margin:18px 0 8px; font-weight:bold; padding-left:6px; border-left:3px solid $sc; line-height:1.4; }"
            H3Style.ARROW_PREFIX ->
                "h3::before { content:'▶'; color:$pc; margin-right:6px; font-size:${size - 2}px; } h3 { font-size:${size}px; color:$tc; margin:18px 0 8px; font-weight:bold; line-height:1.4; }"
            H3Style.BOLD_SUBTITLE ->
                "h3 { font-size:${size}px; color:$tc; margin:18px 0 8px; font-weight:bold; line-height:1.4; }"
            H3Style.ITALIC_COLOR ->
                "h3 { font-size:${size}px; color:$pc; margin:18px 0 8px; font-weight:bold; font-style:italic; line-height:1.4; }"
            H3Style.CIRCLE_BULLET ->
                "h3::before { content:'○'; color:$pc; margin-right:6px; } h3 { font-size:${size}px; color:$tc; margin:18px 0 8px; font-weight:bold; line-height:1.4; }"
        }
    }

    private fun h1InlineStyle(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val size = layout.h1Size
        val plainBoldColor = layout.pasteTitleColor ?: tc
        return when (layout.h1Style) {
            H1Style.UNDERLINE_BORDER ->
                "font-size:${size}px;color:$pc;margin:28px 0 14px;font-weight:bold;padding-bottom:8px;border-bottom:3px solid $pc;line-height:1.35;"
            H1Style.BACKGROUND_BLOCK ->
                "font-size:${size}px;color:#ffffff;background-color:$pc;margin:28px 0 14px;font-weight:bold;padding:10px 16px;border-radius:4px;line-height:1.35;"
            H1Style.LEFT_ACCENT ->
                "font-size:${size}px;color:$pc;margin:28px 0 14px;font-weight:bold;padding-left:14px;border-left:6px solid $pc;line-height:1.35;"
            H1Style.CENTERED_LINE ->
                "font-size:${size}px;color:$tc;margin:28px 0 14px;font-weight:bold;text-align:center;padding:10px 0;border-top:2px solid $pc;border-bottom:2px solid $pc;line-height:1.35;"
            H1Style.PLAIN_BOLD ->
                "font-size:${size}px;color:$plainBoldColor;margin:28px 0 14px;font-weight:bold;line-height:1.35;"
        }
    }

    private fun h2InlineStyle(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val size = layout.h2Size
        return when (layout.h2Style) {
            H2Style.LEFT_BORDER ->
                "font-size:${size}px;color:$pc;margin:22px 0 10px;font-weight:bold;padding-left:10px;border-left:4px solid $pc;line-height:1.4;"
            H2Style.DOT_PREFIX ->
                "font-size:${size}px;color:$pc;margin:22px 0 10px;font-weight:bold;line-height:1.4;"
            H2Style.UNDERLINE ->
                "font-size:${size}px;color:$tc;margin:22px 0 10px;font-weight:bold;padding-bottom:6px;border-bottom:1px dashed $pc;line-height:1.4;"
            H2Style.BACKGROUND_LIGHT ->
                "font-size:${size}px;color:$pc;margin:22px 0 10px;font-weight:bold;padding:6px 12px;background-color:${pc}1A;border-radius:6px;line-height:1.4;"
            H2Style.PLAIN_COLOR ->
                "font-size:${size}px;color:$pc;margin:22px 0 10px;font-weight:bold;line-height:1.4;"
        }
    }

    private fun h3InlineStyle(layout: LayoutSettings): String {
        val pc = layout.primaryColor
        val tc = layout.textColor
        val sc = layout.subtitleColor
        val size = layout.h3Size
        return when (layout.h3Style) {
            H3Style.THIN_LEFT_BORDER ->
                "font-size:${size}px;color:$tc;margin:18px 0 8px;font-weight:bold;padding-left:6px;border-left:3px solid $sc;line-height:1.4;"
            H3Style.ARROW_PREFIX ->
                "font-size:${size}px;color:$tc;margin:18px 0 8px;font-weight:bold;line-height:1.4;"
            H3Style.BOLD_SUBTITLE ->
                "font-size:${size}px;color:$tc;margin:18px 0 8px;font-weight:bold;line-height:1.4;"
            H3Style.ITALIC_COLOR ->
                "font-size:${size}px;color:$pc;margin:18px 0 8px;font-weight:bold;font-style:italic;line-height:1.4;"
            H3Style.CIRCLE_BULLET ->
                "font-size:${size}px;color:$tc;margin:18px 0 8px;font-weight:bold;line-height:1.4;"
        }
    }

    fun convertMarkdownToHtml(markdown: String): String {
        var html = escapeHtml(markdown)
        // Blockquote lines start with `>`; escapeHtml turns `>` into `&gt;`, which must be undone here
        // so the blockquote regex below can match (preview/export were showing literal `>` / wrong styling).
        html = html.replace(Regex("(?m)^&gt;"), ">")

        // Code blocks (before inline code)
        html = html.replace(Regex("```(\\w*)\\n([\\s\\S]*?)```")) { match ->
            val lang = match.groupValues[1]
            val code = match.groupValues[2].trimEnd()
            if (lang.isNotEmpty()) {
                "<pre><code class=\"language-$lang\">$code</code></pre>"
            } else {
                "<pre><code>$code</code></pre>"
            }
        }

        // Headings
        html = html.replace(Regex("^#{6}\\s+(.+)$", RegexOption.MULTILINE), "<h6>$1</h6>")
        html = html.replace(Regex("^#{5}\\s+(.+)$", RegexOption.MULTILINE), "<h5>$1</h5>")
        html = html.replace(Regex("^#{4}\\s+(.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
        html = html.replace(Regex("^###\\s+(.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
        html = html.replace(Regex("^##\\s+(.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
        html = html.replace(Regex("^#\\s+(.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")

        // Bold and italic
        html = html.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "<strong><em>$1</em></strong>")
        html = html.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        html = html.replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
        html = html.replace(Regex("__(.+?)__"), "<strong>$1</strong>")
        html = html.replace(Regex("_(.+?)_"), "<em>$1</em>")

        // Strikethrough
        html = html.replace(Regex("~~(.+?)~~"), "<del>$1</del>")

        // Inline code
        html = html.replace(Regex("`([^`]+)`"), "<code>$1</code>")

        // Links
        html = html.replace(Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")) { match ->
            val alt = match.groupValues[1]
            val src = match.groupValues[2]
            "<img src=\"$src\" alt=\"$alt\" />"
        }
        html = html.replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match ->
            val text = match.groupValues[1]
            val href = match.groupValues[2]
            "<a href=\"$href\">$text</a>"
        }

        // Horizontal rule
        html = html.replace(Regex("^(---|-{3,}|\\*{3,}|_{3,})$", RegexOption.MULTILINE), "<hr>")

        // Blockquote
        html = html.replace(Regex("^>\\s*(.+)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")

        // Unordered lists
        html = html.replace(Regex("((?:^[*\\-+]\\s.+\\n?)+)", RegexOption.MULTILINE)) { match ->
            val items = match.value.trim().split("\n").joinToString("") { line ->
                "<li>${line.trimStart('*', '-', '+').trim()}</li>"
            }
            "<ul>$items</ul>"
        }

        // Ordered lists
        html = html.replace(Regex("((?:^\\d+\\.\\s.+\\n?)+)", RegexOption.MULTILINE)) { match ->
            val items = match.value.trim().split("\n").joinToString("") { line ->
                "<li>${line.replace(Regex("^\\d+\\.\\s"), "")}</li>"
            }
            "<ol>$items</ol>"
        }

        // Paragraphs (wrap non-tagged lines)
        val lines = html.split("\n")
        val result = StringBuilder()
        var inBlock = false
        val blockTags = setOf("h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "blockquote", "pre", "hr", "img", "p")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (!inBlock) result.append("")
                continue
            }
            val isBlockElement = blockTags.any { tag ->
                trimmed.startsWith("<$tag") || trimmed.startsWith("</$tag")
            }
            if (isBlockElement) {
                result.append(trimmed).append("\n")
            } else {
                result.append("<p>").append(trimmed).append("</p>\n")
            }
        }

        return result.toString()
    }

    private fun escapeHtml(text: String): String {
        // Preserve existing HTML tags while escaping bare special chars
        // Only escape & that's not part of an entity, and < > that are not part of known HTML tags
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            // Restore HTML tags we want to pass through
            .replace(Regex("&lt;(/?(?:u|b|i|span|p|div|br|img|a|strong|em|del|code|pre|h[1-6]|ul|ol|li|blockquote|hr|table|tr|th|td)[^>]*)&gt;")) {
                "<${it.groupValues[1]}>"
            }
    }
}
