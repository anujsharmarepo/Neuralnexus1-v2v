package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = AbhayaPrimary,
    secondary = AbhayaSecondary,
    tertiary = AbhayaTertiary,
    background = AbhayaBackground,
    surface = AbhayaSurface,
    onPrimary = AbhayaOnPrimary,
    onSecondary = AbhayaOnSecondary,
    onBackground = AbhayaOnBackground,
    onSurface = AbhayaOnSurface,
    error = AbhayaError,
    onError = AbhayaOnError,
    surfaceVariant = AbhayaSurfaceVariant,
    onSurfaceVariant = AbhayaOnBackground
  )

private val DarkColorScheme = LightColorScheme // Keep consistent brand colors for light-mode-first high-contrast theme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Light mode first energetic branding as requested
  dynamicColor: Boolean = false, // Set to false to preserve the high-contrast brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
