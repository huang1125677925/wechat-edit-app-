package com.wechat.editor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.AiNewsItem
import com.wechat.editor.model.Article
import com.wechat.editor.viewmodel.AiNewsDigestViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiNewsDigestScreen(
    viewModel: AiNewsDigestViewModel,
    onBack: () -> Unit,
    onDigestReady: (Article) -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar by viewModel.snackbar.collectAsState()

    LaunchedEffect(snackbar) {
        if (snackbar != null) {
            delay(3500)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 资讯摘要", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "接入开源数据「ai-news-aggregator」（GitHub Pages JSON），拉取标题列表后用 DeepSeek 生成公众号风格 Markdown 汇总。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = ui.windowHours == 24,
                    onClick = viewModel::setWindow24h,
                    label = { Text("24 小时") }
                )
                FilterChip(
                    selected = ui.windowHours == 168,
                    onClick = viewModel::setWindow7d,
                    label = { Text("近 7 天") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "送入模型的条目数量（越多越全，耗时与 tokens 越高）：${ui.maxItemsForModel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Slider(
                value = ui.maxItemsForModel.toFloat(),
                onValueChange = { viewModel.setMaxForModel(it.toInt()) },
                valueRange = 20f..800f,
                steps = 155
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::loadFeed,
                    enabled = !ui.isLoadingFeed,
                    modifier = Modifier.weight(1f)
                ) {
                    if (ui.isLoadingFeed) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text("拉取数据")
                }
                Button(
                    onClick = { viewModel.generateDigest(onDigestReady) },
                    enabled = !ui.isGenerating && ui.items.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (ui.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text("生成汇总")
                }
            }

            ui.feedMeta?.let { meta ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "数据：约 ${meta.totalItems} 条 · 窗口 ${meta.windowHours}h · 生成 ${meta.generatedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ui.feedError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "拉取失败：$err",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ui.generateError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "生成失败：$err",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            snackbar?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "预览（前 30 条）",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.items.take(30)) { item ->
                    HeadlinePreviewCard(item)
                }
                if (ui.items.size > 30) {
                    item {
                        Text(
                            text = "… 共 ${ui.items.size} 条，生成时将取前 ${ui.maxItemsForModel} 条送入模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadlinePreviewCard(item: AiNewsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.displayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.siteName} · ${item.source}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
