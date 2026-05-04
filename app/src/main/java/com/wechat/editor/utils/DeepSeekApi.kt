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
 * Model: DeepSeek V4 Flash — suitable for article polish / layout assistance.
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
            你是微信公众号文章的排版与编辑助手。用户会提供标题、作者和正文（Markdown）。
            请只做：润色语句、统一标点与空格、优化标题层级与列表结构、适度分段，使 Markdown 更适合公众号排版。
            不要编造事实或删减用户想表达的核心信息；不要输出任何解释或前言；只输出优化后的完整正文 Markdown。
            保留用户已有的 Markdown/HTML 内联样式（如 **粗体**、图片链接、代码块等），除非明显错误否则不要删除。
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
            put("temperature", 0.35)
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
