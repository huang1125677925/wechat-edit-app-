package com.wechat.editor.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI-compatible chat completions for [DeepSeek](https://api.deepseek.com).
 * Model IDs: [MODEL_V4_FLASH], [MODEL_V4_PRO] (see official API docs).
 */
object DeepSeekChatApi {

    const val MODEL_V4_FLASH = "deepseek-v4-flash"
    const val MODEL_V4_PRO = "deepseek-v4-pro"

    private const val CHAT_URL = "https://api.deepseek.com/v1/chat/completions"

    sealed class Result {
        data class Success(val content: String) : Result()
        data class Error(val message: String, val httpCode: Int = -1) : Result()
    }

    suspend fun chatCompletion(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String
    ): Result = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.Error("请先填写 API Key")
        }
        try {
            val body = JSONObject().apply {
                put("model", model.trim().ifEmpty { MODEL_V4_FLASH })
                put("temperature", 0.35)
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", systemPrompt))
                        put(JSONObject().put("role", "user").put("content", userMessage))
                    }
                )
            }

            val conn = (URL(CHAT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 30_000
                readTimeout = 120_000
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val text = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
            conn.disconnect()

            if (code !in 200..299) {
                return@withContext Result.Error(parseErrorMessage(text, code), code)
            }

            val content = extractAssistantContent(text)
                ?: return@withContext Result.Error("响应中未找到正文", code)

            Result.Success(stripOuterMarkdownFence(content.trim()))
        } catch (e: Exception) {
            Result.Error("请求失败：${e.localizedMessage}")
        }
    }

    private fun readStream(stream: java.io.InputStream?): String {
        stream ?: return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val err = json.optJSONObject("error")
            val msg = err?.optString("message").orEmpty().ifBlank { json.optString("message") }
            if (msg.isNotBlank()) "$msg（HTTP $code）" else "请求失败（HTTP $code）"
        } catch (_: Exception) {
            body.ifBlank { "请求失败（HTTP $code）" }
        }
    }

    private fun extractAssistantContent(body: String): String? {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
        return message.optString("content", "").takeIf { it.isNotBlank() }
    }

    /** If the model wrapped the whole answer in ``` or ```markdown, unwrap once. */
    private fun stripOuterMarkdownFence(text: String): String {
        var t = text.trim()
        val fence = Regex("^```(?:markdown|md)?\\s*\\n([\\s\\S]*?)\\n```\\s*$", RegexOption.IGNORE_CASE)
        val m = fence.find(t)
        if (m != null) return m.groupValues[1].trim()
        if (t.startsWith("```")) {
            val lines = t.lines()
            if (lines.size >= 2 && lines.last().trim() == "```") {
                val inner = lines.drop(1).dropLast(1).joinToString("\n").trim()
                if (inner.isNotEmpty()) return inner
            }
        }
        return t
    }
}
