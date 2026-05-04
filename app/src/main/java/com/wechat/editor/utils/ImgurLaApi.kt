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
 * Client for [imgur.la API v1.1](https://www.imgur.la/api-v1).
 *
 * POST multipart/form-data to `/api/1/upload` with field [source] (binary file).
 * Auth: `X-API-Key` header (public demo key; replace with your key from account settings if needed).
 *
 * Supported types per host: AVIF, JPG, PNG, GIF, WebP, etc.; max ~10 MB per file.
 */
object ImgurLaApi {

    private const val HOST = "https://www.imgur.la"
    private const val UPLOAD_URL = "$HOST/api/1/upload"
    /** Public API key from imgur.la documentation; override in settings for your own quota. */
    private const val API_KEY = "89bf00be2f91e3e5c74ea050d5b1d3f3"
    private const val BOUNDARY = "----ImgurLaBoundary7MA4YWxkTrZu0gW"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    }

    data class UploadedImage(val url: String)

    suspend fun uploadImage(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Result<UploadedImage> = withContext(Dispatchers.IO) {
        try {
            val url = URL(UPLOAD_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-API-Key", API_KEY)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            DataOutputStream(conn.outputStream).use { out ->
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"source\"; filename=\"$filename\"\r\n"
                )
                out.writeBytes("Content-Type: $mimeType\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n")
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
            val statusCode = json.optInt("status_code", code)
            val imageObj = json.optJSONObject("image")

            if (statusCode == 200 && imageObj != null) {
                val rawUrl = imageObj.optString("url", "")
                    .ifBlank { imageObj.optString("display_url", "") }
                    .ifBlank { imageObj.optString("url_viewer", "") }

                if (rawUrl.isNotBlank()) {
                    Result.Success(UploadedImage(url = absolutizeUrl(rawUrl)))
                } else {
                    Result.Error("响应中缺少图片链接", statusCode)
                }
            } else {
                val message = sequenceOf(
                    json.optString("status_txt", ""),
                    json.optJSONObject("error")?.optString("message").orEmpty()
                ).firstOrNull { it.isNotBlank() } ?: "上传失败"
                Result.Error(message, statusCode)
            }
        } catch (e: Exception) {
            Result.Error("响应解析失败：${e.localizedMessage}", code)
        }
    }

    private fun absolutizeUrl(raw: String): String {
        val fixedLocalhost = raw.replace("http://localhost", HOST)
        return when {
            fixedLocalhost.startsWith("http://") || fixedLocalhost.startsWith("https://") ->
                fixedLocalhost
            fixedLocalhost.startsWith("//") -> "https:$fixedLocalhost"
            fixedLocalhost.startsWith("/") -> "$HOST$fixedLocalhost"
            else -> fixedLocalhost
        }
    }

    private fun readStream(stream: InputStream?): String {
        stream ?: return ""
        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
    }
}
