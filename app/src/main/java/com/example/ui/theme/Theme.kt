package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalLightColorScheme = lightColorScheme(
    primary = TerminalAccentLight,
    onPrimary = Color.White,
    primaryContainer = TerminalSurfaceAltLight,
    onPrimaryContainer = TerminalTextPrimaryLight,

    secondary = TerminalBuyLight,
    onSecondary = Color.White,
    secondaryContainer = TerminalSurfaceAltLight,
    onSecondaryContainer = TerminalTextPrimaryLight,

    tertiary = TerminalWarningLight,
    onTertiary = Color.White,
    tertiaryContainer = TerminalSurfaceAltLight,
    onTertiaryContainer = TerminalTextPrimaryLight,

    background = TerminalBgLight,
    onBackground = TerminalTextPrimaryLight,

    surface = TerminalBgLight,
    onSurface = TerminalTextPrimaryLight,
    surfaceVariant = TerminalSurfaceAltLight,
    onSurfaceVariant = TerminalTextSecondaryLight,

    outline = TerminalBorderLight,
    outlineVariant = TerminalDividerLight,

    error = TerminalNegativeLight,
    onError = Color.White,
    errorContainer = TerminalSurfaceAltLight,
    onErrorContainer = TerminalNegativeLight
)

private val TerminalDarkColorScheme = darkColorScheme(
    primary = TerminalAccentDark,
    onPrimary = Color.White,
    primaryContainer = TerminalSurfaceAltDark,
    onPrimaryContainer = TerminalTextPrimaryDark,

    secondary = TerminalBuyDark,
    onSecondary = Color.White,
    secondaryContainer = TerminalSurfaceAltDark,
    onSecondaryContainer = TerminalTextPrimaryDark,

    tertiary = TerminalWarningDark,
    onTertiary = Color.White,
    tertiaryContainer = TerminalSurfaceAltDark,
    onTertiaryContainer = TerminalTextPrimaryDark,

    background = TerminalBgDark,
    onBackground = TerminalTextPrimaryDark,

    surface = TerminalBgDark,
    onSurface = TerminalTextPrimaryDark,
    surfaceVariant = TerminalSurfaceAltDark,
    onSurfaceVariant = TerminalTextSecondaryDark,

    outline = TerminalBorderDark,
    outlineVariant = TerminalDividerDark,

    error = TerminalNegativeDark,
    onError = Color.White,
    errorContainer = TerminalSurfaceAltDark,
    onErrorContainer = TerminalNegativeDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TerminalDarkColorScheme else TerminalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
