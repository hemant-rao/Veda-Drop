package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Rose is the hero action colour in both schemes (modern, on-brand with the
// web admin). Gold is demoted to a secondary champagne accent. Darks are warm
// charcoal-plum so the app feels premium rather than harsh.
private val DarkColorScheme =
  darkColorScheme(
    primary = GlamRose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A2238),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = GlamGold,
    onSecondary = Color(0xFF2A1E33),
    tertiary = AccentBronze,
    background = DarkSlate,
    onBackground = Color(0xFFF3ECF2),
    surface = DeepPlum,
    onSurface = Color(0xFFF3ECF2),
    surfaceVariant = Color(0xFF362B41),
    onSurfaceVariant = Color(0xFFD3C7D6),
    outline = Color(0xFF6E5F73),
    error = Color(0xFFE5687E),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GlamRose,
    onPrimary = Color.White,
    primaryContainer = RoseSoft,
    onPrimaryContainer = Color(0xFF5A2238),
    secondary = PlumDeepInk,
    onSecondary = Color.White,
    tertiary = AccentBronze,
    background = SoftCream,
    onBackground = Color(0xFF231A28),
    surface = Color.White,
    onSurface = Color(0xFF231A28),
    surfaceVariant = Color(0xFFF1E9EE),
    onSurfaceVariant = Color(0xFF5C5260),
    outline = Color(0xFFD9CEd6),
    error = Color(0xFFC4374F),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic colour is intentionally OFF so GlamGo's brand palette is consistent
  // across devices (Material You would override our rose/plum identity).
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
