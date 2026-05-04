package com.wechat.editor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog for configuring the Hello图床 API token (and optional strategy/album ids).
 *
 * @param currentToken   Pre-filled token value (from [AppSettings])
 * @param currentStrategyId  -1 means "not set"
 * @param currentAlbumId     -1 means "not set"
 * @param onSave    Called with (token, strategyId, albumId) when user confirms
 * @param onDismiss Called when dialog is cancelled
 */
@Composable
fun HelloImgSettingsDialog(
    currentToken: String,
    currentStrategyId: Int,
    currentAlbumId: Int,
    onSave: (token: String, strategyId: Int, albumId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(currentToken) }
    var strategyIdText by remember {
        mutableStateOf(if (currentStrategyId > 0) currentStrategyId.toString() else "")
    }
    var albumIdText by remember {
        mutableStateOf(if (currentAlbumId > 0) currentAlbumId.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Hello图床 设置") },
        text = {
            Column {
                Text(
                    text = "在 helloimg.com 个人中心获取 API Token，" +
                            "然后填入下方。未填 Token 则以游客身份上传。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("API Token") },
                    placeholder = { Text("1|1bJbwlqBf…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "可选配置",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = strategyIdText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) strategyIdText = it },
                    label = { Text("储存策略 ID（可选）") },
                    placeholder = { Text("留空则使用默认策略") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = albumIdText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) albumIdText = it },
                    label = { Text("相册 ID（可选）") },
                    placeholder = { Text("留空则不绑定相册") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sid = strategyIdText.toIntOrNull() ?: -1
                    val aid = albumIdText.toIntOrNull() ?: -1
                    onSave(token.trim(), sid, aid)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
