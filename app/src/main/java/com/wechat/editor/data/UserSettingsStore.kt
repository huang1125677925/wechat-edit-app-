package com.wechat.editor.data

import android.content.Context

/**
 * Stores user-supplied credentials and publishing targets locally on device.
 */
class UserSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeepSeekApiKey(): String = prefs.getString(KEY_DEEPSEEK_API_KEY, "")?.trim().orEmpty()

    fun setDeepSeekApiKey(key: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply()
    }

    fun getGitHubToken(): String = prefs.getString(KEY_GITHUB_TOKEN, "")?.trim().orEmpty()

    fun getGitHubDirectory(): String = prefs.getString(KEY_GITHUB_DIRECTORY, "")?.trim().orEmpty()

    fun getGitHubBranch(): String = prefs.getString(KEY_GITHUB_BRANCH, DEFAULT_GITHUB_BRANCH)
        ?.trim()
        ?.ifBlank { DEFAULT_GITHUB_BRANCH }
        .orEmpty()

    fun setGitHubSettings(token: String, directory: String, branch: String) {
        prefs.edit()
            .putString(KEY_GITHUB_TOKEN, token.trim())
            .putString(KEY_GITHUB_DIRECTORY, directory.trim())
            .putString(KEY_GITHUB_BRANCH, branch.trim().ifBlank { DEFAULT_GITHUB_BRANCH })
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "wechat_editor_user_settings"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GITHUB_DIRECTORY = "github_directory"
        private const val KEY_GITHUB_BRANCH = "github_branch"
        private const val DEFAULT_GITHUB_BRANCH = "main"
    }
}
