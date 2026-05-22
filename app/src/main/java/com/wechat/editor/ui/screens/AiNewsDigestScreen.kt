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
import androidx.compose.foundation.lazy.LazyRow
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
import com.wechat.editor.viewmodel.AiNewsDigestLimits
import com.wechat.editor.viewmodel.AiNewsDigestViewModel
import com.wechat.editor.viewmodel.AiNewsDigestViewModel.FeedBackend
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
                text = when (ui.feedBackend) {
                    FeedBackend.AI_NEWS_AGGREGATOR ->
                        "接入「ai-news-aggregator」GitHub Pages JSON，拉取标题列表后用 DeepSeek 生成公众号风格 Markdown 汇总。"
                    FeedBackend.PERPS_NEWS ->
                        "接入开源「Perps-news」仓库中的 news.json 快照，按时间窗口筛选标题后用 DeepSeek 生成股市与财经向 Markdown 汇总（上游为定时采集数据，以 JSON 更新时间为准）。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "数据源",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = ui.feedBackend == FeedBackend.AI_NEWS_AGGREGATOR,
                    onClick = viewModel::setBackendAggregator,
                    label = { Text("AI 科技聚合") }
                )
                FilterChip(
                    selected = ui.feedBackend == FeedBackend.PERPS_NEWS,
                    onClick = viewModel::setBackendPerpsNews,
                    label = { Text("股市新闻 Perps") }
                )
            }

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
            val modelInputBounds = AiNewsDigestLimits.modelInputBounds(ui.sendableItemCount)
            if (modelInputBounds != null) {
                val sliderMin = modelInputBounds.first.toFloat()
                val sliderMax = modelInputBounds.last.toFloat()
                Text(
                    text = "送入模型的条目数量（${ui.maxItemsForModel} / ${ui.sendableItemCount}，已选且含有效链接；越多越全，耗时与 tokens 越高）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Slider(
                    value = ui.maxItemsForModel.toFloat().coerceIn(sliderMin, sliderMax),
                    onValueChange = { viewModel.setMaxForModel(it.toInt()) },
                    valueRange = sliderMin..sliderMax,
                    steps = AiNewsDigestLimits.sliderSteps(modelInputBounds),
                    enabled = modelInputBounds.last > modelInputBounds.first
                )
            } else {
                Text(
                    text = "送入模型的条目数量：拉取数据并选择来源后，可按实际条数调整（默认 ${AiNewsDigestLimits.DEFAULT_MAX_ITEMS_FOR_MODEL} 条）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

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
                    text = "数据：约 ${meta.totalItems} 条 · 已选 ${ui.items.size} 条 · 窗口 ${meta.windowHours}h · 生成 ${meta.generatedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (ui.sourceOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SourceFilterSection(
                    options = ui.sourceOptions,
                    selectedKeys = ui.selectedSourceKeys,
                    selectedItemCount = ui.items.size,
                    onToggle = viewModel::toggleSource,
                    onSelectAll = viewModel::selectAllSources,
                    onClear = viewModel::clearSourceSelection
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
private fun SourceFilterSection(
    options: List<AiNewsDigestViewModel.SourceFilterOption>,
    selectedKeys: Set<String>,
    selectedItemCount: Int,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "来源筛选：${selectedKeys.size}/${options.size} 个来源 · $selectedItemCount 条",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onSelectAll,
                    enabled = selectedKeys.size != options.size
                ) {
                    Text("全选")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = selectedKeys.isNotEmpty()
                ) {
                    Text("清空")
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option.key in selectedKeys,
                    onClick = { onToggle(option.key) },
                    label = {
                        Text(
                            text = "${option.label} (${option.count})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
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
