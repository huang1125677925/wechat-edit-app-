package com.wechat.editor.data

import android.content.Context
import com.wechat.editor.model.Article
import com.wechat.editor.model.ArticleTemplate
import com.wechat.editor.model.CodeStyle
import com.wechat.editor.model.H1Style
import com.wechat.editor.model.H2Style
import com.wechat.editor.model.H3Style
import com.wechat.editor.model.LayoutSettings
import com.wechat.editor.model.QuoteStyle
import org.json.JSONObject

/**
 * Persists in-progress article drafts so edits survive process death or accidental exit
 * before the user taps save.
 */
class ArticleDraftStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readDraft(sessionKey: String): Article? {
        val raw = prefs.getString(keyFor(sessionKey), null) ?: return null
        return runCatching { articleFromJson(JSONObject(raw)) }.getOrNull()
    }

    fun writeDraft(sessionKey: String, article: Article) {
        prefs.edit().putString(keyFor(sessionKey), articleToJson(article).toString()).apply()
    }

    fun clearDraft(sessionKey: String) {
        prefs.edit().remove(keyFor(sessionKey)).apply()
    }

    private fun keyFor(sessionKey: String) = "draft_$sessionKey"

    companion object {
        private const val PREFS_NAME = "wechat_editor_drafts"
    }
}

private fun articleToJson(a: Article): JSONObject = JSONObject().apply {
    put("id", a.id)
    put("title", a.title)
    put("content", a.content)
    put("htmlContent", a.htmlContent)
    put("coverImageUrl", a.coverImageUrl)
    put("digest", a.digest)
    put("author", a.author)
    put("template", a.template.name)
    put("layoutSettings", layoutToJson(a.layoutSettings))
    put("createdAt", a.createdAt)
    put("updatedAt", a.updatedAt)
}

private fun articleFromJson(o: JSONObject): Article = Article(
    id = o.optString("id"),
    title = o.optString("title"),
    content = o.optString("content"),
    htmlContent = o.optString("htmlContent"),
    coverImageUrl = o.optString("coverImageUrl"),
    digest = o.optString("digest"),
    author = o.optString("author"),
    template = o.optString("template").toArticleTemplate(),
    layoutSettings = o.optJSONObject("layoutSettings")?.let(::layoutFromJson) ?: LayoutSettings(),
    createdAt = o.optLong("createdAt"),
    updatedAt = o.optLong("updatedAt")
)

private fun layoutToJson(l: LayoutSettings): JSONObject = JSONObject().apply {
    put("fontFamily", l.fontFamily)
    put("baseFontSize", l.baseFontSize)
    put("lineHeight", l.lineHeight.toDouble())
    put("paragraphSpacing", l.paragraphSpacing)
    put("firstLineIndent", l.firstLineIndent)
    put("contentMaxWidth", l.contentMaxWidth)
    put("primaryColor", l.primaryColor)
    put("textColor", l.textColor)
    put("subtitleColor", l.subtitleColor)
    put("backgroundColor", l.backgroundColor)
    put("h1Size", l.h1Size)
    put("h2Size", l.h2Size)
    put("h3Size", l.h3Size)
    put("h1Style", l.h1Style.name)
    put("h2Style", l.h2Style.name)
    put("h3Style", l.h3Style.name)
    put("quoteStyle", l.quoteStyle.name)
    put("codeStyle", l.codeStyle.name)
    put("extraPreviewCss", l.extraPreviewCss)
}

private fun layoutFromJson(o: JSONObject): LayoutSettings = LayoutSettings(
    fontFamily = o.optString("fontFamily", "system"),
    baseFontSize = o.optInt("baseFontSize", 16),
    lineHeight = o.optDouble("lineHeight", 1.75).toFloat(),
    paragraphSpacing = o.optInt("paragraphSpacing", 12),
    firstLineIndent = o.optBoolean("firstLineIndent", false),
    contentMaxWidth = o.optInt("contentMaxWidth", 677),
    primaryColor = o.optString("primaryColor", "#1AAD19"),
    textColor = o.optString("textColor", "#333333"),
    subtitleColor = o.optString("subtitleColor", "#666666"),
    backgroundColor = o.optString("backgroundColor", "#FFFFFF"),
    h1Size = o.optInt("h1Size", 22),
    h2Size = o.optInt("h2Size", 20),
    h3Size = o.optInt("h3Size", 18),
    h1Style = o.optString("h1Style").toH1Style(),
    h2Style = o.optString("h2Style").toH2Style(),
    h3Style = o.optString("h3Style").toH3Style(),
    quoteStyle = o.optString("quoteStyle").toQuoteStyle(),
    codeStyle = o.optString("codeStyle").toCodeStyle(),
    extraPreviewCss = o.optString("extraPreviewCss", "")
)

private fun String.toArticleTemplate(): ArticleTemplate =
    runCatching { ArticleTemplate.valueOf(this) }.getOrDefault(ArticleTemplate.DEFAULT)

private fun String.toH1Style(): H1Style =
    runCatching { H1Style.valueOf(this) }.getOrDefault(H1Style.UNDERLINE_BORDER)

private fun String.toH2Style(): H2Style =
    runCatching { H2Style.valueOf(this) }.getOrDefault(H2Style.LEFT_BORDER)

private fun String.toH3Style(): H3Style =
    runCatching { H3Style.valueOf(this) }.getOrDefault(H3Style.THIN_LEFT_BORDER)

private fun String.toQuoteStyle(): QuoteStyle =
    runCatching { QuoteStyle.valueOf(this) }.getOrDefault(QuoteStyle.LEFT_BORDER)

private fun String.toCodeStyle(): CodeStyle =
    runCatching { CodeStyle.valueOf(this) }.getOrDefault(CodeStyle.DARK)
