package com.wechat.editor.utils

import android.content.Context

/**
 * Local user settings (API keys are stored on-device only; never embedded in the app).
 */
class UserPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeepSeekApiKey(): String = prefs.getString(KEY_DEEPSEEK_API_KEY, "").orEmpty()

    fun setDeepSeekApiKey(value: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, value.trim()).apply()
    }

    fun getDeepSeekModel(): String =
        prefs.getString(KEY_DEEPSEEK_MODEL, DeepSeekChatApi.MODEL_V4_FLASH).orEmpty()
            .ifBlank { DeepSeekChatApi.MODEL_V4_FLASH }

    fun setDeepSeekModel(model: String) {
        prefs.edit().putString(
            KEY_DEEPSEEK_MODEL,
            model.trim().ifBlank { DeepSeekChatApi.MODEL_V4_FLASH }
        ).apply()
    }

    companion object {
        private const val PREFS_NAME = "wechat_editor_user_prefs"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
    }
}
