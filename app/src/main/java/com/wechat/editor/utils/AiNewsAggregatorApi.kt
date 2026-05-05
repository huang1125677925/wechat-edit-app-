package com.wechat.editor.utils

import com.wechat.editor.model.AiNewsFeed
import com.wechat.editor.model.AiNewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches structured AI news from [SuYxh/ai-news-aggregator](https://github.com/SuYxh/ai-news-aggregator)
 * GitHub Pages JSON endpoints (updated ~every 2 hours upstream).
 */
object AiNewsAggregatorApi {

    const val BASE_JSON_URL = "https://suyxh.github.io/ai-news-aggregator/data"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val feed: AiNewsFeed) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun fetchLatest(windowHours: Int): Result = withContext(Dispatchers.IO) {
        val path = when (windowHours) {
            24 -> "latest-24h.json"
            168 -> "latest-7d.json"
            else -> return@withContext Result.Error("不支持的时间窗口：$windowHours 小时")
        }
        val url = "$BASE_JSON_URL/$path"
        runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.Error("请求失败 HTTP ${response.code}")
                }
                parseFeed(JSONObject(body))
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    private fun parseFeed(root: JSONObject): Result {
        val itemsJson = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until itemsJson.length()) {
                val o = itemsJson.optJSONObject(i) ?: continue
                add(
                    AiNewsItem(
                        id = o.optString("id"),
                        siteName = o.optString("site_name").ifBlank { o.optString("site_id") },
                        source = o.optString("source"),
                        title = o.optString("title"),
                        url = o.optString("url"),
                        publishedAt = o.optString("published_at").takeIf { it.isNotBlank() },
                        titleZh = o.optString("title_zh").takeIf { it.isNotBlank() },
                        titleBilingual = o.optString("title_bilingual").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        val feed = AiNewsFeed(
            generatedAt = root.optString("generated_at"),
            windowHours = root.optInt("window_hours", 24),
            totalItems = root.optInt("total_items", items.size),
            items = items
        )
        return Result.Success(feed)
    }
}
