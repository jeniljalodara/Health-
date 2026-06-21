package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PalePastelGreen,
    onPrimary = AccentForest,
    primaryContainer = PrimarySage,
    onPrimaryContainer = PalePastelGreen,
    secondary = LightSageGray,
    onSecondary = AccentForest,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkOnSurface,
    tertiary = SoftAshGreen,
    onTertiary = AccentForest,
    tertiaryContainer = Color(0xFF191C19),
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimarySage,
    onPrimary = Color.White,
    primaryContainer = PalePastelGreen,
    onPrimaryContainer = AccentForest,
    secondary = SecondaryOliveCharcoal,
    onSecondary = Color.White,
    secondaryContainer = LightSageGray,
    onSecondaryContainer = AccentForest,
    tertiary = AccentForest,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF191C19),
    onTertiaryContainer = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    surfaceVariant = SoftAshGreen,
    onSurfaceVariant = SecondaryOliveCharcoal
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamicColor by default to preserve the gorgeous specific Emerald brand feel!
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
