package com.wechat.editor.model

data class EditorState(
    val currentTextStyle: TextStyle = TextStyle(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val wordCount: Int = 0,
    val isPreviewMode: Boolean = false,
    val showFormatToolbar: Boolean = true,
    val selectedTab: EditorTab = EditorTab.FORMAT,
    val showColorPicker: Boolean = false,
    val colorPickerTarget: ColorPickerTarget = ColorPickerTarget.TEXT,
    val showFontSizePicker: Boolean = false,
    val showLineHeightPicker: Boolean = false,
    val showHeadingStyleDialog: Boolean = false,
    val headingStyleLevel: Int = 1,
    val showParagraphSettingsDialog: Boolean = false,
    val showLinkDialog: Boolean = false,
    val showImageDialog: Boolean = false,
    val showCodeSnippetDialog: Boolean = false,
    val isUploadingImage: Boolean = false,
    val uploadProgress: String = "",
    val isDeepSeekPolishing: Boolean = false,
    /** Wall-clock time of the last successful background draft persist, or null if none yet. */
    val lastAutoSavedAtEpochMs: Long? = null
)

enum class EditorTab {
    FORMAT, PARAGRAPH, INSERT
}

enum class ColorPickerTarget {
    TEXT, BACKGROUND, HIGHLIGHT
}
