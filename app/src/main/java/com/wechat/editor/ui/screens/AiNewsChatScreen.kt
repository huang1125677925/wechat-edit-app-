package com.wechat.editor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.ChatMessage
import com.wechat.editor.model.ChatRole
import com.wechat.editor.viewmodel.AiNewsChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiNewsChatScreen(
    viewModel: AiNewsChatViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) {
            listState.animateScrollToItem(ui.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 资讯咨询", fontWeight = FontWeight.Bold)
                        ui.feedInfo?.let { info ->
                            Text(
                                text = "已加载 ${info.totalItems} 条 · ${if (info.windowHours >= 168) "近 7 天" else "近 24 小时"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (ui.messages.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearChat) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "清空对话",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                input = ui.inputText,
                onInputChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                enabled = ui.feedLoaded && !ui.isReplying,
                isReplying = ui.isReplying
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!ui.feedLoaded) {
                FeedLoadPanel(
                    windowHours = ui.windowHours,
                    isLoading = ui.isLoadingFeed,
                    error = ui.feedError,
                    onSelect24h = { viewModel.setWindowHours(24) },
                    onSelect7d = { viewModel.setWindowHours(168) },
                    onLoad = viewModel::loadFeed
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ui.messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                    if (ui.isReplying) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedLoadPanel(
    windowHours: Int,
    isLoading: Boolean,
    error: String?,
    onSelect24h: () -> Unit,
    onSelect7d: () -> Unit,
    onLoad: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AI 资讯咨询",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "加载最新 AI 科技资讯后，你可以向 AI 提问，获取资讯解读与动向分析。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = windowHours == 24,
                onClick = onSelect24h,
                label = { Text("近 24 小时") }
            )
            FilterChip(
                selected = windowHours == 168,
                onClick = onSelect7d,
                label = { Text("近 7 天") }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLoad,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text("拉取资讯数据")
        }

        error?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "加载失败：$err",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = when {
                        message.isError -> MaterialTheme.colorScheme.onErrorContainer
                        isUser -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    lineHeight = 22.sp
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp, end = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "正在思考…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    isReplying: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (!enabled && !isReplying) "请先拉取资讯数据" else "向 AI 提问资讯相关问题…",
                        fontSize = 14.sp
                    )
                },
                enabled = enabled,
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && input.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled && input.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (enabled && input.isNotBlank())
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
