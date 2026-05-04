package com.wechat.editor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.ColorPickerTarget
import com.wechat.editor.ui.components.ColorPickerDialog
import com.wechat.editor.ui.components.FormatToolbar
import com.wechat.editor.ui.components.HelloImgSettingsDialog
import com.wechat.editor.ui.components.ImageInsertDialog
import com.wechat.editor.ui.components.LinkDialog
import com.wechat.editor.ui.components.TemplateDialog
import com.wechat.editor.utils.ClipboardUtils
import com.wechat.editor.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onSave: (com.wechat.editor.model.Article) -> Unit,
    onBack: () -> Unit
) {
    val article by viewModel.article.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val titleValue by viewModel.titleValue.collectAsState()
    val contentValue by viewModel.contentValue.collectAsState()
    val authorValue by viewModel.authorValue.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Pending alt-text while waiting for the user to pick an image from gallery
    var pendingImageAlt by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadImageFromUri(uri, pendingImageAlt)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (titleValue.text.isBlank()) "新建文章" else titleValue.text,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = editorState.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = editorState.canRedo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做")
                    }
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            imageVector = Icons.Default.Preview,
                            contentDescription = if (editorState.isPreviewMode) "编辑" else "预览"
                        )
                    }
                    IconButton(onClick = {
                        val html = viewModel.getHtmlContent()
                        ClipboardUtils.copyToClipboard(context, html, "WeChat Article HTML")
                        viewModel.clearSnackbar()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制HTML")
                    }
                    IconButton(onClick = {
                        val saved = viewModel.saveArticle()
                        onSave(saved)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (editorState.isPreviewMode) {
                PreviewPanel(
                    htmlContent = viewModel.getHtmlContent(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Author field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "作者：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        BasicTextField(
                            value = authorValue,
                            onValueChange = viewModel::updateAuthor,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (authorValue.text.isEmpty()) {
                                        Text(
                                            "请输入作者名...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    // Title field
                    BasicTextField(
                        value = titleValue,
                        onValueChange = viewModel::updateTitle,
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                if (titleValue.text.isEmpty()) {
                                    Text(
                                        "请输入文章标题...",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    // Content field
                    BasicTextField(
                        value = contentValue,
                        onValueChange = viewModel::updateContent,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        "开始编写您的文章内容...\n\n支持Markdown语法：\n• **粗体** *斜体* ~~删除线~~\n• # 标题1  ## 标题2  ### 标题3\n• - 无序列表  1. 有序列表\n• > 引用块\n• ```代码块```\n• [链接文字](URL)\n• ![图片描述](图片URL)",
                                        fontSize = 16.sp,
                                        lineHeight = 28.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Word count indicator
                Text(
                    text = "${editorState.wordCount} 字",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Format toolbar
                FormatToolbar(
                    viewModel = viewModel,
                    selectedTab = editorState.selectedTab,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (editorState.showColorPicker) {
        val title = when (editorState.colorPickerTarget) {
            ColorPickerTarget.TEXT -> "选择文字颜色"
            ColorPickerTarget.BACKGROUND -> "选择背景颜色"
            ColorPickerTarget.HIGHLIGHT -> "选择高亮颜色"
        }
        ColorPickerDialog(
            title = title,
            onColorSelected = { color ->
                when (editorState.colorPickerTarget) {
                    ColorPickerTarget.TEXT -> viewModel.applyTextColor(color)
                    ColorPickerTarget.BACKGROUND -> viewModel.applyBackgroundColor(color)
                    ColorPickerTarget.HIGHLIGHT -> viewModel.applyTextColor(color)
                }
            },
            onDismiss = viewModel::dismissColorPicker
        )
    }

    if (editorState.showFontSizePicker) {
        FontSizePickerDialog(
            onSizeSelected = viewModel::applyFontSize,
            onDismiss = viewModel::dismissFontSizePicker
        )
    }

    if (editorState.showLinkDialog) {
        LinkDialog(
            onConfirm = viewModel::insertLink,
            onDismiss = viewModel::dismissLinkDialog
        )
    }

    if (editorState.showImageDialog) {
        ImageInsertDialog(
            isUploading = editorState.isUploadingImage,
            onInsertUrl = { alt, url -> viewModel.insertImageMarkdown(alt, url) },
            onPickAndUpload = { alt ->
                pendingImageAlt = alt
                imagePickerLauncher.launch("image/*")
            },
            onOpenSettings = {
                viewModel.dismissImageDialog()
                viewModel.showHelloImgSettings()
            },
            onDismiss = viewModel::dismissImageDialog
        )
    }

    if (editorState.showHelloImgSettings) {
        HelloImgSettingsDialog(
            currentToken = viewModel.appSettings.helloImgToken,
            currentStrategyId = viewModel.appSettings.helloImgStrategyId,
            currentAlbumId = viewModel.appSettings.helloImgAlbumId,
            onSave = { token, strategyId, albumId ->
                viewModel.appSettings.helloImgToken = token
                viewModel.appSettings.helloImgStrategyId = strategyId
                viewModel.appSettings.helloImgAlbumId = albumId
                viewModel.dismissHelloImgSettings()
                viewModel.showImageDialog()
            },
            onDismiss = viewModel::dismissHelloImgSettings
        )
    }

    if (editorState.showTemplateDialog) {
        TemplateDialog(
            currentTemplate = article.template,
            onTemplateSelected = viewModel::applyTemplate,
            onDismiss = viewModel::dismissTemplateDialog
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontSizePickerDialog(
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = listOf(12, 14, 15, 16, 18, 20, 22, 24, 28, 32, 36)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择字号") },
        text = {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                sizes.forEach { size ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .clickable { onSizeSelected(size) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "${size}px",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
