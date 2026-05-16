package com.wechat.editor.utils

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GitHubContentsApi {
    private const val API_ROOT = "https://api.github.com"
    private const val API_VERSION = "2022-11-28"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val path: String, val htmlUrl: String?) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun saveMarkdown(
        token: String,
        target: GitHubRepositoryTarget,
        fileName: String,
        markdown: String,
        commitMessage: String
    ): Result = withContext(Dispatchers.IO) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            return@withContext Result.Error("请先在设置中填写 GitHub Token")
        }

        val filePath = target.filePath(fileName)
        val existingSha = when (val shaResult = fetchExistingSha(trimmedToken, target, filePath)) {
            is ShaResult.Found -> shaResult.sha
            ShaResult.NotFound -> null
            is ShaResult.Error -> return@withContext Result.Error(shaResult.message)
        }

        val body = JSONObject().apply {
            put("message", commitMessage.ifBlank { "Save article from WeChat editor" })
            put("content", Base64.encodeToString(markdown.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            put("branch", target.branch)
            if (existingSha != null) put("sha", existingSha)
        }

        val request = Request.Builder()
            .url(contentsUrl(target, filePath))
            .addGitHubHeaders(trimmedToken)
            .put(body.toString().toRequestBody(jsonMedia))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.Error(readGitHubError(responseBody) ?: "保存失败 HTTP ${response.code}")
                }

                val content = JSONObject(responseBody).optJSONObject("content")
                Result.Success(
                    path = content?.optString("path")?.takeIf { it.isNotBlank() } ?: filePath,
                    htmlUrl = content?.optString("html_url")?.takeIf { it.isNotBlank() }
                )
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    private sealed class ShaResult {
        data class Found(val sha: String) : ShaResult()
        data class Error(val message: String) : ShaResult()
        object NotFound : ShaResult()
    }

    private fun fetchExistingSha(
        token: String,
        target: GitHubRepositoryTarget,
        filePath: String
    ): ShaResult {
        val request = Request.Builder()
            .url("${contentsUrl(target, filePath)}?ref=${encodeSegment(target.branch)}")
            .addGitHubHeaders(token)
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                when {
                    response.code == 404 -> ShaResult.NotFound
                    !response.isSuccessful -> ShaResult.Error(
                        readGitHubError(responseBody) ?: "读取文件状态失败 HTTP ${response.code}"
                    )
                    else -> {
                        val sha = JSONObject(responseBody).optString("sha")
                        if (sha.isBlank()) ShaResult.NotFound else ShaResult.Found(sha)
                    }
                }
            }
        }.getOrElse { e ->
            ShaResult.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    private fun Request.Builder.addGitHubHeaders(token: String): Request.Builder {
        return addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", API_VERSION)
            .addHeader("Content-Type", "application/json")
    }

    private fun contentsUrl(target: GitHubRepositoryTarget, filePath: String): String {
        val owner = encodeSegment(target.owner)
        val repo = encodeSegment(target.repo)
        val path = filePath.trim('/').split('/').joinToString("/") { encodeSegment(it) }
        return "$API_ROOT/repos/$owner/$repo/contents/$path"
    }

    private fun encodeSegment(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("%7E", "~")
    }

    private fun readGitHubError(responseBody: String): String? {
        return runCatching {
            JSONObject(responseBody).optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
