package com.modukey.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF426523), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF6A5), onPrimaryContainer = Color(0xFF263716),
    background = Color(0xFFF8F9F4), surface = Color(0xFFF8F9F4),
    surfaceContainer = Color(0xFFEEF0E8), surfaceContainerLow = Color(0xFFF1F3EC),
    onSurface = Color(0xFF20251C), onSurfaceVariant = Color(0xFF606756),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB8D982), onPrimary = Color(0xFF20360C),
    primaryContainer = Color(0xFF334827), onPrimaryContainer = Color(0xFFDDF6A5),
    background = Color(0xFF151813), surface = Color(0xFF151813),
    surfaceContainer = Color(0xFF252A21), surfaceContainerLow = Color(0xFF1D2119),
    onSurface = Color(0xFFE7ECDD), onSurfaceVariant = Color(0xFFB4BDA9),
)

@Composable
fun NudgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
