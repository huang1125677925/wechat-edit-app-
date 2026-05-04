package com.wechat.editor.data

import android.content.Context

/**
 * Stores user-supplied credentials (e.g. DeepSeek API key) locally on device.
 */
class UserSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeepSeekApiKey(): String = prefs.getString(KEY_DEEPSEEK_API_KEY, "")?.trim().orEmpty()

    fun setDeepSeekApiKey(key: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply()
    }

    companion object {
        private const val PREFS_NAME = "wechat_editor_user_settings"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    }
}
