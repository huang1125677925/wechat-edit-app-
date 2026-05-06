package com.wechat.editor.utils

/**
 * Deterministic cleanup for AI-news digests before opening them in the editor.
 *
 * The model sometimes emits readable bullet glyphs (`•`) and then puts source links
 * on bare lines under the bullet. Those lines look acceptable as text, but they are
 * not valid Markdown list items and render as loose paragraphs in preview / WeChat.
 */
object AiNewsDigestMarkdownSanitizer {

    private val listItemRegex = Regex("^\\s*(?:[-*+]\\s+|\\d+\\.\\s+).+")
    private val markdownLinkRegex = Regex("!?\\[[^\\]]+\\]\\([^)]+\\)")
    private val bareTitleLinkRegex = Regex("^(.+?)\\((https?://[^\\s)]+)\\)$")
    private val standaloneLinkPrefixes = listOf(
        "参考：",
        "参考:",
        "链接：",
        "链接:",
        "来源：",
        "来源:",
        "相关阅读：",
        "相关阅读:",
        "延伸阅读：",
        "延伸阅读:"
    )

    fun normalize(markdown: String): String {
        if (markdown.isEmpty()) return markdown

        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = mutableListOf<String>()
        var inFence = false

        for (rawLine in lines) {
            if (isFenceDelimiter(rawLine)) {
                inFence = !inFence
                out.add(rawLine.trimEnd())
                continue
            }

            if (inFence) {
                out.add(rawLine)
                continue
            }

            val line = normalizeBulletMarker(rawLine)
            val linkPayload = standaloneLinkPayload(line.trim())
            if (linkPayload != null && appendToPreviousListItem(out, linkPayload)) {
                continue
            }

            out.add(line.trimEnd())
        }

        return out.joinToString("\n").trimEnd()
    }

    private fun isFenceDelimiter(line: String): Boolean {
        val t = line.trimStart()
        if (!t.startsWith("```")) return false
        val after = t.drop(3).trim()
        return after.isEmpty() || after.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    private fun normalizeBulletMarker(line: String): String {
        val t = line.trimStart()
        val indent = line.take(line.length - t.length)
        return when {
            t.startsWith("• ") -> indent + "- " + t.drop(2)
            t.startsWith("•") && t.length > 1 -> indent + "- " + t.drop(1).trimStart()
            t.startsWith("· ") -> indent + "- " + t.drop(2)
            t.startsWith("·") && t.length > 1 -> indent + "- " + t.drop(1).trimStart()
            t.startsWith("* ") && !t.startsWith("**") -> indent + "- " + t.drop(2)
            else -> line
        }
    }

    private fun standaloneLinkPayload(trimmedLine: String): String? {
        if (trimmedLine.isEmpty()) return null
        if (trimmedLine.startsWith("#") || trimmedLine.startsWith(">") || trimmedLine.startsWith("<")) {
            return null
        }
        if (listItemRegex.matches(trimmedLine)) return null

        var payload = trimmedLine
        for (prefix in standaloneLinkPrefixes) {
            if (payload.startsWith(prefix)) {
                payload = payload.removePrefix(prefix).trim()
                break
            }
        }

        val markdownLinks = markdownLinkRegex.findAll(payload).toList()
        if (markdownLinks.isEmpty()) {
            bareTitleLinkRegex.matchEntire(payload)?.let { match ->
                val label = match.groupValues[1].trim()
                val url = match.groupValues[2].trim()
                if (label.isNotEmpty() && '[' !in label && ']' !in label) {
                    return "[$label]($url)"
                }
            }
            return null
        }

        val remainder = markdownLinkRegex.replace(payload, "").trim()
        val onlySeparators = remainder.isEmpty() || remainder.all {
            it.isWhitespace() || it == '、' || it == '，' || it == ',' || it == '；' || it == ';' || it == '。'
        }
        return if (onlySeparators) payload.trim() else null
    }

    private fun appendToPreviousListItem(out: MutableList<String>, linkPayload: String): Boolean {
        var previousIndex = out.size - 1
        while (previousIndex >= 0 && out[previousIndex].isBlank()) {
            previousIndex--
        }
        if (previousIndex < 0 || !listItemRegex.matches(out[previousIndex])) {
            return false
        }

        while (out.size > previousIndex + 1) {
            out.removeAt(out.lastIndex)
        }

        val separator = if (out[previousIndex].contains("参考：")) "、" else " 参考："
        out[previousIndex] = out[previousIndex].trimEnd() + separator + linkPayload.trim()
        return true
    }
}
