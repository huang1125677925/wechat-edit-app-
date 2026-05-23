package com.wechat.editor.utils

import android.util.JsonReader
import android.util.JsonToken
import com.wechat.editor.model.AiNewsFeed
import com.wechat.editor.model.AiNewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Fetches structured AI news from [SuYxh/ai-news-aggregator](https://github.com/SuYxh/ai-news-aggregator)
 * GitHub Pages JSON endpoints (updated ~every 2 hours upstream).
 *
 * Tries multiple mirrors (GitHub Pages, raw.githubusercontent.com, jsDelivr) because
 * GitHub Pages is often unreachable or slow on some networks.
 */
object AiNewsAggregatorApi {

    const val BASE_JSON_URL = "https://suyxh.github.io/ai-news-aggregator/data"

    private const val USER_AGENT = "WeChatEditor/1.0 (Android; +https://github.com/huang1125677925/wechat-edit-app)"

    private val FEED_URL_TEMPLATES = listOf(
        "$BASE_JSON_URL/%s",
        "https://raw.githubusercontent.com/SuYxh/ai-news-aggregator/main/data/%s",
        "https://cdn.jsdelivr.net/gh/SuYxh/ai-news-aggregator@main/data/%s"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val feed: AiNewsFeed) : Result()
        data class Error(val message: String) : Result()
    }

    /** @visibleForTesting */
    internal fun feedUrlsForWindow(windowHours: Int): List<String>? {
        val fileName = fileNameForWindow(windowHours) ?: return null
        return FEED_URL_TEMPLATES.map { it.format(fileName) }
    }

    suspend fun fetchLatest(windowHours: Int): Result = withContext(Dispatchers.IO) {
        val fileName = fileNameForWindow(windowHours)
            ?: return@withContext Result.Error("不支持的时间窗口：$windowHours 小时")

        val failures = mutableListOf<String>()
        for (url in FEED_URL_TEMPLATES.map { it.format(fileName) }) {
            when (val attempt = downloadAndParse(url)) {
                is Result.Success -> return@withContext attempt
                is Result.Error -> failures.add(attempt.message)
            }
        }
        Result.Error(
            failures.distinct().take(3).joinToString("；").ifBlank { "网络错误" }
                .let { "无法拉取资讯数据：$it" }
        )
    }

    private fun fileNameForWindow(windowHours: Int): String? = when (windowHours) {
        24 -> "latest-24h.json"
        168 -> "latest-7d.json"
        else -> null
    }

    private fun downloadAndParse(url: String): Result {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.Error("${hostLabel(url)} HTTP ${response.code}")
                }
                if (body.isBlank()) {
                    return@use Result.Error("${hostLabel(url)} 响应为空")
                }
                parseFeedBody(body)
            }
        }.getOrElse { e ->
            Result.Error("${hostLabel(url)} ${e.localizedMessage ?: e.message ?: "网络错误"}")
        }
    }

    private fun hostLabel(url: String): String =
        runCatching {
            val host = java.net.URI(url).host.orEmpty()
            when {
                host.contains("jsdelivr") -> "jsDelivr"
                host.contains("raw.githubusercontent") -> "GitHub Raw"
                host.contains("github.io") -> "GitHub Pages"
                host.isNotBlank() -> host
                else -> "上游"
            }
        }.getOrDefault("上游")

    internal fun parseFeedBody(body: String): Result {
        if (body.isBlank()) return Result.Error("响应为空")
        return try {
            parseFeedWithJsonReader(body)
        } catch (_: OutOfMemoryError) {
            Result.Error("数据量过大导致内存不足，请改用「24 小时」窗口后重试")
        } catch (e: JSONException) {
            Result.Error("JSON 解析失败：${e.message ?: "格式异常"}")
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: e.message ?: "解析失败")
        }
    }

    private fun parseFeedWithJsonReader(body: String): Result {
        val reader = JsonReader(StringReader(body))
        var generatedAt = ""
        var windowHours = 24
        var totalItems = 0
        val items = ArrayList<AiNewsItem>(512)
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "generated_at" -> generatedAt = reader.nextString()
                "window_hours" -> windowHours = reader.nextInt()
                "total_items" -> totalItems = reader.nextInt()
                "items" -> readItemsArray(reader, items)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        reader.close()
        if (items.isEmpty() && totalItems > 0) {
            return Result.Error("解析成功但未读取到条目，请稍后重试")
        }
        val feed = AiNewsFeed(
            generatedAt = generatedAt,
            windowHours = windowHours,
            totalItems = if (totalItems > 0) totalItems else items.size,
            items = items
        )
        return Result.Success(feed)
    }

    private fun readItemsArray(reader: JsonReader, items: ArrayList<AiNewsItem>) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
                continue
            }
            items.add(readItem(reader))
        }
        reader.endArray()
    }

    private fun readItem(reader: JsonReader): AiNewsItem {
        var id = ""
        var siteName = ""
        var siteId = ""
        var source = ""
        var title = ""
        var url = ""
        var publishedAt: String? = null
        var titleZh: String? = null
        var titleBilingual: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "site_name" -> siteName = reader.nextString()
                "site_id" -> siteId = reader.nextString()
                "source" -> source = reader.nextString()
                "title" -> title = reader.nextString()
                "url" -> url = reader.nextString()
                "published_at" -> {
                    val v = reader.nextString()
                    publishedAt = v.takeIf { it.isNotBlank() }
                }
                "title_zh" -> {
                    val v = reader.nextString()
                    titleZh = v.takeIf { it.isNotBlank() }
                }
                "title_bilingual" -> {
                    val v = reader.nextString()
                    titleBilingual = v.takeIf { it.isNotBlank() }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val resolvedSite = siteName.ifBlank { siteId }
        return AiNewsItem(
            id = id,
            siteName = resolvedSite,
            source = source,
            title = title,
            url = url,
            publishedAt = publishedAt,
            titleZh = titleZh,
            titleBilingual = titleBilingual
        )
    }
}
