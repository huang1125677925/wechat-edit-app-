package com.wechat.editor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wechat.editor.utils.DeepSeekChatApi
import com.wechat.editor.viewmodel.EditorViewModel

@Composable
fun DeepSeekAiLayoutDialog(
    viewModel: EditorViewModel,
    initialApiKey: String,
    initialModel: String,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel) }
    var extra by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(initialApiKey, initialModel) {
        apiKey = initialApiKey
        model = initialModel
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("DeepSeek V4 辅助排版") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "在下方填写 DeepSeek 开放平台 API Key（仅保存在本机）。模型请选择官方 V4 系列。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    enabled = !isLoading
                )

                Text(
                    text = "模型",
                    style = MaterialTheme.typography.labelLarge
                )
                Column(Modifier.selectableGroup()) {
                    ModelOptionRow(
                        label = "DeepSeek V4 Flash（推荐，性价比高）",
                        selected = model == DeepSeekChatApi.MODEL_V4_FLASH,
                        enabled = !isLoading,
                        onClick = { model = DeepSeekChatApi.MODEL_V4_FLASH }
                    )
                    ModelOptionRow(
                        label = "DeepSeek V4 Pro（更强推理）",
                        selected = model == DeepSeekChatApi.MODEL_V4_PRO,
                        enabled = !isLoading,
                        onClick = { model = DeepSeekChatApi.MODEL_V4_PRO }
                    )
                }

                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("排版说明（可选）") },
                    placeholder = { Text("例如：拆成小标题、加粗重点、适合公众号阅读") },
                    minLines = 2,
                    maxLines = 5,
                    enabled = !isLoading
                )

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                strokeWidth = 2.dp
                            )
                            Text("正在调用 DeepSeek…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.runDeepSeekLayoutAssist(
                        apiKey = apiKey,
                        model = model,
                        userNotes = extra
                    )
                },
                enabled = !isLoading && apiKey.isNotBlank()
            ) {
                Text("开始 AI 排版")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ModelOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
