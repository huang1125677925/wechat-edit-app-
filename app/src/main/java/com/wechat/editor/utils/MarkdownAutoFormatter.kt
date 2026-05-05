package com.wechat.editor.utils

/**
 * Local, deterministic cleanup for AI-generated Markdown: list markers,
 * emphasis markers, obvious broken link fragments, and removal of blank lines
 * outside fenced code blocks — without calling APIs.
 * Lines inside ``` fenced blocks are passed through unchanged (including inner blank lines).
 */
object MarkdownAutoFormatter {

    fun format(markdown: String): String {
        if (markdown.isEmpty()) return markdown
        val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')
        val out = mutableListOf<String>()
        var inFence = false

        fun isFenceDelimiter(line: String): Boolean {
            val t = line.trimStart()
            if (!t.startsWith("```")) return false
            val after = t.drop(3).trim()
            return after.isEmpty() || after.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (isFenceDelimiter(line)) {
                inFence = !inFence
                out.add(line.trimEnd())
                i++
                continue
            }
            if (inFence) {
                out.add(line)
                i++
                continue
            }

            if (i < lines.size - 1) {
                val merged = tryMergeSplitLink(lines[i], lines[i + 1])
                if (merged != null) {
                    val normalizedLine = normalizeListLine(merged)
                    stripOrphanLinkTail(normalizedLine)?.let { out.add(it) }
                    i += 2
                    continue
                }
            }

            val normalizedLine = normalizeListLine(line)
            stripOrphanLinkTail(normalizedLine)?.let { candidate ->
                if (candidate.isBlank()) return@let
                out.add(candidate)
            }
            i++
        }

        var text = out.joinToString("\n")
        text = balanceBoldAcrossDocumentString(text)
        return finalizeWhitespace(text)
    }

    /** Merge `[text](` on one line with `https://...` on the next. */
    private fun tryMergeSplitLink(current: String, next: String): String? {
        val trimmed = current.trimEnd()
        val openIdx = trimmed.lastIndexOf("](")
        if (openIdx < 0 || trimmed.endsWith(')')) return null
        val afterBracket = trimmed.substring(openIdx + 2).trimStart()
        if (afterBracket.isNotEmpty()) return null
        val n = next.trim()
        if (!n.startsWith("http://") && !n.startsWith("https://")) return null
        val ws = n.indexOfFirst { it.isWhitespace() }
        var urlPart = if (ws == -1) n else n.substring(0, ws)
        val suffix = if (ws == -1) "" else n.substring(ws)
        // Closing `)` of the markdown link is often on the URL line: `https://x.com/path)`
        if (urlPart.endsWith(')')) urlPart = urlPart.dropLast(1)
        return trimmed + urlPart + ")" + suffix
    }

    /** `•` / `·` / `*` bullets → `- ` */
    private fun normalizeListLine(line: String): String {
        val t = line.trimStart()
        val indent = line.take(line.length - t.length)
        when {
            t.startsWith("• ") -> return indent + "- " + t.drop(2)
            t.startsWith("•") && t.length > 1 && t[1] != ' ' ->
                return indent + "- " + t.drop(1).trimStart()
            t.startsWith("· ") -> return indent + "- " + t.drop(2)
            t.startsWith("* ") && !t.startsWith("**") -> return indent + "- " + t.drop(2)
        }
        return line.trimEnd()
    }

    /**
     * Drops lines that look like a lone closing fragment of a markdown link
     * (e.g. `-regulation-of-new-ai-models)` without `[text](`).
     */
    private fun stripOrphanLinkTail(line: String): String? {
        val t = line.trim()
        if (t.isEmpty()) return line
        if (t.contains('[') && t.contains("](")) return line
        if (Regex("^[\\-\\s]*[a-zA-Z][a-zA-Z0-9\\-]{3,}\\)\\s*$").matches(t)) return null
        if (Regex("^\\s*\\)\\s*https?://\\S+\\s*$").matches(t)) return null
        return line
    }

    private fun balanceBoldAcrossDocumentString(text: String): String {
        val lines = text.split('\n').toMutableList()
        var doubleStarCount = 0
        for (line in lines) {
            doubleStarCount += countDoubleStarsOutsideTicks(line)
        }
        if (doubleStarCount % 2 == 0) return text
        val idx = lines.indexOfLast { it.isNotBlank() }
        if (idx < 0) return text
        val last = lines[idx]
        lines[idx] = if (last.endsWith("**")) last else last.trimEnd() + "**"
        return lines.joinToString("\n")
    }

    private fun countDoubleStarsOutsideTicks(line: String): Int {
        val noCode = stripInlineCodeSegments(line)
        return noCode.split("**").size - 1
    }

    private fun stripInlineCodeSegments(line: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            if (line[i] == '`') {
                val end = line.indexOf('`', i + 1)
                if (end == -1) {
                    sb.append(line[i])
                    i++
                } else {
                    i = end + 1
                }
            } else {
                sb.append(line[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun finalizeWhitespace(s: String): String {
        val t = s.replace(Regex("\n{3,}"), "\n\n")
        return t.trimEnd()
    }
}
