package com.wechat.editor.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtils {
    /**
     * Copy HTML content to the clipboard using the text/html MIME type.
     * HTML-aware editors (e.g. the WeChat Official Account web editor) will receive the
     * content as rendered HTML rather than raw source text.
     * A plain-text fallback (the raw HTML string) is provided for apps that only support
     * text/plain.
     */
    fun copyHtmlToClipboard(context: Context, html: String, label: String = "HTML Content") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newHtmlText(label, html, html)
        clipboard.setPrimaryClip(clip)
    }

    fun copyToClipboard(context: Context, text: String, label: String = "HTML Content") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
