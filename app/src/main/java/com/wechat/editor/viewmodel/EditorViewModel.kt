package com.wechat.editor.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wechat.editor.model.Article
import com.wechat.editor.model.ArticleTemplate
import com.wechat.editor.model.ColorPickerTarget
import com.wechat.editor.model.EditorState
import com.wechat.editor.model.EditorTab
import com.wechat.editor.model.LayoutSettings
import com.wechat.editor.model.QuoteStyle
import com.wechat.editor.model.TextAlignment
import com.wechat.editor.model.TextStyle
import com.wechat.editor.utils.AppSettings
import com.wechat.editor.utils.HelloImgApi
import com.wechat.editor.utils.HtmlGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    val appSettings = AppSettings(application)

    private val _article = MutableStateFlow(Article())
    val article: StateFlow<Article> = _article.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _titleValue = MutableStateFlow(TextFieldValue(""))
    val titleValue: StateFlow<TextFieldValue> = _titleValue.asStateFlow()

    private val _contentValue = MutableStateFlow(TextFieldValue(""))
    val contentValue: StateFlow<TextFieldValue> = _contentValue.asStateFlow()

    private val _authorValue = MutableStateFlow(TextFieldValue(""))
    val authorValue: StateFlow<TextFieldValue> = _authorValue.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _layoutSettings = MutableStateFlow(LayoutSettings())
    val layoutSettings: StateFlow<LayoutSettings> = _layoutSettings.asStateFlow()

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()

    fun loadArticle(article: Article) {
        _article.value = article
        _titleValue.value = TextFieldValue(article.title)
        _contentValue.value = TextFieldValue(article.content)
        _authorValue.value = TextFieldValue(article.author)
        _layoutSettings.value = article.layoutSettings
    }

    fun loadNewArticle() {
        val newArticle = Article()
        _article.value = newArticle
        _titleValue.value = TextFieldValue("")
        _contentValue.value = TextFieldValue("")
        _authorValue.value = TextFieldValue("")
        _layoutSettings.value = LayoutSettings()
        undoStack.clear()
        redoStack.clear()
    }

    fun updateTitle(value: TextFieldValue) {
        _titleValue.value = value
        _article.update { it.copy(title = value.text) }
    }

    fun updateContent(value: TextFieldValue) {
        if (value.text != _contentValue.value.text) {
            undoStack.addLast(_contentValue.value)
            if (undoStack.size > 50) undoStack.removeFirst()
            redoStack.clear()
        }
        _contentValue.value = value
        _article.update {
            it.copy(
                content = value.text,
                htmlContent = HtmlGenerator.generateHtml(value.text, _layoutSettings.value, it.title, it.author)
            )
        }
        _editorState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                wordCount = value.text.length
            )
        }
    }

    fun updateAuthor(value: TextFieldValue) {
        _authorValue.value = value
        _article.update { it.copy(author = value.text) }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(_contentValue.value)
            val previous = undoStack.removeLast()
            _contentValue.value = previous
            _article.update { it.copy(content = previous.text) }
            _editorState.update {
                it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(_contentValue.value)
            val next = redoStack.removeLast()
            _contentValue.value = next
            _article.update { it.copy(content = next.text) }
            _editorState.update {
                it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
            }
        }
    }

    fun insertMarkdown(prefix: String, suffix: String = "") {
        val current = _contentValue.value
        val selection = current.selection
        val selectedText = current.text.substring(selection.start, selection.end)
        val newText = buildString {
            append(current.text.substring(0, selection.start))
            append(prefix)
            append(selectedText)
            append(suffix)
            append(current.text.substring(selection.end))
        }
        val newCursorPos = if (selectedText.isEmpty()) {
            selection.start + prefix.length
        } else {
            selection.start + prefix.length + selectedText.length + suffix.length
        }
        updateContent(TextFieldValue(newText, TextRange(newCursorPos)))
    }

    fun insertHeading(level: Int) {
        val current = _contentValue.value
        val text = current.text
        val cursorPos = current.selection.start
        val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
        val lineEnd = text.indexOf('\n', cursorPos).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)
        val prefix = "#".repeat(level) + " "
        val cleanedLine = currentLine.trimStart('#').trimStart(' ')
        val newLine = prefix + cleanedLine
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        updateContent(TextFieldValue(newText, TextRange(lineStart + newLine.length)))
    }

    fun insertBulletList() {
        insertMarkdown("\n- ")
    }

    fun insertOrderedList() {
        insertMarkdown("\n1. ")
    }

    fun insertHorizontalRule() {
        val current = _contentValue.value
        val pos = current.selection.start
        val newText = current.text.substring(0, pos) + "\n\n---\n\n" + current.text.substring(pos)
        updateContent(TextFieldValue(newText, TextRange(pos + 7)))
    }

    fun insertQuote() {
        insertMarkdown("\n> ", "")
    }

    fun insertCodeBlock() {
        insertMarkdown("\n```\n", "\n```\n")
    }

    fun insertLink(text: String, url: String) {
        val linkMarkdown = "[$text]($url)"
        val current = _contentValue.value
        val pos = current.selection.start
        val newText = current.text.substring(0, pos) + linkMarkdown + current.text.substring(pos)
        updateContent(TextFieldValue(newText, TextRange(pos + linkMarkdown.length)))
        dismissLinkDialog()
    }

    fun insertImageMarkdown(altText: String, url: String) {
        val imgMarkdown = "\n![${altText}](${url})\n"
        val current = _contentValue.value
        val pos = current.selection.start
        val newText = current.text.substring(0, pos) + imgMarkdown + current.text.substring(pos)
        updateContent(TextFieldValue(newText, TextRange(pos + imgMarkdown.length)))
        dismissImageDialog()
    }

    fun toggleBold() = insertMarkdown("**", "**")
    fun toggleItalic() = insertMarkdown("*", "*")
    fun toggleStrikethrough() = insertMarkdown("~~", "~~")
    fun toggleUnderline() = insertMarkdown("<u>", "</u>")

    fun applyFontSize(size: Int) {
        insertMarkdown("<span style=\"font-size:${size}px\">", "</span>")
        dismissFontSizePicker()
    }

    fun applyTextColor(color: String) {
        insertMarkdown("<span style=\"color:${color}\">", "</span>")
        dismissColorPicker()
    }

    fun applyBackgroundColor(color: String) {
        insertMarkdown("<span style=\"background-color:${color}\">", "</span>")
        dismissColorPicker()
    }

    fun applyAlignment(alignment: TextAlignment) {
        val current = _contentValue.value
        val text = current.text
        val cursorPos = current.selection.start
        val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
        val lineEnd = text.indexOf('\n', cursorPos).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)
        val wrappedLine = "<p style=\"text-align:${alignment.cssValue}\">$currentLine</p>"
        val newText = text.substring(0, lineStart) + wrappedLine + text.substring(lineEnd)
        updateContent(TextFieldValue(newText, TextRange(lineStart + wrappedLine.length)))
    }

    fun applyTemplate(template: ArticleTemplate) {
        _article.update { it.copy(template = template) }
        _layoutSettings.update { getLayoutForTemplate(template) }
        dismissTemplateDialog()
        _snackbarMessage.value = "已应用模板：${template.displayName}"
    }

    private fun getLayoutForTemplate(template: ArticleTemplate): LayoutSettings {
        return when (template) {
            ArticleTemplate.DEFAULT -> LayoutSettings()
            ArticleTemplate.ELEGANT -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 2.0f,
                primaryColor = "#C0392B",
                textColor = "#2C3E50",
                h1Size = 24,
                h2Size = 20,
                quoteStyle = QuoteStyle.ITALIC
            )
            ArticleTemplate.TECH -> LayoutSettings(
                baseFontSize = 15,
                lineHeight = 1.6f,
                primaryColor = "#2980B9",
                textColor = "#2C3E50",
                codeStyle = com.wechat.editor.model.CodeStyle.GITHUB
            )
            ArticleTemplate.BUSINESS -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.8f,
                primaryColor = "#1A252F",
                textColor = "#2C3E50",
                subtitleColor = "#555555",
                paragraphSpacing = 16
            )
            ArticleTemplate.LIFE -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.9f,
                primaryColor = "#E74C3C",
                textColor = "#333333",
                backgroundColor = "#FFFEF9"
            )
            ArticleTemplate.EDUCATION -> LayoutSettings(
                baseFontSize = 16,
                lineHeight = 1.8f,
                primaryColor = "#27AE60",
                textColor = "#2C3E50",
                quoteStyle = QuoteStyle.BACKGROUND
            )
        }
    }

    fun updateLayoutSettings(settings: LayoutSettings) {
        _layoutSettings.value = settings
        _article.update { it.copy(layoutSettings = settings) }
    }

    fun getHtmlContent(): String {
        return HtmlGenerator.generateHtml(
            _contentValue.value.text,
            _layoutSettings.value,
            _article.value.title,
            _article.value.author
        )
    }

    fun saveArticle(): Article {
        val saved = _article.value.copy(
            title = _titleValue.value.text,
            content = _contentValue.value.text,
            author = _authorValue.value.text,
            layoutSettings = _layoutSettings.value,
            htmlContent = getHtmlContent(),
            updatedAt = System.currentTimeMillis()
        )
        _article.value = saved
        _snackbarMessage.value = "文章已保存"
        return saved
    }

    fun togglePreviewMode() {
        _editorState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun selectEditorTab(tab: EditorTab) {
        _editorState.update { it.copy(selectedTab = tab) }
    }

    fun showColorPicker(target: ColorPickerTarget) {
        _editorState.update { it.copy(showColorPicker = true, colorPickerTarget = target) }
    }

    fun dismissColorPicker() {
        _editorState.update { it.copy(showColorPicker = false) }
    }

    fun showFontSizePicker() {
        _editorState.update { it.copy(showFontSizePicker = true) }
    }

    fun dismissFontSizePicker() {
        _editorState.update { it.copy(showFontSizePicker = false) }
    }

    fun showLinkDialog() {
        _editorState.update { it.copy(showLinkDialog = true) }
    }

    fun dismissLinkDialog() {
        _editorState.update { it.copy(showLinkDialog = false) }
    }

    fun showImageDialog() {
        _editorState.update { it.copy(showImageDialog = true) }
    }

    fun dismissImageDialog() {
        _editorState.update { it.copy(showImageDialog = false) }
    }

    fun showTemplateDialog() {
        _editorState.update { it.copy(showTemplateDialog = true) }
    }

    fun dismissTemplateDialog() {
        _editorState.update { it.copy(showTemplateDialog = false) }
    }

    fun showHelloImgSettings() {
        _editorState.update { it.copy(showHelloImgSettings = true) }
    }

    fun dismissHelloImgSettings() {
        _editorState.update { it.copy(showHelloImgSettings = false) }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ── Hello图床 upload ──────────────────────────────────────────────────────

    /**
     * Upload an image selected from the gallery (via [uri]) to Hello图床 and
     * insert the resulting Markdown image link into the editor content.
     */
    fun uploadImageFromUri(uri: Uri, altText: String = "图片") {
        viewModelScope.launch {
            _editorState.update { it.copy(isUploadingImage = true, uploadProgress = "正在上传图片…") }
            try {
                val cr: ContentResolver = getApplication<Application>().contentResolver
                val mimeType = cr.getType(uri) ?: "image/jpeg"
                val ext = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType) ?: "jpg"
                val filename = "image_${System.currentTimeMillis()}.$ext"
                val bytes = cr.openInputStream(uri)?.readBytes()
                    ?: run {
                        _snackbarMessage.value = "无法读取图片文件"
                        _editorState.update { it.copy(isUploadingImage = false, uploadProgress = "") }
                        return@launch
                    }

                val token = appSettings.helloImgToken
                val strategyId = appSettings.helloImgStrategyId.takeIf { it > 0 }
                val albumId = appSettings.helloImgAlbumId.takeIf { it > 0 }

                when (val result = HelloImgApi.uploadImage(token, bytes, filename, mimeType, strategyId, albumId)) {
                    is HelloImgApi.Result.Success -> {
                        insertImageMarkdown(altText, result.data.url)
                        _snackbarMessage.value = "图片上传成功"
                    }
                    is HelloImgApi.Result.Error -> {
                        val hint = if (token.isBlank()) "（请在设置中配置您的 Hello图床 Token）" else ""
                        _snackbarMessage.value = "上传失败：${result.message}$hint"
                        _editorState.update { it.copy(isUploadingImage = false, uploadProgress = "") }
                    }
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "上传出错：${e.localizedMessage}"
            } finally {
                _editorState.update { it.copy(isUploadingImage = false, uploadProgress = "") }
            }
        }
    }
}
