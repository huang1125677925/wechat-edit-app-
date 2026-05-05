package com.wechat.editor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wechat.editor.data.UserSettingsStore
import com.wechat.editor.model.AiNewsFeed
import com.wechat.editor.model.AiNewsItem
import com.wechat.editor.model.Article
import com.wechat.editor.model.ArticleTemplate
import com.wechat.editor.model.TemplateLayoutProvider
import com.wechat.editor.utils.AiNewsAggregatorApi
import com.wechat.editor.utils.DeepSeekApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiNewsDigestViewModel(application: Application) : AndroidViewModel(application) {

    private val userSettingsStore = UserSettingsStore(application)

    private val _ui = MutableStateFlow(DigestUiState())
    val ui: StateFlow<DigestUiState> = _ui.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private var loadedFeed: AiNewsFeed? = null

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun setWindow24h() {
        _ui.update { it.copy(windowHours = 24) }
    }

    fun setWindow7d() {
        _ui.update { it.copy(windowHours = 168) }
    }

    fun setMaxForModel(n: Int) {
        _ui.update { it.copy(maxItemsForModel = n.coerceIn(20, 120)) }
    }

    fun loadFeed() {
        val hours = _ui.value.windowHours
        _ui.update { it.copy(isLoadingFeed = true, feedError = null) }
        viewModelScope.launch {
            when (val r = AiNewsAggregatorApi.fetchLatest(hours)) {
                is AiNewsAggregatorApi.Result.Error -> {
                    _ui.update {
                        it.copy(
                            isLoadingFeed = false,
                            feedError = r.message,
                            feedMeta = null,
                            items = emptyList()
                        )
                    }
                }
                is AiNewsAggregatorApi.Result.Success -> {
                    loadedFeed = r.feed
                    _ui.update {
                        it.copy(
                            isLoadingFeed = false,
                            feedError = null,
                            feedMeta = FeedMeta(
                                generatedAt = r.feed.generatedAt,
                                windowHours = r.feed.windowHours,
                                totalItems = r.feed.totalItems
                            ),
                            items = r.feed.items
                        )
                    }
                }
            }
        }
    }

    /**
     * Generates digest markdown via DeepSeek, then [onReady] can navigate to the editor.
     */
    fun generateDigest(onReady: (Article) -> Unit) {
        if (_ui.value.isGenerating) return
        val feed = loadedFeed
        if (feed == null || _ui.value.items.isEmpty()) {
            _snackbar.value = "请先拉取资讯数据"
            return
        }
        val apiKey = userSettingsStore.getDeepSeekApiKey()
        if (apiKey.isBlank()) {
            _snackbar.value = "请先在设置中填写 DeepSeek API Key"
            return
        }

        val max = _ui.value.maxItemsForModel
        val lines = _ui.value.items
            .asSequence()
            .filter { it.url.isNotBlank() }
            .take(max)
            .mapIndexed { index, item -> formatItemLine(index + 1, item) }
            .joinToString("\n")

        if (lines.isBlank()) {
            _snackbar.value = "没有带有效链接的条目"
            return
        }

        _ui.update { it.copy(isGenerating = true, generateError = null) }
        viewModelScope.launch {
            val digestTitle = buildDigestTitle(feed.windowHours)
            val digestAuthor = "AI 资讯摘要 · DeepSeek"
            val attributionBlock = buildAttributionMarkdown(feed, lines.lines().size)

            when (
                val result = DeepSeekApi.writeAiNewsDigestMarkdown(
                    apiKey = apiKey,
                    feedGeneratedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    itemCount = feed.totalItems,
                    inputLines = lines
                )
            ) {
                is DeepSeekApi.Result.Error -> {
                    _ui.update { it.copy(isGenerating = false, generateError = result.message) }
                    _snackbar.value = result.message
                }
                is DeepSeekApi.Result.Success -> {
                    val fullMarkdown = buildString {
                        append(result.markdown.trim())
                        append("\n\n---\n\n")
                        append(attributionBlock)
                    }
                    val article = Article(
                        title = digestTitle,
                        content = fullMarkdown,
                        author = digestAuthor,
                        template = ArticleTemplate.TECH,
                        layoutSettings = TemplateLayoutProvider.layoutForTemplate(ArticleTemplate.TECH)
                    )
                    _ui.update { it.copy(isGenerating = false) }
                    onReady(article)
                }
            }
        }
    }

    private fun formatItemLine(index: Int, item: AiNewsItem): String {
        val title = item.displayTitle()
        val site = listOf(item.siteName, item.source)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" · ")
        val time = item.publishedAt?.let { " · $it" }.orEmpty()
        return "$index. $title | $site$time | ${item.url}"
    }

    private fun buildDigestTitle(windowHours: Int): String {
        val label = if (windowHours >= 168) "近 7 天" else "24 小时"
        val now = SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date())
        return "AI 科技动态 · $label 要点（$now）"
    }

    private fun buildAttributionMarkdown(
        feed: AiNewsFeed,
        lineCount: Int
    ): String {
        val sourceName = "SuYxh / ai-news-aggregator"
        val dataUrl = when (feed.windowHours) {
            24, 168 -> "${AiNewsAggregatorApi.BASE_JSON_URL}/latest-${if (feed.windowHours >= 168) "7d" else "24h"}.json"
            else -> AiNewsAggregatorApi.BASE_JSON_URL
        }
        return buildString {
            append("**数据来源**：开源聚合 [$sourceName](https://github.com/SuYxh/ai-news-aggregator) ")
            append("（[在线站点](https://suyxh.github.io/ai-news-aggregator/) · ")
            append("[JSON]($dataUrl)）。聚合更新时间：`${feed.generatedAt}`；本期正文生成依据约 **$lineCount** 条标题链接。\n\n")
            append("**说明**：正文由 DeepSeek 根据标题归纳撰写，细节请以原文链接为准。")
        }
    }

    data class DigestUiState(
        val windowHours: Int = 24,
        val maxItemsForModel: Int = 80,
        val isLoadingFeed: Boolean = false,
        val feedError: String? = null,
        val feedMeta: FeedMeta? = null,
        val items: List<AiNewsItem> = emptyList(),
        val isGenerating: Boolean = false,
        val generateError: String? = null
    )

    data class FeedMeta(
        val generatedAt: String,
        val windowHours: Int,
        val totalItems: Int
    )
}
