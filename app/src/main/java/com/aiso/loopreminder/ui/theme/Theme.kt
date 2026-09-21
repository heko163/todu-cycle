package com.aiso.loopreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    background = Cream,
    surface = Surface,
    onPrimary = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = Success,
    error = Danger
)

@Composable
fun LoopReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
