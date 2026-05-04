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
    fun `convertMarkdownToHtml handles strikethrough`() {
        val html = HtmlGenerator.convertMarkdownToHtml("~~strikethrough~~")
        assertTrue("Should convert strikethrough", html.contains("<del>strikethrough</del>"))
    }

    @Test
    fun `generateCss includes font size`() {
        val css = HtmlGenerator.generateCss(defaultLayout)
        assertTrue("CSS should include font size", css.contains("${defaultLayout.baseFontSize}px"))
    }
}
