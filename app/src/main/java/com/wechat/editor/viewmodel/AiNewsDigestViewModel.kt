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
import com.wechat.editor.utils.AiNewsDigestMarkdownSanitizer
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
        setWindowHours(24)
    }

    fun setWindow7d() {
        setWindowHours(168)
    }

    fun setMaxForModel(n: Int) {
        _ui.update { it.copy(maxItemsForModel = n.coerceIn(20, 800)) }
    }

    private fun setWindowHours(hours: Int) {
        if (_ui.value.windowHours == hours) return
        loadedFeed = null
        _ui.update {
            it.copy(
                windowHours = hours,
                feedMeta = null,
                feedError = null,
                generateError = null,
                sourceOptions = emptyList(),
                selectedSourceKeys = emptySet(),
                items = emptyList()
            )
        }
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
                            sourceOptions = emptyList(),
                            selectedSourceKeys = emptySet(),
                            items = emptyList()
                        )
                    }
                }
                is AiNewsAggregatorApi.Result.Success -> {
                    loadedFeed = r.feed
                    val sourceOptions = buildSourceOptions(r.feed.items)
                    val selectedSourceKeys = sourceOptions.map { it.key }.toSet()
                    _ui.update {
                        it.copy(
                            isLoadingFeed = false,
                            feedError = null,
                            feedMeta = FeedMeta(
                                generatedAt = r.feed.generatedAt,
                                windowHours = r.feed.windowHours,
                                totalItems = r.feed.totalItems
                            ),
                            sourceOptions = sourceOptions,
                            selectedSourceKeys = selectedSourceKeys,
                            items = filterItemsBySources(r.feed.items, selectedSourceKeys)
                        )
                    }
                }
            }
        }
    }

    fun toggleSource(key: String) {
        val feed = loadedFeed ?: return
        _ui.update { current ->
            val selected = current.selectedSourceKeys.toMutableSet()
            if (!selected.add(key)) {
                selected.remove(key)
            }
            current.copy(
                selectedSourceKeys = selected,
                items = filterItemsBySources(feed.items, selected)
            )
        }
    }

    fun selectAllSources() {
        val feed = loadedFeed ?: return
        _ui.update { current ->
            val selected = current.sourceOptions.map { it.key }.toSet()
            current.copy(
                selectedSourceKeys = selected,
                items = filterItemsBySources(feed.items, selected)
            )
        }
    }

    fun clearSourceSelection() {
        val feed = loadedFeed ?: return
        _ui.update {
            it.copy(
                selectedSourceKeys = emptySet(),
                items = filterItemsBySources(feed.items, emptySet())
            )
        }
    }

    /**
     * Generates digest markdown via DeepSeek, then [onReady] can navigate to the editor.
     */
    fun generateDigest(onReady: (Article) -> Unit) {
        if (_ui.value.isGenerating) return
        val feed = loadedFeed
        if (feed == null) {
            _snackbar.value = "请先拉取资讯数据"
            return
        }
        if (_ui.value.items.isEmpty()) {
            _snackbar.value = "请至少选择一个包含资讯的来源"
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
            val selectedLineCount = lines.lines().size
            val attributionBlock = buildAttributionMarkdown(feed, selectedLineCount)

            when (
                val result = DeepSeekApi.writeAiNewsDigestMarkdown(
                    apiKey = apiKey,
                    feedGeneratedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    itemCount = selectedLineCount,
                    inputLines = lines
                )
            ) {
                is DeepSeekApi.Result.Error -> {
                    _ui.update { it.copy(isGenerating = false, generateError = result.message) }
                    _snackbar.value = result.message
                }
                is DeepSeekApi.Result.Success -> {
                    val normalizedDigest = AiNewsDigestMarkdownSanitizer.normalize(result.markdown.trim())
                    val fullMarkdown = buildString {
                        append(normalizedDigest)
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
        val site = sourceLabel(item)
        val time = item.publishedAt?.let { " · $it" }.orEmpty()
        return "$index. $title | $site$time | ${item.url}"
    }

    private fun buildSourceOptions(items: List<AiNewsItem>): List<SourceFilterOption> {
        return items
            .groupingBy { sourceKey(it) to sourceLabel(it) }
            .eachCount()
            .map { (source, count) ->
                SourceFilterOption(
                    key = source.first,
                    label = source.second,
                    count = count
                )
            }
            .sortedWith(compareByDescending<SourceFilterOption> { it.count }.thenBy { it.label })
    }

    private fun filterItemsBySources(items: List<AiNewsItem>, selectedSourceKeys: Set<String>): List<AiNewsItem> {
        if (selectedSourceKeys.isEmpty()) return emptyList()
        return items.filter { sourceKey(it) in selectedSourceKeys }
    }

    private fun sourceKey(item: AiNewsItem): String {
        return listOf(item.siteName, item.source)
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("|")
            .ifBlank { "unknown" }
    }

    private fun sourceLabel(item: AiNewsItem): String {
        return listOf(item.siteName, item.source)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" · ")
            .ifBlank { "未知来源" }
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
        val sourceOptions: List<SourceFilterOption> = emptyList(),
        val selectedSourceKeys: Set<String> = emptySet(),
        val items: List<AiNewsItem> = emptyList(),
        val isGenerating: Boolean = false,
        val generateError: String? = null
    )

    data class FeedMeta(
        val generatedAt: String,
        val windowHours: Int,
        val totalItems: Int
    )

    data class SourceFilterOption(
        val key: String,
        val label: String,
        val count: Int
    )
}
