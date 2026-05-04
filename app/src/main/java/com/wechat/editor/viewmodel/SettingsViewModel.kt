package com.wechat.editor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wechat.editor.data.UserSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = UserSettingsStore(application)

    private val _apiKey = MutableStateFlow(store.getDeepSeekApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun updateApiKeyPreview(value: String) {
        _apiKey.value = value
    }

    fun saveApiKey() {
        store.setDeepSeekApiKey(_apiKey.value)
        _apiKey.value = store.getDeepSeekApiKey()
    }

    fun reloadFromDisk() {
        _apiKey.value = store.getDeepSeekApiKey()
    }
}
