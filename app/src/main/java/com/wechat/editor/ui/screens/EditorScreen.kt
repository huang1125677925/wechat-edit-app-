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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.ColorPickerTarget
import com.wechat.editor.ui.components.ArticleTemplatePreviewSection
import com.wechat.editor.ui.components.CodeSnippetDialog
import com.wechat.editor.ui.components.ColorPickerDialog
import com.wechat.editor.ui.components.FormatToolbar
import com.wechat.editor.ui.components.HeadingStyleDialog
import com.wechat.editor.ui.components.ImageInsertDialog
import com.wechat.editor.ui.components.LinkDialog
import com.wechat.editor.ui.components.ParagraphSettingsDialog
import com.wechat.editor.utils.ClipboardUtils
import com.wechat.editor.viewmodel.EditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    val layoutSettings by viewModel.layoutSettings.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    val editorScrollState = rememberScrollState()
    var editorViewportHeightPx by remember { mutableStateOf(0) }
    var contentFieldTopPx by remember { mutableStateOf(0f) }
    var contentTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorScrollMarginPx = with(density) { 80.dp.toPx() }
    val refocusContentEditor: () -> Unit = remember(contentFocusRequester, keyboardController) {
        {
            contentFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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

    LaunchedEffect(
        contentValue.selection.start,
        contentValue.selection.end,
        contentTextLayout,
        editorViewportHeightPx
    ) {
        val layout = contentTextLayout ?: return@LaunchedEffect
        if (editorViewportHeightPx <= 0) return@LaunchedEffect

        val cursorOffset = contentValue.selection.end.coerceIn(0, contentValue.text.length)
        val cursorRect = layout.getCursorRect(cursorOffset)
        val fieldTopInContent = contentFieldTopPx + editorScrollState.value
        val cursorTop = fieldTopInContent + cursorRect.top
        val cursorBottom = fieldTopInContent + cursorRect.bottom
        val visibleTop = editorScrollState.value.toFloat()
        val visibleBottom = visibleTop + editorViewportHeightPx

        val target = when {
            cursorBottom + cursorScrollMarginPx > visibleBottom ->
                (cursorBottom + cursorScrollMarginPx - editorViewportHeightPx).roundToInt()
            cursorTop - cursorScrollMarginPx < visibleTop ->
                (cursorTop - cursorScrollMarginPx).roundToInt()
            else -> null
        } ?: return@LaunchedEffect

        editorScrollState.animateScrollTo(target.coerceIn(0, editorScrollState.maxValue))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (titleValue.text.isBlank()) "新建文章" else titleValue.text,
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                        val savedAt = editorState.lastAutoSavedAtEpochMs
                        if (savedAt != null) {
                            val timeStr = remember(savedAt) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(savedAt))
                            }
                            Text(
                                text = "已自动保存 $timeStr",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
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
                    IconButton(
                        onClick = {
                            viewModel.undo()
                            refocusContentEditor()
                        },
                        enabled = editorState.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销")
                    }
                    IconButton(
                        onClick = {
                            viewModel.redo()
                            refocusContentEditor()
                        },
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
                        val html = viewModel.getWeChatPasteHtml()
                        ClipboardUtils.copyHtmlToClipboard(context, html, "WeChat Article HTML")
                        viewModel.showSnackbar("已复制，可直接粘贴到公众号编辑器")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制HTML")
                    }
                    IconButton(onClick = {
                        val saved = viewModel.saveArticle()
                        onSave(saved)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    IconButton(
                        onClick = {
                            val saved = viewModel.saveArticle(showSnackbar = false)
                            onSave(saved)
                            viewModel.saveArticleToGitHub(saved)
                        },
                        enabled = !editorState.isSavingToGitHub
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = if (editorState.isSavingToGitHub) {
                                "正在保存到GitHub"
                            } else {
                                "保存到GitHub"
                            }
                        )
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
                        .onSizeChanged { editorViewportHeightPx = it.height }
                        .verticalScroll(editorScrollState)
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

                    ArticleTemplatePreviewSection(
                        articleId = article.id,
                        appliedTemplate = article.template,
                        previewHtml = viewModel::previewHtmlForTemplate,
                        onSettledTemplate = { t -> viewModel.applyTemplate(t, showSnackbar = false) }
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
                            Box {
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        "开始编写您的文章内容...\n\n支持Markdown语法：\n• **粗体** *斜体* ~~删除线~~\n• # 标题1  ## 标题2  ### 标题3\n• - 无序列表  1. 有序列表\n• > 引用块\n• | 表头 | 表头 | 管道表格\n• ```代码块```\n• [链接文字](URL)\n• ![图片描述](图片URL)",
                                        fontSize = 16.sp,
                                        lineHeight = 28.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        onTextLayout = { contentTextLayout = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .focusRequester(contentFocusRequester)
                            .onGloballyPositioned { coordinates ->
                                contentFieldTopPx = coordinates.positionInParent().y
                            }
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
                    isDeepSeekBusy = editorState.isDeepSeekPolishing,
                    onContentEdit = refocusContentEditor,
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
                refocusContentEditor()
            },
            onDismiss = viewModel::dismissColorPicker
        )
    }

    if (editorState.showFontSizePicker) {
        FontSizePickerDialog(
            onSizeSelected = { size ->
                viewModel.applyFontSize(size)
                refocusContentEditor()
            },
            onDismiss = viewModel::dismissFontSizePicker
        )
    }

    if (editorState.showLinkDialog) {
        LinkDialog(
            onConfirm = { text, url ->
                viewModel.insertLink(text, url)
                refocusContentEditor()
            },
            onDismiss = viewModel::dismissLinkDialog
        )
    }

    if (editorState.showImageDialog) {
        ImageInsertDialog(
            isUploading = editorState.isUploadingImage,
            onInsertUrl = { alt, url ->
                viewModel.insertImageMarkdown(alt, url)
                refocusContentEditor()
            },
            onPickAndUpload = { alt ->
                pendingImageAlt = alt
                imagePickerLauncher.launch("image/*")
            },
            onDismiss = viewModel::dismissImageDialog
        )
    }

    if (editorState.showCodeSnippetDialog) {
        CodeSnippetDialog(
            onInsert = { language ->
                viewModel.insertCodeFence(language)
                refocusContentEditor()
            },
            onDismiss = viewModel::dismissCodeSnippetDialog
        )
    }

    if (editorState.showHeadingStyleDialog) {
        HeadingStyleDialog(
            level = editorState.headingStyleLevel,
            layout = layoutSettings,
            onApplyH1 = viewModel::applyH1Style,
            onApplyH2 = viewModel::applyH2Style,
            onApplyH3 = viewModel::applyH3Style,
            onDismiss = viewModel::dismissHeadingStyleDialog
        )
    }

    if (editorState.showParagraphSettingsDialog) {
        ParagraphSettingsDialog(
            layout = layoutSettings,
            onApplyLineHeight = viewModel::applyLineHeight,
            onApplyParagraphSpacing = viewModel::applyParagraphSpacing,
            onToggleFirstLineIndent = viewModel::toggleFirstLineIndent,
            onDismiss = viewModel::dismissParagraphSettingsDialog
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
