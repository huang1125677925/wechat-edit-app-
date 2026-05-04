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
 * Minimal client for the img.remit.ee image hosting API.
 *
 * No authentication required. POST multipart/form-data with a "file" field.
 * Response: { "success": true, "url": "/api/file/..." }
 * Full image URL = "https://img.remit.ee" + url
 *
 * Limits: max 20 MB per file, supports JPG/PNG/GIF/WebP/PDF.
 */
object RemiteeApi {

    private const val BASE_URL = "https://img.remit.ee"
    private const val UPLOAD_URL = "$BASE_URL/api/upload"
    private const val BOUNDARY = "----RemiteeBoundary7MA4YWxkTrZu0gW"

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
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            DataOutputStream(conn.outputStream).use { out ->
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
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
            val success = json.optBoolean("success", false)
            val urlPath = json.optString("url", "")

            if (success && urlPath.isNotBlank()) {
                Result.Success(UploadedImage(url = "$BASE_URL$urlPath"))
            } else {
                val message = json.optString("message", "上传失败")
                Result.Error(if (message.isNotBlank()) message else "响应中缺少图片链接", code)
            }
        } catch (e: Exception) {
            Result.Error("响应解析失败：${e.localizedMessage}", code)
        }
    }

    private fun readStream(stream: InputStream?): String {
        stream ?: return ""
        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
    }
}
