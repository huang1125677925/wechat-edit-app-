package com.wechat.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.ColorPickerTarget
import com.wechat.editor.model.EditorTab
import com.wechat.editor.model.TextAlignment
import com.wechat.editor.viewmodel.EditorViewModel

@Composable
fun FormatToolbar(
    viewModel: EditorViewModel,
    selectedTab: EditorTab,
    isDeepSeekBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp
        ) {
            val tabs = listOf("格式", "段落", "插入", "模板")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab.ordinal == index,
                    onClick = { viewModel.selectEditorTab(EditorTab.entries[index]) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab.ordinal == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            when (selectedTab) {
                EditorTab.FORMAT -> FormatTab(viewModel)
                EditorTab.PARAGRAPH -> ParagraphTab(viewModel)
                EditorTab.INSERT -> InsertTab(viewModel, isDeepSeekBusy)
                EditorTab.TEMPLATE -> TemplateTab(viewModel)
            }
        }
    }
}

@Composable
private fun FormatTab(viewModel: EditorViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: inline text formatting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(icon = Icons.Default.FormatBold, label = "粗体") { viewModel.toggleBold() }
            ToolbarIconButton(icon = Icons.Default.FormatItalic, label = "斜体") { viewModel.toggleItalic() }
            ToolbarIconButton(icon = Icons.Default.FormatUnderlined, label = "下划线") { viewModel.toggleUnderline() }
            ToolbarIconButton(icon = Icons.Default.FormatStrikethrough, label = "删除线") { viewModel.toggleStrikethrough() }
            ToolbarDivider()
            ToolbarIconButton(icon = Icons.Default.FormatSize, label = "字号") { viewModel.showFontSizePicker() }
            ToolbarIconButton(icon = Icons.Default.ColorLens, label = "文字颜色") {
                viewModel.showColorPicker(ColorPickerTarget.TEXT)
            }
            ToolbarIconButton(icon = Icons.Default.Palette, label = "背景颜色") {
                viewModel.showColorPicker(ColorPickerTarget.BACKGROUND)
            }
        }

        androidx.compose.material3.HorizontalDivider(
            color = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Row 2: heading insert + style picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeadingButtonWithStyle(label = "H1", onInsert = { viewModel.insertHeading(1) }, onStyle = { viewModel.showHeadingStyleDialog(1) })
            HeadingButtonWithStyle(label = "H2", onInsert = { viewModel.insertHeading(2) }, onStyle = { viewModel.showHeadingStyleDialog(2) })
            HeadingButtonWithStyle(label = "H3", onInsert = { viewModel.insertHeading(3) }, onStyle = { viewModel.showHeadingStyleDialog(3) })
            ToolbarTextButton(text = "H4") { viewModel.insertHeading(4) }
            ToolbarTextButton(text = "H5") { viewModel.insertHeading(5) }
            ToolbarTextButton(text = "H6") { viewModel.insertHeading(6) }
        }
    }
}

@Composable
private fun ParagraphTab(viewModel: EditorViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: alignment + list + block elements
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(icon = Icons.AutoMirrored.Filled.FormatAlignLeft, label = "左对齐") {
                viewModel.applyAlignment(TextAlignment.LEFT)
            }
            ToolbarIconButton(icon = Icons.Default.FormatAlignCenter, label = "居中") {
                viewModel.applyAlignment(TextAlignment.CENTER)
            }
            ToolbarIconButton(icon = Icons.AutoMirrored.Filled.FormatAlignRight, label = "右对齐") {
                viewModel.applyAlignment(TextAlignment.RIGHT)
            }
            ToolbarDivider()
            ToolbarIconButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, label = "无序列表") {
                viewModel.insertBulletList()
            }
            ToolbarIconButton(icon = Icons.Default.FormatListNumbered, label = "有序列表") {
                viewModel.insertOrderedList()
            }
            ToolbarDivider()
            ToolbarIconButton(icon = Icons.Default.FormatQuote, label = "引用") {
                viewModel.insertQuote()
            }
            ToolbarIconButton(icon = Icons.Default.HorizontalRule, label = "分割线") {
                viewModel.insertHorizontalRule()
            }
        }

        androidx.compose.material3.HorizontalDivider(
            color = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Row 2: paragraph-level settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(icon = Icons.Default.FormatLineSpacing, label = "行间距") {
                viewModel.showParagraphSettingsDialog()
            }
            ToolbarIconButton(icon = Icons.Default.Tune, label = "段落设置") {
                viewModel.showParagraphSettingsDialog()
            }
            ToolbarDivider()
            // Quick line height shortcuts
            listOf(1.5f to "1.5", 1.75f to "1.75", 2.0f to "2.0").forEach { (value, label) ->
                ToolbarTextButton(text = label) { viewModel.applyLineHeight(value) }
            }
        }
    }
}

@Composable
private fun InsertTab(viewModel: EditorViewModel, isDeepSeekBusy: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDeepSeekBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
        } else {
            ToolbarIconButton(icon = Icons.Default.AutoAwesome, label = "DeepSeek 规范化排版") {
                viewModel.polishContentWithDeepSeek()
            }
        }
        ToolbarDivider()
        ToolbarIconButton(icon = Icons.Default.InsertLink, label = "链接") {
            viewModel.showLinkDialog()
        }
        ToolbarIconButton(icon = Icons.Default.Image, label = "图片") {
            viewModel.showImageDialog()
        }
        ToolbarIconButton(icon = Icons.Default.Code, label = "代码块") {
            viewModel.insertCodeBlock()
        }
        ToolbarDivider()
        ToolbarTextButton(text = "阅读原文") {
            viewModel.insertMarkdown("\n[阅读原文](https://mp.weixin.qq.com)\n")
        }
        ToolbarTextButton(text = "关注我们") {
            viewModel.insertMarkdown("\n---\n**关注我们**，获取更多精彩内容。\n")
        }
    }
}

@Composable
private fun TemplateTab(viewModel: EditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(icon = Icons.Default.Style, label = "选择模板") {
            viewModel.showTemplateDialog()
        }
        ToolbarDivider()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "点击选择模板以快速应用排版风格",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val iconColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ToolbarTextButton(
    text: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val textColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

/**
 * A compound heading button: tapping the label inserts the heading markdown,
 * tapping the small dropdown indicator opens the heading style picker.
 */
@Composable
fun HeadingButtonWithStyle(
    label: String,
    onInsert: () -> Unit,
    onStyle: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Insert area
        Box(
            modifier = Modifier
                .clickable(onClick = onInsert)
                .padding(start = 10.dp, end = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Thin separator
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
        // Style selector area (small dropdown indicator)
        Box(
            modifier = Modifier
                .clickable(onClick = onStyle)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▾",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
