package com.wechat.editor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = ColorOnPrimary,
    primaryContainer = WeChatGreenLight,
    onPrimaryContainer = WeChatGreenDark,
    secondary = WeChatGreenDark,
    onSecondary = ColorOnPrimary,
    background = ColorBackground,
    surface = CardLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = EditorBorderColor,
    surfaceVariant = EditorToolbarBg
)

private val DarkColorScheme = darkColorScheme(
    primary = WeChatGreen,
    onPrimary = ColorOnPrimary,
    primaryContainer = WeChatGreenDark,
    onPrimaryContainer = WeChatGreenLight,
    background = TextPrimary,
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
)

@Composable
fun WeChatEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
