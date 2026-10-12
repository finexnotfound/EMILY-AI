package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EmilyColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    secondary = NothingGreyLight,
    onSecondary = NothingBlack,
    tertiary = NothingRed,
    onTertiary = NothingWhite,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingDarkSurface,
    onSurface = NothingWhite,
    surfaceVariant = NothingCardBg,
    onSurfaceVariant = NothingGreyLight,
    outline = NothingCardBorder,
    error = StatusError,
    onError = NothingWhite
)

@Composable
fun EmilyAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EmilyColorScheme,
        typography = Typography,
        content = content
    )
}
