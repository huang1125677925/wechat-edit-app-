package com.wechat.editor.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Thin wrapper over [SharedPreferences] for persisting app-level settings.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wechat_editor_prefs", Context.MODE_PRIVATE)

    var helloImgToken: String
        get() = prefs.getString(KEY_HELLOIMG_TOKEN, DEFAULT_TOKEN) ?: DEFAULT_TOKEN
        set(value) = prefs.edit { putString(KEY_HELLOIMG_TOKEN, value) }

    var helloImgStrategyId: Int
        get() = prefs.getInt(KEY_HELLOIMG_STRATEGY_ID, -1)
        set(value) = prefs.edit { putInt(KEY_HELLOIMG_STRATEGY_ID, value) }

    var helloImgAlbumId: Int
        get() = prefs.getInt(KEY_HELLOIMG_ALBUM_ID, -1)
        set(value) = prefs.edit { putInt(KEY_HELLOIMG_ALBUM_ID, value) }

    companion object {
        private const val KEY_HELLOIMG_TOKEN = "helloimg_token"
        private const val KEY_HELLOIMG_STRATEGY_ID = "helloimg_strategy_id"
        private const val KEY_HELLOIMG_ALBUM_ID = "helloimg_album_id"

        private const val DEFAULT_TOKEN = ""
    }
}
