package com.wechat.editor.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for the Hello图床 (helloimg.com) v1 REST API.
 *
 * All I/O runs on [Dispatchers.IO] via suspend functions – callers live in coroutine scope.
 */
object HelloImgApi {

    private const val BASE_URL = "https://www.helloimg.com/api/v1"
    private const val BOUNDARY = "----WeChatEditorBoundary7MA4YWxkTrZu0gW"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    }

    data class UploadedImage(
        val key: String,
        val name: String,
        val url: String,
        val thumbnailUrl: String,
        val markdown: String,
        val markdownWithLink: String,
        val sizeKb: Float
    )

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Upload an image [bytes] to Hello图床.
     *
     * @param token   Bearer token (empty → guest upload)
     * @param bytes   Raw image bytes
     * @param filename  Original filename including extension
     * @param mimeType  MIME type, e.g. "image/jpeg"
     * @param strategyId Optional storage strategy id
     * @param albumId    Optional album id
     */
    suspend fun uploadImage(
        token: String,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        strategyId: Int? = null,
        albumId: Int? = null
    ): Result<UploadedImage> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/upload")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            DataOutputStream(conn.outputStream).use { out ->
                // file part
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
                out.writeBytes("Content-Type: $mimeType\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n")

                // optional strategy_id
                if (strategyId != null) {
                    out.writeBytes("--$BOUNDARY\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"strategy_id\"\r\n\r\n")
                    out.writeBytes("$strategyId\r\n")
                }

                // optional album_id
                if (albumId != null) {
                    out.writeBytes("--$BOUNDARY\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"album_id\"\r\n\r\n")
                    out.writeBytes("$albumId\r\n")
                }

                out.writeBytes("--$BOUNDARY--\r\n")
                out.flush()
            }

            val responseCode = conn.responseCode
            val responseBody = readStream(
                if (responseCode in 200..299) conn.inputStream else conn.errorStream
            )
            conn.disconnect()

            parseUploadResponse(responseBody, responseCode)
        } catch (e: Exception) {
            Result.Error("网络请求失败：${e.localizedMessage}")
        }
    }

    private fun parseUploadResponse(body: String, code: Int): Result<UploadedImage> {
        return try {
            val json = JSONObject(body)
            val status = json.optBoolean("status", false)
            val message = json.optString("message", "未知错误")

            // Try to extract image data regardless of status. Some server-side errors
            // (e.g. content-review service billing issues) cause status=false even though
            // the image was stored successfully and a URL is present in the response.
            val data = json.optJSONObject("data")
            val links = data?.optJSONObject("links")
            val url = links?.optString("url").orEmpty()

            if (url.isNotBlank()) {
                Result.Success(
                    UploadedImage(
                        key = data?.optString("key").orEmpty(),
                        name = data?.optString("name").orEmpty(),
                        url = url,
                        thumbnailUrl = links?.optString("thumbnail_url").orEmpty(),
                        markdown = links?.optString("markdown").orEmpty(),
                        markdownWithLink = links?.optString("markdown_with_link").orEmpty(),
                        sizeKb = data?.optDouble("size", 0.0)?.toFloat() ?: 0f
                    )
                )
            } else if (!status || code !in 200..299) {
                Result.Error(message, code)
            } else {
                Result.Error("响应中缺少图片链接", code)
            }
        } catch (e: Exception) {
            Result.Error("响应解析失败：${e.localizedMessage}", code)
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    data class UserProfile(
        val username: String,
        val name: String,
        val email: String,
        val imageNum: Int,
        val albumNum: Int
    )

    suspend fun getProfile(token: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val json = getJson("$BASE_URL/profile", token)
                ?: return@withContext Result.Error("响应为空")
            if (!json.optBoolean("status", false)) {
                return@withContext Result.Error(json.optString("message", "获取用户信息失败"))
            }
            val d = json.getJSONObject("data")
            Result.Success(
                UserProfile(
                    username = d.optString("username"),
                    name = d.optString("name"),
                    email = d.optString("email"),
                    imageNum = d.optInt("image_num"),
                    albumNum = d.optInt("album_num")
                )
            )
        } catch (e: Exception) {
            Result.Error("请求失败：${e.localizedMessage}")
        }
    }

    // ── Strategies ────────────────────────────────────────────────────────────

    data class Strategy(val id: Int, val name: String)

    suspend fun getStrategies(token: String): Result<List<Strategy>> = withContext(Dispatchers.IO) {
        try {
            val json = getJson("$BASE_URL/strategies", token)
                ?: return@withContext Result.Error("响应为空")
            if (!json.optBoolean("status", false)) {
                return@withContext Result.Error(json.optString("message", "获取策略失败"))
            }
            val arr = json.getJSONObject("data").getJSONArray("strategies")
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Strategy(obj.getInt("id"), obj.getString("name"))
            }
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error("请求失败：${e.localizedMessage}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getJson(urlString: String, token: String): JSONObject? {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val code = conn.responseCode
        val body = readStream(if (code in 200..299) conn.inputStream else conn.errorStream)
        conn.disconnect()
        return if (body.isBlank()) null else JSONObject(body)
    }

    private fun readStream(stream: InputStream?): String {
        stream ?: return ""
        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
    }
}
