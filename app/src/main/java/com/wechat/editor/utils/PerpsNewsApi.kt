package com.wechat.editor.utils

import com.wechat.editor.model.AiNewsFeed
import com.wechat.editor.model.AiNewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Fetches raw news from [gunksd/Perps-news](https://github.com/gunksd/Perps-news)
 * committed JSON ([news.json](https://raw.githubusercontent.com/gunksd/Perps-news/main/data/news.json)).
 *
 * The file is a periodically committed snapshot; its newest row can lag wall-clock by weeks or months.
 * Time-window filtering therefore anchors to the newest parsed `time` in the snapshot when that anchor
 * falls outside the requested window relative to "now", so 24h / 7d still returns rows from the snapshot.
 */
object PerpsNewsApi {

    const val NEWS_JSON_URL =
        "https://raw.githubusercontent.com/gunksd/Perps-news/main/data/news.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** RFC 822 / RSS style dates in upstream JSON, e.g. `Sat, 17 Jan 2026 12:11:20 +0800`. */
    private val rfc822 = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply {
        isLenient = true
    }

    sealed class Result {
        data class Success(val feed: AiNewsFeed) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun fetchNews(windowHours: Int): Result = withContext(Dispatchers.IO) {
        if (windowHours != 24 && windowHours != 168) {
            return@withContext Result.Error("不支持的时间窗口：$windowHours 小时")
        }
        runCatching {
            val request = Request.Builder().url(NEWS_JSON_URL).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.Error("请求失败 HTTP ${response.code}")
                }
                val arr = runCatching { JSONArray(body) }.getOrElse {
                    return@use Result.Error("响应不是有效的 JSON 数组")
                }
                val windowMs = windowHours * 3_600_000L
                val now = System.currentTimeMillis()
                // GitHub 上的 news.json 是定时提交的静态快照，可能比「当前时刻」滞后数月。
                // 若仍用 wall-clock 做 cutoff，24h/7d 窗口会把快照内条目全部滤掉。此处用快照内
                // 最新一条的时间作为参考点：仅当参考点仍足够「接近」当前时间时才用 now。
                var latestInSnapshot: Long? = null
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val t = parseTimeMillis(o.optString("time"))
                    if (t != null) {
                        latestInSnapshot = when (val cur = latestInSnapshot) {
                            null -> t
                            else -> maxOf(cur, t)
                        }
                    }
                }
                val referenceMs = when {
                    latestInSnapshot == null -> now
                    latestInSnapshot < now - windowMs -> latestInSnapshot
                    else -> now
                }
                val cutoff = referenceMs - windowMs
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val timeStr = o.optString("time")
                        val publishedMs = parseTimeMillis(timeStr)
                        if (publishedMs != null && publishedMs < cutoff) continue
                        val id = o.optString("id")
                        val sourceId = o.optString("source")
                        val title = o.optString("title")
                        val url = o.optString("url")
                        if (title.isBlank() || url.isBlank()) continue
                        add(
                            AiNewsItem(
                                id = id.ifBlank { "${sourceId}_${i}_${title.hashCode()}" },
                                siteName = "Perps News",
                                source = sourceLabel(sourceId),
                                title = title,
                                url = url,
                                publishedAt = timeStr.takeIf { it.isNotBlank() },
                                titleZh = null,
                                titleBilingual = null
                            )
                        )
                    }
                }.sortedByDescending { item ->
                    parseTimeMillis(item.publishedAt.orEmpty()) ?: Long.MIN_VALUE
                }
                val generatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }.format(Date())
                Result.Success(
                    AiNewsFeed(
                        generatedAt = generatedAt,
                        windowHours = windowHours,
                        totalItems = items.size,
                        items = items
                    )
                )
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    private fun sourceLabel(sourceId: String): String {
        val s = sourceId.trim()
        if (s.isEmpty()) return "未知来源"
        return s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun parseTimeMillis(time: String): Long? {
        if (time.isBlank()) return null
        return try {
            rfc822.parse(time)?.time
        } catch (_: ParseException) {
            null
        }
    }
}
