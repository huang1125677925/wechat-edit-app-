package com.wechat.editor.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible DeepSeek API (https://api.deepseek.com/v1/chat/completions).
 * Model: DeepSeek V4 Flash — typography-only Markdown normalization (no wording changes).
 */
object DeepSeekApi {

    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MODEL = "deepseek-v4-flash"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val markdown: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun polishArticleMarkdown(
        apiKey: String,
        title: String,
        author: String,
        bodyMarkdown: String
    ): Result = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.Error("请先在设置中填写 DeepSeek API Key")
        }

        val systemPrompt = """
            你是中文 Markdown 的「纯排版」助手。用户会提供标题、作者和正文（Markdown）。

            【最高优先级：不得改动正文语义与用词】
            - 禁止润色、改写、换同义词、增删句子、调整语序、概括或扩写。
            - 每个列表项、段落里「说了什么」必须与原文一致；只能改标点、空白、Markdown 结构与换行分段。
            - 标题与作者仅供上下文；只输出处理后的正文 Markdown 全文，不要重复输出「标题：」「作者：」等元信息块。

            【必须执行的引号与加粗规则（优先级仅次于语义保留）】
            规则 A：列表项开头的「小标题关键词」必须加粗，且统一用直角引号「…」包裹，不得使用英文双引号 "…" 或弯引号 "…"。
              - 识别方式：列表项以 `- "xxx"：` 或 `- "xxx"：` 或 `- xxx：` 形式开头，xxx 即小标题关键词。
              - 转换规则：将其统一改写为 `- **「xxx」**：` 形式（加粗 + 直角引号，冒号紧跟其后，冒号后内容原文照抄）。
              - 示例（原文 → 排版后）：
                  原：- "严禁手写文档"：他认为写文档的时代已结束……
                  后：- **「严禁手写文档」**：他认为写文档的时代已结束……
                  原：- "软件时代终结"：断言软件时代已彻底终结……
                  后：- **「软件时代终结」**：断言软件时代已彻底终结……
                  原：- 提出"AIQ"新指标：未来企业核心指标……
                  后：- **「提出「AIQ」新指标」**：未来企业核心指标……
                    （若小标题内部还有英文缩写被双引号包围，内层改用单书名号『…』或直接去掉引号，保证字面不变）
            规则 B：正文段落内的强调词（被 "…" 或 "…" 包裹的词组）若不是小标题，改用直角引号「…」，不加粗。
            规则 C：**…** 内部绝对不能再出现英文双引号 " 或弯引号 " "；若出现，按规则 A 示例转换。

            【其他格式规范】
            - 中英文、中文与数字之间按常见规范补半角空格（如「AI 商数」「KPI/OKR」与两侧中文之间），不改动字母与数字本身。
            - 中文叙述优先全角标点（，。：；？！）；列表项内说明性冒号用全角「：」；半角标点仅保留在 URL、代码、纯英文片段处。
            - 列表、标题层级（#）、引用（>）、空行分段：仅在明显利于渲染且不改变阅读顺序时调整。
            - 保留链接 ![](…) `代码` 行内代码与代码围栏；不要改 URL 与代码字面量。

            【输出】
            - 不要前言、不要解释；只输出处理后的正文 Markdown 字符串（与输入同一语言与信息量）。
        """.trimIndent()

        val userPayload = buildString {
            append("标题：\n")
            append(title.ifBlank { "（无）" })
            append("\n\n作者：\n")
            append(author.ifBlank { "（无）" })
            append("\n\n正文 Markdown：\n")
            append(bodyMarkdown)
        }

        val root = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPayload)
                })
            })
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(root.toString().toRequestBody(jsonMedia))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@use Result.Error(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "请求失败 HTTP ${response.code}"
                    )
                }
                val json = JSONObject(responseBody)
                val choice = json.optJSONArray("choices")?.optJSONObject(0)
                val content = choice?.optJSONObject("message")?.optString("content")
                    ?: return@use Result.Error("响应格式异常")
                val trimmed = content.trim()
                if (trimmed.isEmpty()) {
                    return@use Result.Error("模型返回为空")
                }
                Result.Success(trimmed)
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }
}
