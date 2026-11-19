package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = NeonMint,
    onPrimary = Color(0xFF00261C),
    primaryContainer = MintContainerDark,
    onPrimaryContainer = NeonMint,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF00232E),
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = ElectricCyan,
    tertiary = AmberAccent,
    onTertiary = Color(0xFF2C1B00),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    error = CoralError,
    onError = Color(0xFF33000B)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepMint,
    onPrimary = Color.White,
    primaryContainer = MintContainerLight,
    onPrimaryContainer = Color(0xFF002D20),
    secondary = DeepCyan,
    onSecondary = Color.White,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = Color(0xFF002733),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = FrostBg,
    onBackground = TextPrimaryLight,
    surface = FrostSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = FrostSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = FrostSurface,
    surfaceContainer = FrostSurfaceElevated,
    surfaceContainerHigh = FrostSurfaceHighest,
    outline = FrostOutline,
    error = Color(0xFFD12C4B),
    onError = Color.White
)

val QrLensShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

@Composable
fun QrLensTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = QrLensShapes,
        content = content
    )
}
