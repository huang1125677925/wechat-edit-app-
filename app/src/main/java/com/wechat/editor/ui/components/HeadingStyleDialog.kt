package com.wechat.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.H1Style
import com.wechat.editor.model.H2Style
import com.wechat.editor.model.H3Style
import com.wechat.editor.model.LayoutSettings

@Composable
fun HeadingStyleDialog(
    level: Int,
    layout: LayoutSettings,
    onApplyH1: (H1Style) -> Unit,
    onApplyH2: (H2Style) -> Unit,
    onApplyH3: (H3Style) -> Unit,
    onDismiss: () -> Unit
) {
    when (level) {
        1 -> H1StyleDialog(layout = layout, onApply = onApplyH1, onDismiss = onDismiss)
        2 -> H2StyleDialog(layout = layout, onApply = onApplyH2, onDismiss = onDismiss)
        else -> H3StyleDialog(layout = layout, onApply = onApplyH3, onDismiss = onDismiss)
    }
}

@Composable
private fun H1StyleDialog(
    layout: LayoutSettings,
    onApply: (H1Style) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(layout.h1Style) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("H1 标题样式") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "选择一级标题的展示风格",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                H1Style.entries.forEach { style ->
                    HeadingStyleItem(
                        name = style.displayName,
                        description = style.description,
                        isSelected = style == selected,
                        preview = { H1StylePreview(style = style, primaryColor = layout.primaryColor) },
                        onClick = { selected = style }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(selected) }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun H2StyleDialog(
    layout: LayoutSettings,
    onApply: (H2Style) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(layout.h2Style) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("H2 标题样式") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "选择二级标题的展示风格",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                H2Style.entries.forEach { style ->
                    HeadingStyleItem(
                        name = style.displayName,
                        description = style.description,
                        isSelected = style == selected,
                        preview = { H2StylePreview(style = style, primaryColor = layout.primaryColor) },
                        onClick = { selected = style }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(selected) }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun H3StyleDialog(
    layout: LayoutSettings,
    onApply: (H3Style) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(layout.h3Style) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("H3 标题样式") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "选择三级标题的展示风格",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                H3Style.entries.forEach { style ->
                    HeadingStyleItem(
                        name = style.displayName,
                        description = style.description,
                        isSelected = style == selected,
                        preview = { H3StylePreview(style = style, primaryColor = layout.primaryColor) },
                        onClick = { selected = style }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(selected) }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun HeadingStyleItem(
    name: String,
    description: String,
    isSelected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                preview()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun H1StylePreview(style: H1Style, primaryColor: String) {
    val pc = parsePreviewColor(primaryColor)
    when (style) {
        H1Style.UNDERLINE_BORDER -> Box(
            modifier = Modifier
                .background(Color.Transparent)
                .border(width = 0.dp, color = Color.Transparent)
                .padding(bottom = 2.dp)
        ) {
            Text(
                text = "标题",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = pc,
                modifier = Modifier
                    .border(width = 1.5.dp, color = pc, shape = RoundedCornerShape(0.dp))
                    .padding(bottom = 2.dp)
            )
        }
        H1Style.BACKGROUND_BLOCK -> Box(
            modifier = Modifier
                .background(pc, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        H1Style.LEFT_ACCENT -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(16.dp).background(pc))
            Spacer(modifier = Modifier.width(4.dp))
            Text("标题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pc)
        }
        H1Style.CENTERED_LINE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.width(32.dp).height(1.dp).background(pc))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Box(modifier = Modifier.width(32.dp).height(1.dp).background(pc))
        }
        H1Style.PLAIN_BOLD -> Text("标题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun H2StylePreview(style: H2Style, primaryColor: String) {
    val pc = parsePreviewColor(primaryColor)
    when (style) {
        H2Style.LEFT_BORDER -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(14.dp).background(pc))
            Spacer(modifier = Modifier.width(4.dp))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pc)
        }
        H2Style.DOT_PREFIX -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("●", fontSize = 8.sp, color = pc)
            Spacer(modifier = Modifier.width(3.dp))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pc)
        }
        H2Style.UNDERLINE -> Text(
            text = "标题",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = TextDecoration.Underline
        )
        H2Style.BACKGROUND_LIGHT -> Box(
            modifier = Modifier
                .background(pc.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pc)
        }
        H2Style.PLAIN_COLOR -> Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pc)
    }
}

@Composable
private fun H3StylePreview(style: H3Style, primaryColor: String) {
    val pc = parsePreviewColor(primaryColor)
    when (style) {
        H3Style.THIN_LEFT_BORDER -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(2.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        H3Style.ARROW_PREFIX -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▶", fontSize = 9.sp, color = pc)
            Spacer(modifier = Modifier.width(3.dp))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        H3Style.BOLD_SUBTITLE -> Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        H3Style.ITALIC_COLOR -> Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pc)
        H3Style.CIRCLE_BULLET -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("○", fontSize = 10.sp, color = pc)
            Spacer(modifier = Modifier.width(3.dp))
            Text("标题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun parsePreviewColor(hex: String): Color {
    return try {
        val cleaned = hex.trimStart('#')
        val argb = when (cleaned.length) {
            6 -> 0xFF000000.toInt() or cleaned.toLong(16).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> 0xFF1AAD19.toInt()
        }
        Color(argb)
    } catch (e: Exception) {
        Color(0xFF1AAD19.toInt())
    }
}
