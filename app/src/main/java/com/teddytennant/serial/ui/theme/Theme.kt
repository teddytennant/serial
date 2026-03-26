package com.teddytennant.serial.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF999999)
)

private val BlackColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    background = BlackBackground,
    onBackground = BlackOnBackground,
    surface = BlackSurface,
    onSurface = BlackOnSurface,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF999999)
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF8B6914),
    onPrimary = Color.White,
    background = SepiaBackground,
    onBackground = SepiaOnBackground,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    surfaceVariant = Color(0xFFDDCCA8),
    onSurfaceVariant = Color(0xFF6E5A3B)
)

@Composable
fun SerialTheme(
    theme: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        "black" -> BlackColorScheme
        "sepia" -> SepiaColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
