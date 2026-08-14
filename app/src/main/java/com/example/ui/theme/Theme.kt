package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = EmeraldDarkPrimary,
        onPrimary = SlateDarkBackground,
        primaryContainer = EmeraldDarkContainer,
        onPrimaryContainer = EmeraldContainer,
        secondary = GoldAccent,
        background = SlateDarkBackground,
        surface = SlateDarkSurface,
        onBackground = SlateDarkTextPrimary,
        onSurface = SlateDarkTextPrimary,
        onSurfaceVariant = SlateDarkTextSecondary
    )

private val LightColorScheme =
    lightColorScheme(
        primary = EmeraldPrimary,
        onPrimary = EmeraldOnPrimary,
        primaryContainer = EmeraldContainer,
        onPrimaryContainer = EmeraldOnContainer,
        secondary = GoldAccent,
        secondaryContainer = GoldContainer,
        onSecondaryContainer = GoldOnContainer,
        background = SlateBackground,
        surface = SlateSurface,
        onBackground = SlateTextPrimary,
        onSurface = SlateTextPrimary,
        onSurfaceVariant = SlateTextSecondary
    )

@Composable
fun CashFlowTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    CashFlowTheme(darkTheme = darkTheme, content = content)
}

