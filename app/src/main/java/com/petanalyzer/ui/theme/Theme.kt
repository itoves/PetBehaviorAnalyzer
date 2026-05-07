package com.petanalyzer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PinkDark,
    onPrimary = CardBackground,
    primaryContainer = PinkLight,
    onPrimaryContainer = PinkDark,
    secondary = MintGreenDark,
    onSecondary = CardBackground,
    secondaryContainer = MintGreenLight,
    onSecondaryContainer = MintGreenDark,
    tertiary = Lavender,
    onTertiary = CardBackground,
    tertiaryContainer = LavenderLight,
    background = BackgroundStart,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Cream,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = BehaviorBubbleBorder,
)

@Composable
fun PetAnalyzerTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
