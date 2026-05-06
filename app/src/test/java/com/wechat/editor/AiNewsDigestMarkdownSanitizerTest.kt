package com.wechat.editor

import com.wechat.editor.utils.AiNewsDigestMarkdownSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiNewsDigestMarkdownSanitizerTest {

    @Test
    fun `normalizes bullet glyphs to markdown list markers`() {
        val input = "• GPT-5.5 Instant 全面开放：[标题](https://example.com/a)"

        val out = AiNewsDigestMarkdownSanitizer.normalize(input)

        assertEquals("- GPT-5.5 Instant 全面开放：[标题](https://example.com/a)", out)
    }

    @Test
    fun `folds bare reference links into previous list item`() {
        val input = """
            • GPT-5.5 Instant 全面开放：OpenAI 发布新版模型。
            GPT-5.5 Instant：更可靠、更智能、向全体用户开放(https://example.com/a)
            5月5日5点55分，GPT-5.5自己选择人开派对！Codex反超ClaudeCode(https://example.com/b)
        """.trimIndent()

        val out = AiNewsDigestMarkdownSanitizer.normalize(input)

        assertEquals(
            "- GPT-5.5 Instant 全面开放：OpenAI 发布新版模型。 参考：" +
                "[GPT-5.5 Instant：更可靠、更智能、向全体用户开放](https://example.com/a)、" +
                "[5月5日5点55分，GPT-5.5自己选择人开派对！Codex反超ClaudeCode](https://example.com/b)",
            out
        )
        assertFalse(out.lines().drop(1).any { it.isNotBlank() && !it.startsWith("- ") })
    }

    @Test
    fun `keeps fenced code untouched`() {
        val input = "```\n• not markdown\nBare link(https://example.com)\n```"

        assertEquals(input, AiNewsDigestMarkdownSanitizer.normalize(input))
    }
}
