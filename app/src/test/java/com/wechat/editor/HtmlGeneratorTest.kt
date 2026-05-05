package com.wechat.editor

import com.wechat.editor.model.LayoutSettings
import com.wechat.editor.utils.HtmlGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlGeneratorTest {

    private val defaultLayout = LayoutSettings()

    @Test
    fun `generateHtml includes title`() {
        val html = HtmlGenerator.generateHtml("content", defaultLayout, "Test Title", "Author")
        assertTrue("HTML should include title", html.contains("Test Title"))
    }

    @Test
    fun `generateHtml includes author`() {
        val html = HtmlGenerator.generateHtml("content", defaultLayout, "Title", "Test Author")
        assertTrue("HTML should include author", html.contains("Test Author"))
    }

    @Test
    fun `convertMarkdownToHtml handles bold`() {
        val html = HtmlGenerator.convertMarkdownToHtml("**bold text**")
        assertTrue("Should convert bold markdown", html.contains("<strong>bold text</strong>"))
    }

    @Test
    fun `convertMarkdownToHtml handles italic`() {
        val html = HtmlGenerator.convertMarkdownToHtml("*italic text*")
        assertTrue("Should convert italic markdown", html.contains("<em>italic text</em>"))
    }

    @Test
    fun `convertMarkdownToHtml handles headings`() {
        val html = HtmlGenerator.convertMarkdownToHtml("# Heading 1")
        assertTrue("Should convert h1", html.contains("<h1>Heading 1</h1>"))
    }

    @Test
    fun `convertMarkdownToHtml handles blockquote after escapeHtml restores line-start gt`() {
        val html = HtmlGenerator.convertMarkdownToHtml("> 引用一行")
        assertTrue("Should convert blockquote", html.contains("<blockquote>"))
        assertTrue("Should not leave raw markdown marker in output", !html.contains("&gt; 引用"))
    }

    @Test
    fun `convertMarkdownToHtml handles strikethrough`() {
        val html = HtmlGenerator.convertMarkdownToHtml("~~strikethrough~~")
        assertTrue("Should convert strikethrough", html.contains("<del>strikethrough</del>"))
    }

    @Test
    fun `generateCss includes font size`() {
        val css = HtmlGenerator.generateCss(defaultLayout)
        assertTrue("CSS should include font size", css.contains("${defaultLayout.baseFontSize}px"))
    }

    @Test
    fun `generateCss appends extraPreviewCss`() {
        val layout = defaultLayout.copy(extraPreviewCss = ".article-content p { color: red; }")
        val css = HtmlGenerator.generateCss(layout)
        assertTrue(css.contains(".article-content p { color: red; }"))
    }

    @Test
    fun `generateWeChatPasteHtml is fragment with inline styles only`() {
        val html = HtmlGenerator.generateWeChatPasteHtml(
            "正文一段",
            defaultLayout,
            "测试标题",
            "作者甲"
        )
        assertTrue("No full document", !html.contains("<!DOCTYPE"))
        assertTrue("No style block", !html.contains("<style>"))
        assertTrue("Uses section wrapper", html.contains("<section style="))
        assertTrue("Title escaped in inline h1", html.contains("测试标题"))
        assertTrue("Author in fragment", html.contains("作者甲"))
        assertTrue("Paragraph has inline style", html.contains("<p style="))
    }

    @Test
    fun `applyWeChatInlineStyles handles fenced code with language class`() {
        val body = HtmlGenerator.convertMarkdownToHtml("```kotlin\nval x = 1\n```")
        val styled = HtmlGenerator.applyWeChatInlineStyles(body, defaultLayout)
        assertTrue("pre has inline background", styled.contains("<pre style="))
        assertTrue("code block keeps class attribute", styled.contains("language-kotlin"))
    }
}
