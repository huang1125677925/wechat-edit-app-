package com.wechat.editor.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wechat.editor.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val githubDirectory by viewModel.githubDirectory.collectAsState()
    val githubBranch by viewModel.githubBranch.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.reloadFromDisk()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "DeepSeek API",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在编辑器中可使用 DeepSeek V4 规范化 Markdown：仅调整标点、空格与结构，不改写正文措辞。请在 DeepSeek 开放平台创建 API Key 并粘贴到下方；密钥仅保存在本机，不会上传到我们的服务器。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "获取密钥：DeepSeek 开放平台 → API Keys（https://platform.deepseek.com）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = viewModel::updateApiKeyPreview,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::saveApiKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "GitHub 保存",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "填写具有仓库 Contents 读写权限的 GitHub Token，并指定仓库目录 URL 或 owner/repo/path。编辑器可将当前文章以 Markdown 文件保存到该目录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "目录示例：https://github.com/owner/repo/tree/main/articles 或 owner/repo/articles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = githubToken,
                onValueChange = viewModel::updateGitHubTokenPreview,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GitHub Token") },
                placeholder = { Text("github_pat_…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = githubDirectory,
                onValueChange = viewModel::updateGitHubDirectoryPreview,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GitHub 目录") },
                placeholder = { Text("https://github.com/owner/repo/tree/main/articles") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = githubBranch,
                onValueChange = viewModel::updateGitHubBranchPreview,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分支（目录 URL 未包含分支时使用）") },
                placeholder = { Text("main") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::saveGitHubSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存 GitHub 设置")
            }
        }
    }
}
