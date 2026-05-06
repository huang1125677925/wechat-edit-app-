package com.wechat.editor

import com.wechat.editor.model.ArticleTemplate
import com.wechat.editor.model.LayoutSettings
import com.wechat.editor.model.PreviewStylePresets
import com.wechat.editor.utils.HtmlGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlGeneratorTest {

    private val defaultLayout = LayoutSettings()

    @Test
    fun `generateWeChatPasteHtml includes pasteStrongBackgroundColor on strong`() {
        val layout = defaultLayout.copy(
            pasteStrongColor = "#1a1a1a",
            pasteStrongBackgroundColor = "#fff3cd"
        )
        val html = HtmlGenerator.generateWeChatPasteHtml("**小标题**：正文", layout, "", "")
        assertTrue(html.contains("background-color:#fff3cd"))
        assertTrue(html.contains("color:#1a1a1a"))
    }

    @Test
    fun `generateWeChatPasteHtml uses pasteLinkColor for links when set`() {
        val layout = defaultLayout.copy(pasteLinkColor = "#576B95")
        val html = HtmlGenerator.generateWeChatPasteHtml("[点击](https://example.com)", layout, "", "")
        assertTrue(html.contains("color:#576B95"))
    }

    @Test
    fun `WECHAT_CYBER_ZEN preset defines paste colors for WeChat-style editorial`() {
        val layout = PreviewStylePresets.layoutForHuashengTemplate(ArticleTemplate.WECHAT_CYBER_ZEN)
        assertTrue(layout != null)
        assertEquals("#576B95", layout!!.pasteLinkColor)
        assertEquals("#C0504D", layout.pasteTitleColor)
    }

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
    fun `convertMarkdownToHtml does not treat wechat url underscores as emphasis`() {
        val markdown = "[Claude Code成本爆降](https://mp.weixin.qq.com/s?__biz=MzAxOTcxNTIwNQ%3D%3D&mid=2457993794)"
        val html = HtmlGenerator.convertMarkdownToHtml(markdown)

        assertTrue(html.contains("""<a href="https://mp.weixin.qq.com/s?__biz=MzAxOTcxNTIwNQ%3D%3D&amp;mid=2457993794">Claude Code成本爆降</a>"""))
        assertTrue("URL must not be split by strong tags", !html.contains("<strong>biz="))
        assertTrue("href must not contain nested markup", !html.contains("""href="https://mp.weixin.qq.com/s?<strong>"""))
    }

    @Test
    fun `generateWeChatPasteHtml keeps multiple wechat links legal on one list item`() {
        val markdown = "- 参考：[Claude Code成本爆降](https://mp.weixin.qq.com/s?__biz=MzAxOTcxNTIwNQ%3D%3D&mid=2457993794)、[DeepSeek版Claude Code](https://mp.weixin.qq.com/s?__biz=MzIzNjc1NzUzMw%3D%3D&mid=2247888322)"
        val html = HtmlGenerator.generateWeChatPasteHtml(markdown, defaultLayout, "", "")

        assertTrue(html.contains("href=\"https://mp.weixin.qq.com/s?__biz=MzAxOTcxNTIwNQ%3D%3D&amp;mid=2457993794\""))
        assertTrue(html.contains("href=\"https://mp.weixin.qq.com/s?__biz=MzIzNjc1NzUzMw%3D%3D&amp;mid=2247888322\""))
        assertTrue("WeChat paste HTML must not put strong tags inside href", !html.contains("<a style=\"color:#2980B9;text-decoration:none;\" href=\"https://mp.weixin.qq.com/s?<strong"))
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
    fun `convertMarkdownToHtml merges soft line breaks into one paragraph`() {
        val md = "第一行\n第二行"
        val html = HtmlGenerator.convertMarkdownToHtml(md)
        assertEquals(
            "<p>第一行<br/>第二行</p>\n",
            html
        )
    }

    @Test
    fun `convertMarkdownToHtml starts new paragraph only after blank line`() {
        val md = "第一段\n\n第二段"
        val html = HtmlGenerator.convertMarkdownToHtml(md)
        assertEquals(
            "<p>第一段</p>\n<p>第二段</p>\n",
            html
        )
    }

    @Test
    fun `mergeSoftBreakParagraphs preserves multiline pre blocks`() {
        val inner = "line a\nline b"
        val html = HtmlGenerator.mergeSoftBreakParagraphs("<pre><code>$inner</code></pre>")
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("line a"))
        assertTrue(html.contains("line b"))
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

    @Test
    fun `applyWeChatInlineStyles uses capped spacing for paragraph margins on WeChat paste`() {
        val layout = defaultLayout.copy(paragraphSpacing = 24)
        val styled = HtmlGenerator.applyWeChatInlineStyles("<p>一段</p>", layout)
        assertTrue("paste HTML should not use full paragraphSpacing as margin-bottom", styled.contains("margin:0 0 8px"))
    }

    @Test
    fun `applyWeChatInlineStyles uses capped spacing for list margins on WeChat paste`() {
        val layout = defaultLayout.copy(paragraphSpacing = 24)
        val styled = HtmlGenerator.applyWeChatInlineStyles("<ul><li>a</li></ul>", layout)
        assertTrue(styled.contains("margin:4px 0 8px 24px"))
    }

    @Test
    fun `applyWeChatInlineStyles expands br inside p into separate p elements`() {
        // A single <p> with <br/> should become multiple <p> elements (no <br/> in output)
        val html = HtmlGenerator.convertMarkdownToHtml("第一行\n第二行\n第三行")
        val styled = HtmlGenerator.applyWeChatInlineStyles(html, defaultLayout)
        assertTrue("Should not contain <br/> after expansion", !styled.contains("<br/>"))
        assertTrue("Should contain multiple <p> tags", styled.split("<p ").size >= 4)
    }

    @Test
    fun `generateWeChatPasteHtml soft line breaks produce no br tags`() {
        // Lines joined by a single newline (soft break) must not appear as <br/> in WeChat paste HTML
        val html = HtmlGenerator.generateWeChatPasteHtml("行一\n行二\n行三", defaultLayout, "", "")
        assertTrue("WeChat paste HTML should not contain <br/> to avoid blank lines", !html.contains("<br/>"))
    }

    @Test
    fun `applyWeChatInlineStyles wraps li content in zero-margin p to prevent WeChat blank lines`() {
        val html = HtmlGenerator.convertMarkdownToHtml("- 项目一\n- 项目二\n- 项目三")
        val styled = HtmlGenerator.applyWeChatInlineStyles(html, defaultLayout)
        assertTrue("Each <li> should contain an inner <p> with zero margins", styled.contains("""<li style="margin:0 0 4px"""))
        assertTrue("Inner p should have margin:0", styled.contains("""<p style="margin:0;padding:0"""))
        assertTrue("Should still keep three list items", styled.split("</li>").size == 4)
    }

    @Test
    fun `applyWeChatInlineStyles paste list margin caps small gap`() {
        // Even with very large paragraphSpacing, the paste output for ul should cap at small gap
        val layout = defaultLayout.copy(paragraphSpacing = 32)
        val html = HtmlGenerator.convertMarkdownToHtml("- a\n- b")
        val styled = HtmlGenerator.applyWeChatInlineStyles(html, layout)
        assertTrue(styled.contains("margin:4px 0 8px 24px"))
    }

    @Test
    fun `applyWeChatInlineStyles preserves paragraph gap on last br-split line only`() {
        val layout = defaultLayout.copy(paragraphSpacing = 10)
        val input = """<p style="margin:0 0 8px;text-align:justify;">A<br/>B</p>"""
        val styled = HtmlGenerator.applyWeChatInlineStyles(input, layout)
        // First line should have margin:0
        assertTrue("First split line should have margin:0", styled.contains("""<p style="margin:0;"""))
        // Last line should keep block gap margin
        assertTrue("Last split line should have gap margin", styled.contains("""<p style="margin:0 0 8px;"""))
    }
}
