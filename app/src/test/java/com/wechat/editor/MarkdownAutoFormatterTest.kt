package com.wechat.editor

import com.wechat.editor.utils.MarkdownAutoFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MarkdownAutoFormatterTest {

    @Test
    fun `bullet variants become hyphen list`() {
        val input = "• 第一项\n· 第二项\n* 第三项"
        val out = MarkdownAutoFormatter.format(input)
        assertEquals("- 第一项\n- 第二项\n- 第三项", out)
    }

    @Test
    fun `does not break bold asterisk line start`() {
        val input = "**bold** only"
        assertEquals(input, MarkdownAutoFormatter.format(input))
    }

    @Test
    fun `closes odd bold markers at document end`() {
        val input = "前言 **未闭合粗体"
        val out = MarkdownAutoFormatter.format(input)
        assertEquals("前言 **未闭合粗体**", out)
    }

    @Test
    fun `drops orphan link slug tail line`() {
        val input = "正文一行\n-regulation-of-new-ai-models)"
        val out = MarkdownAutoFormatter.format(input)
        assertFalse(out.contains("regulation"))
        assertEquals("正文一行", out)
    }

    @Test
    fun `merges split markdown link across lines`() {
        val input = "[Reuters headline](\nhttps://example.com/page)"
        val out = MarkdownAutoFormatter.format(input)
        assertEquals("[Reuters headline](https://example.com/page)", out)
    }

    @Test
    fun `preserves fenced code block content`() {
        val input = "```\n• not a list\n```"
        assertEquals(input, MarkdownAutoFormatter.format(input))
    }

    @Test
    fun `removes blank lines outside fences`() {
        val input = "第一段\n\n\n第二段"
        assertEquals("第一段\n第二段", MarkdownAutoFormatter.format(input))
    }

    @Test
    fun `preserves blank lines inside fenced code`() {
        val input = "```\nline1\n\nline2\n```"
        assertEquals(input, MarkdownAutoFormatter.format(input))
    }
}
