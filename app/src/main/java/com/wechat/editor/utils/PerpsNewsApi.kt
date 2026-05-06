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
 * committed JSON ([news.json](https://raw.githubusercontent.com/gunksd/Perps-news/main/data/news.json)),
 * plus the same repo’s [indices.json](https://raw.githubusercontent.com/gunksd/Perps-news/main/data/indices.json)
 * snapshot for brief market context (mirrors upstream’s index module).
 *
 * Upstream collects headlines via RSS/HTML schedulers and commits static JSON; the newest row in
 * `news.json` can lag wall-clock. When that anchor falls outside the requested window relative to
 * “now”, filtering uses the snapshot’s latest `time` as the reference so 24h / 7d still returns rows.
 */
object PerpsNewsApi {

    const val NEWS_JSON_URL =
        "https://raw.githubusercontent.com/gunksd/Perps-news/main/data/news.json"

    const val INDICES_JSON_URL =
        "https://raw.githubusercontent.com/gunksd/Perps-news/main/data/indices.json"

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
                val indicesContext = fetchIndicesContextBlock()
                Result.Success(
                    AiNewsFeed(
                        generatedAt = generatedAt,
                        windowHours = windowHours,
                        totalItems = items.size,
                        items = items,
                        perpsContext = indicesContext
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

    /** Same snapshot style as Perps-news web app index panel; failure is non-fatal. */
    private fun fetchIndicesContextBlock(): String? {
        return runCatching {
            val request = Request.Builder().url(INDICES_JSON_URL).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                val arr = runCatching { JSONArray(body) }.getOrNull() ?: return@use null
                if (arr.length() == 0) return@use null
                buildString {
                    append(
                        "【指数快照】以下为上游 Perps-news 仓库 data/indices.json 中的条目（与新闻同源定时提交，仅供参考）：\n"
                    )
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val name = o.optString("name").trim().ifBlank { o.optString("symbol").trim() }
                        val sym = o.optString("symbol").trim()
                        val label = when {
                            name.isNotEmpty() && sym.isNotEmpty() && name != sym -> "$name（$sym）"
                            name.isNotEmpty() -> name
                            sym.isNotEmpty() -> sym
                            else -> continue
                        }
                        val price = o.optDouble("price", Double.NaN)
                        val priceStr = if (price.isNaN()) "—" else String.format(Locale.US, "%.2f", price)
                        val ch = o.optDouble("change", Double.NaN)
                        val chStr = if (ch.isNaN()) "—" else String.format(Locale.US, "%+.2f", ch)
                        val pct = o.optDouble("changePercent", Double.NaN)
                        val pctStr = if (pct.isNaN()) "—" else String.format(Locale.US, "%+.2f%%", pct)
                        val ts = o.optString("timestamp").trim()
                        append("- $label：最新价 $priceStr，涨跌 $chStr，涨跌幅 $pctStr")
                        if (ts.isNotEmpty()) append("；快照时间 $ts")
                        append('\n')
                    }
                }.trim().takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
    }
}
