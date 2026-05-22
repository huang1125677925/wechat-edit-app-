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
import com.wechat.editor.utils.PerpsNewsApi
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

    enum class FeedBackend {
        /** SuYxh / ai-news-aggregator GitHub Pages JSON. */
        AI_NEWS_AGGREGATOR,

        /** gunksd / Perps-news `data/news.json` on GitHub raw. */
        PERPS_NEWS
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun setBackendAggregator() {
        setBackend(FeedBackend.AI_NEWS_AGGREGATOR)
    }

    fun setBackendPerpsNews() {
        setBackend(FeedBackend.PERPS_NEWS)
    }

    private fun setBackend(backend: FeedBackend) {
        if (_ui.value.feedBackend == backend) return
        loadedFeed = null
        _ui.update {
            it.copy(
                feedBackend = backend,
                feedMeta = null,
                feedError = null,
                generateError = null,
                sourceOptions = emptyList(),
                selectedSourceKeys = emptySet(),
                items = emptyList()
            )
        }
    }

    fun setWindow24h() {
        setWindowHours(24)
    }

    fun setWindow7d() {
        setWindowHours(168)
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
        val backend = _ui.value.feedBackend
        _ui.update { it.copy(isLoadingFeed = true, feedError = null) }
        viewModelScope.launch {
            val result = when (backend) {
                FeedBackend.AI_NEWS_AGGREGATOR -> AiNewsAggregatorApi.fetchLatest(hours)
                FeedBackend.PERPS_NEWS -> PerpsNewsApi.fetchNews(hours)
            }
            when (backend) {
                FeedBackend.AI_NEWS_AGGREGATOR -> when (result) {
                    is AiNewsAggregatorApi.Result.Error -> applyFeedError(result.message)
                    is AiNewsAggregatorApi.Result.Success -> applyFeedSuccess(result.feed)
                }
                FeedBackend.PERPS_NEWS -> when (result) {
                    is PerpsNewsApi.Result.Error -> applyFeedError(result.message)
                    is PerpsNewsApi.Result.Success -> applyFeedSuccess(result.feed)
                }
            }
        }
    }

    private fun applyFeedError(message: String) {
        _ui.update {
            it.copy(
                isLoadingFeed = false,
                feedError = message,
                feedMeta = null,
                sourceOptions = emptyList(),
                selectedSourceKeys = emptySet(),
                items = emptyList()
            )
        }
    }

    private fun applyFeedSuccess(feed: AiNewsFeed) {
        loadedFeed = feed
        val sourceOptions = buildSourceOptions(feed.items)
        val selectedSourceKeys = sourceOptions.map { it.key }.toSet()
        _ui.update {
            it.copy(
                isLoadingFeed = false,
                feedError = null,
                feedMeta = FeedMeta(
                    generatedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    totalItems = feed.totalItems
                ),
                sourceOptions = sourceOptions,
                selectedSourceKeys = selectedSourceKeys,
                items = filterItemsBySources(feed.items, selectedSourceKeys)
            )
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

        val lines = _ui.value.items
            .asSequence()
            .filter { it.url.isNotBlank() }
            .mapIndexed { index, item -> formatItemLine(index + 1, item) }
            .joinToString("\n")

        if (lines.isBlank()) {
            _snackbar.value = "没有带有效链接的条目"
            return
        }

        _ui.update { it.copy(isGenerating = true, generateError = null) }
        viewModelScope.launch {
            val backend = _ui.value.feedBackend
            val digestTitle = buildDigestTitle(backend, feed.windowHours)
            val digestAuthor = when (backend) {
                FeedBackend.PERPS_NEWS -> "股市新闻摘要 · DeepSeek"
                FeedBackend.AI_NEWS_AGGREGATOR -> "AI 资讯摘要 · DeepSeek"
            }
            val selectedLineCount = lines.lines().size
            val attributionBlock = buildAttributionMarkdown(feed, selectedLineCount, backend)
            val template = when (backend) {
                FeedBackend.PERPS_NEWS -> ArticleTemplate.BUSINESS
                FeedBackend.AI_NEWS_AGGREGATOR -> ArticleTemplate.TECH
            }

            val deepSeekResult = when (backend) {
                FeedBackend.AI_NEWS_AGGREGATOR -> DeepSeekApi.writeAiNewsDigestMarkdown(
                    apiKey = apiKey,
                    feedGeneratedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    itemCount = selectedLineCount,
                    inputLines = lines
                )
                FeedBackend.PERPS_NEWS -> DeepSeekApi.writeMarketNewsDigestMarkdown(
                    apiKey = apiKey,
                    feedGeneratedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    itemCount = selectedLineCount,
                    inputLines = lines
                )
            }

            when (deepSeekResult) {
                is DeepSeekApi.Result.Error -> {
                    _ui.update { it.copy(isGenerating = false, generateError = deepSeekResult.message) }
                    _snackbar.value = deepSeekResult.message
                }
                is DeepSeekApi.Result.Success -> {
                    val normalizedDigest = AiNewsDigestMarkdownSanitizer.normalize(deepSeekResult.markdown.trim())
                    val fullMarkdown = buildString {
                        append(normalizedDigest)
                        append("\n\n---\n\n")
                        append(attributionBlock)
                    }
                    val article = Article(
                        title = digestTitle,
                        content = fullMarkdown,
                        author = digestAuthor,
                        template = template,
                        layoutSettings = TemplateLayoutProvider.layoutForTemplate(template)
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

    private fun buildDigestTitle(backend: FeedBackend, windowHours: Int): String {
        val label = if (windowHours >= 168) "近 7 天" else "24 小时"
        val now = SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date())
        return when (backend) {
            FeedBackend.AI_NEWS_AGGREGATOR -> "AI 科技动态 · $label 要点（$now）"
            FeedBackend.PERPS_NEWS -> "股市与财经要闻 · $label 要点（$now）"
        }
    }

    private fun buildAttributionMarkdown(
        feed: AiNewsFeed,
        lineCount: Int,
        backend: FeedBackend
    ): String = when (backend) {
        FeedBackend.AI_NEWS_AGGREGATOR -> {
            val sourceName = "SuYxh / ai-news-aggregator"
            val dataUrl = when (feed.windowHours) {
                24, 168 -> "${AiNewsAggregatorApi.BASE_JSON_URL}/latest-${if (feed.windowHours >= 168) "7d" else "24h"}.json"
                else -> AiNewsAggregatorApi.BASE_JSON_URL
            }
            buildString {
                append("**数据来源**：开源聚合 [$sourceName](https://github.com/SuYxh/ai-news-aggregator) ")
                append("（[在线站点](https://suyxh.github.io/ai-news-aggregator/) · ")
                append("[JSON]($dataUrl)）。聚合更新时间：`${feed.generatedAt}`；本期正文生成依据约 **$lineCount** 条标题链接。\n\n")
                append("**说明**：正文由 DeepSeek 根据标题归纳撰写，细节请以原文链接为准。")
            }
        }
        FeedBackend.PERPS_NEWS -> buildString {
            append("**数据来源**：开源项目 [gunksd/Perps-news](https://github.com/gunksd/Perps-news) ")
            append("（[JSON 快照](${PerpsNewsApi.NEWS_JSON_URL})）。")
            append("本页按最近 **${feed.windowHours}** 小时从快照中筛选；筛选后条目数：**${feed.totalItems}**；")
            append("正文生成依据约 **$lineCount** 条标题链接；整理时间戳：`${feed.generatedAt}`。\n\n")
            append("**说明**：正文由 DeepSeek 根据标题归纳撰写，不构成投资建议；细节与数据请以原文链接为准。")
        }
    }

    data class DigestUiState(
        val feedBackend: FeedBackend = FeedBackend.AI_NEWS_AGGREGATOR,
        val windowHours: Int = 24,
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
