package com.wechat.editor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wechat.editor.data.UserSettingsStore
import com.wechat.editor.model.AiNewsFeed
import com.wechat.editor.model.AiNewsItem
import com.wechat.editor.model.ChatMessage
import com.wechat.editor.model.ChatRole
import com.wechat.editor.utils.AiNewsAggregatorApi
import com.wechat.editor.utils.DeepSeekApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiNewsChatViewModel(application: Application) : AndroidViewModel(application) {

    private val userSettingsStore = UserSettingsStore(application)

    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui.asStateFlow()

    private var loadedFeed: AiNewsFeed? = null

    /** Cached numbered lines sent to the model as grounding context. */
    private var newsContextLines: String = ""

    fun setWindowHours(hours: Int) {
        if (_ui.value.windowHours == hours) return
        loadedFeed = null
        newsContextLines = ""
        _ui.update {
            it.copy(
                windowHours = hours,
                feedLoaded = false,
                feedError = null,
                isLoadingFeed = false
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
                            feedLoaded = false,
                            feedError = r.message
                        )
                    }
                }
                is AiNewsAggregatorApi.Result.Success -> {
                    loadedFeed = r.feed
                    newsContextLines = buildContextLines(r.feed.items)
                    val welcomeMsg = buildWelcomeMessage(r.feed)
                    _ui.update {
                        it.copy(
                            isLoadingFeed = false,
                            feedLoaded = true,
                            feedError = null,
                            feedInfo = FeedInfo(
                                generatedAt = r.feed.generatedAt,
                                windowHours = r.feed.windowHours,
                                totalItems = r.feed.totalItems
                            ),
                            messages = listOf(welcomeMsg)
                        )
                    }
                }
            }
        }
    }

    fun updateInput(text: String) {
        _ui.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _ui.value.inputText.trim()
        if (text.isBlank() || _ui.value.isReplying) return

        val feed = loadedFeed
        if (feed == null) {
            appendErrorMessage("请先拉取资讯数据后再提问")
            return
        }

        val apiKey = userSettingsStore.getDeepSeekApiKey()
        if (apiKey.isBlank()) {
            appendErrorMessage("请先在设置中填写 DeepSeek API Key")
            return
        }

        val userMsg = ChatMessage(role = ChatRole.USER, content = text)
        val historyForApi = _ui.value.messages.filter { !it.isError }

        _ui.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isReplying = true
            )
        }

        viewModelScope.launch {
            when (
                val result = DeepSeekApi.chatWithNewsContext(
                    apiKey = apiKey,
                    newsContextLines = newsContextLines,
                    feedGeneratedAt = feed.generatedAt,
                    windowHours = feed.windowHours,
                    history = historyForApi,
                    userMessage = text
                )
            ) {
                is DeepSeekApi.Result.Error -> {
                    appendErrorMessage(result.message)
                    _ui.update { it.copy(isReplying = false) }
                }
                is DeepSeekApi.Result.Success -> {
                    val assistantMsg = ChatMessage(role = ChatRole.ASSISTANT, content = result.markdown)
                    _ui.update {
                        it.copy(
                            messages = it.messages + assistantMsg,
                            isReplying = false
                        )
                    }
                }
            }
        }
    }

    fun clearChat() {
        val feed = loadedFeed
        val welcome = if (feed != null) listOf(buildWelcomeMessage(feed)) else emptyList()
        _ui.update { it.copy(messages = welcome, inputText = "") }
    }

    private fun appendErrorMessage(msg: String) {
        val errMsg = ChatMessage(role = ChatRole.ASSISTANT, content = msg, isError = true)
        _ui.update { it.copy(messages = it.messages + errMsg) }
    }

    private fun buildContextLines(items: List<AiNewsItem>): String {
        return items
            .asSequence()
            .filter { it.url.isNotBlank() }
            .take(MAX_CONTEXT_ITEMS)
            .mapIndexed { index, item ->
                val title = item.displayTitle()
                val site = sourceLabel(item)
                val time = item.publishedAt?.let { " · $it" }.orEmpty()
                "${index + 1}. $title | $site$time | ${item.url}"
            }
            .joinToString("\n")
    }

    private fun buildWelcomeMessage(feed: AiNewsFeed): ChatMessage {
        val windowLabel = if (feed.windowHours >= 168) "近 7 天" else "近 24 小时"
        val content = buildString {
            append("你好！我已加载 **${feed.totalItems}** 条 $windowLabel AI 科技资讯（更新时间：`${feed.generatedAt}`）。\n\n")
            append("你可以问我：\n")
            append("- 「今天 AI 领域有哪些热点？」\n")
            append("- 「有没有关于大模型的新闻？」\n")
            append("- 「帮我总结一下最近 OpenAI 的动态」\n")
            append("- 「有哪些值得关注的开源项目新闻？」\n\n")
            append("我会基于以上资讯为你解答，并附上原文链接。")
        }
        return ChatMessage(role = ChatRole.ASSISTANT, content = content)
    }

    private fun sourceLabel(item: AiNewsItem): String {
        return listOf(item.siteName, item.source)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" · ")
            .ifBlank { "未知来源" }
    }

    data class ChatUiState(
        val windowHours: Int = 24,
        val isLoadingFeed: Boolean = false,
        val feedLoaded: Boolean = false,
        val feedError: String? = null,
        val feedInfo: FeedInfo? = null,
        val messages: List<ChatMessage> = emptyList(),
        val inputText: String = "",
        val isReplying: Boolean = false
    )

    data class FeedInfo(
        val generatedAt: String,
        val windowHours: Int,
        val totalItems: Int
    )

    companion object {
        private const val MAX_CONTEXT_ITEMS = 300
    }
}
