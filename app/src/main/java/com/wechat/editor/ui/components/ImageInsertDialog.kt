package com.wechat.editor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog for inserting images. Provides two tabs:
 *  1. URL – paste an external/Hello图床 URL directly.
 *  2. 上传 – pick a local image and upload to Hello图床.
 */
@Composable
fun ImageInsertDialog(
    isUploading: Boolean,
    onInsertUrl: (altText: String, url: String) -> Unit,
    onPickAndUpload: (altText: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("URL 插入", "上传图片")

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("插入图片", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontSize = 13.sp) }
                        )
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> UrlInsertTab(onInsertUrl)
                    1 -> UploadTab(isUploading, onPickAndUpload, onOpenSettings)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) { Text("取消") }
        }
    )
}

@Composable
private fun UrlInsertTab(onInsert: (altText: String, url: String) -> Unit) {
    var altText by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    Column {
        OutlinedTextField(
            value = altText,
            onValueChange = { altText = it },
            label = { Text("图片描述（Alt）") },
            placeholder = { Text("可选，留空则使用"图片"") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("图片 URL *") },
            placeholder = { Text("https://www.helloimg.com/…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.InsertLink, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                val finalAlt = altText.ifBlank { "图片" }
                onInsert(finalAlt, url)
            },
            enabled = url.isNotBlank() && url != "https://",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("插入")
        }
    }
}

@Composable
private fun UploadTab(
    isUploading: Boolean,
    onPickAndUpload: (altText: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var altText by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = altText,
            onValueChange = { altText = it },
            label = { Text("图片描述（Alt）") },
            placeholder = { Text("可选，留空则使用"图片"") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (isUploading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("正在上传，请稍候…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        } else {
            Button(
                onClick = { onPickAndUpload(altText.ifBlank { "图片" }) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("选择图片并上传到图床")
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("配置 Hello图床 Token", fontSize = 13.sp)
        }
    }
}
