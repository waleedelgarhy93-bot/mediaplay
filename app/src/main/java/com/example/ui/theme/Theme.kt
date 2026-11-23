package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = PulseCoral,
    onPrimary = Color(0xFF1A0703),
    primaryContainer = PulseCoralContainer,
    onPrimaryContainer = PulseCoralLight,
    secondary = PulseCyan,
    onSecondary = Color(0xFF00201A),
    secondaryContainer = PulseCyanContainer,
    onSecondaryContainer = PulseCyan,
    tertiary = PulseAmber,
    onTertiary = Color(0xFF281700),
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = ObsidianSurface,
    onSurface = InkPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    surfaceContainerHighest = ObsidianCardHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = ErrorRose,
    onError = Color.White,
    errorContainer = ErrorRoseContainer,
    onErrorContainer = ErrorRose
)

val PulsePlayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulsePlayTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = ObsidianDarkColorScheme,
        typography = PulsePlayTypography,
        shapes = PulsePlayShapes,
        content = content
    )
}
