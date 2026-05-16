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

    private val _githubToken = MutableStateFlow(store.getGitHubToken())
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _githubDirectory = MutableStateFlow(store.getGitHubDirectory())
    val githubDirectory: StateFlow<String> = _githubDirectory.asStateFlow()

    private val _githubBranch = MutableStateFlow(store.getGitHubBranch())
    val githubBranch: StateFlow<String> = _githubBranch.asStateFlow()

    fun updateApiKeyPreview(value: String) {
        _apiKey.value = value
    }

    fun updateGitHubTokenPreview(value: String) {
        _githubToken.value = value
    }

    fun updateGitHubDirectoryPreview(value: String) {
        _githubDirectory.value = value
    }

    fun updateGitHubBranchPreview(value: String) {
        _githubBranch.value = value
    }

    fun saveApiKey() {
        store.setDeepSeekApiKey(_apiKey.value)
        _apiKey.value = store.getDeepSeekApiKey()
    }

    fun saveGitHubSettings() {
        store.setGitHubSettings(
            token = _githubToken.value,
            directory = _githubDirectory.value,
            branch = _githubBranch.value
        )
        _githubToken.value = store.getGitHubToken()
        _githubDirectory.value = store.getGitHubDirectory()
        _githubBranch.value = store.getGitHubBranch()
    }

    fun reloadFromDisk() {
        _apiKey.value = store.getDeepSeekApiKey()
        _githubToken.value = store.getGitHubToken()
        _githubDirectory.value = store.getGitHubDirectory()
        _githubBranch.value = store.getGitHubBranch()
    }
}
