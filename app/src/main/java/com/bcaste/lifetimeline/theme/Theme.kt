package com.bcaste.lifetimeline.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentBlue,
    background = DeepDarkBlue,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary
)

@Composable
fun LifeTimelineTheme(
  darkTheme: Boolean = true, // Force dark theme to match the screenshot
  dynamicColor: Boolean = false, // Disable dynamic color to maintain the look
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Force dark for now

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
