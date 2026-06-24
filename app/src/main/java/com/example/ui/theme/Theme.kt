package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// §738 — user-selectable theme. Persisted in prefs (key `theme_mode`) and exposed
// by the ViewModel as a StateFlow; MainActivity feeds it into MyApplicationTheme.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// ─────────────────────────────────────────────────────────────────────────────
// Semantic palette (§738) — the SURFACE/TEXT roles that flip between light & dark.
//
// Brand accents (VedaDropRose teal / VedaDropGold amber) are NOT here: they are
// identical in both themes and referenced directly across the screens. The brand
// HEADER bar deliberately stays dark teal in BOTH modes (the signature look), so
// `headerGradient` is the same in both palettes and any Color.White sitting on a
// header/brand surface stays Color.White.
//
// Screens read the active palette via `LocalVedaDropPalette.current`. Every Material
// component already pulls from MaterialTheme.colorScheme (which also flips), so this
// palette only covers the custom-drawn surfaces/text that bypass colorScheme.
// ─────────────────────────────────────────────────────────────────────────────
data class VedaDropPalette(
  val isDark: Boolean,
  val screenBg: Color,      // full-page / scaffold body background
  val surface: Color,       // cards, sheets, elevated panels
  val surfaceAlt: Color,    // secondary/tonal surface (chips, muted rows)
  val textPrimary: Color,   // primary body text/icon on screenBg/surface
  val textSecondary: Color, // muted/secondary body text
  val divider: Color,       // hairlines / outlines on body
  val inputBg: Color,       // text-field / search backgrounds
  val headerGradient: List<Color>, // brand bar — DARK TEAL in both modes
)

val VedaDropDarkPalette = VedaDropPalette(
  isDark = true,
  screenBg = DarkSlate,                 // #081218
  surface = DeepPlum,                   // #11212B
  surfaceAlt = Color(0xFF1D3542),
  textPrimary = Color(0xFFF0F5F4),
  textSecondary = Color(0xFF94A9B3),
  divider = Color(0xFF284657),
  inputBg = Color(0xFF1D3542),
  headerGradient = listOf(DeepPlum, DarkSlate),
)

val VedaDropLightPalette = VedaDropPalette(
  isDark = false,
  screenBg = SoftCream,                 // #F6FAF9
  surface = Color.White,
  surfaceAlt = LightSage,               // #E8F4F1
  textPrimary = Color(0xFF081218),
  textSecondary = Color(0xFF536A75),
  divider = Color(0xFFCFDFDB),
  inputBg = Color.White,
  headerGradient = listOf(Color(0xFF008B8E), Color(0xFF00AAAD)), // Brand Teal gradient header in light mode
)

val LocalVedaDropPalette = staticCompositionLocalOf { VedaDropDarkPalette }

// §738 — @Composable getters so any composable can read a theme-aware role WITHOUT
// declaring `val palette = LocalVedaDropPalette.current` first. This lets the
// light/dark migration replace a raw token (e.g. DarkSlate, Color.White-as-body-text)
// with a single drop-in identifier anywhere inside a @Composable, with no extra
// plumbing. Brand accents (VedaDropRose/VedaDropGold) are NOT here — they stay raw.
val vedaScreenBg: Color
  @Composable get() = LocalVedaDropPalette.current.screenBg
val vedaSurface: Color
  @Composable get() = LocalVedaDropPalette.current.surface
val vedaSurfaceAlt: Color
  @Composable get() = LocalVedaDropPalette.current.surfaceAlt
val vedaTextPrimary: Color
  @Composable get() = LocalVedaDropPalette.current.textPrimary
val vedaTextSecondary: Color
  @Composable get() = LocalVedaDropPalette.current.textSecondary
val vedaDivider: Color
  @Composable get() = LocalVedaDropPalette.current.divider
val vedaInputBg: Color
  @Composable get() = LocalVedaDropPalette.current.inputBg

// Veda Drop "Ocean Teal": designed around the #00AAAD brand color
// with modern deep teal darks and crisp minty white light surfaces.
// All on-colour pairings WCAG-AA verified.
private val DarkColorScheme =
  darkColorScheme(
    primary = AccentBronze,            // Lighter Teal for AA on dark
    onPrimary = Color(0xFF003839),     // dark teal — AA on light teal
    primaryContainer = Color(0xFF005254),
    onPrimaryContainer = Color(0xFF99E6E7),
    secondary = Color(0xFF94A9B3),     // slate/teal 400
    onSecondary = Color(0xFF081218),
    tertiary = VedaDropGold,             // gold accent
    background = DarkSlate,
    onBackground = Color(0xFFF0F5F4),
    surface = DeepPlum,
    onSurface = Color(0xFFF0F5F4),
    surfaceVariant = Color(0xFF1D3542),
    onSurfaceVariant = Color(0xFF94A9B3),
    outline = Color(0xFF284657),
    error = Color(0xFFFFB4AB),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VedaDropRose,              // #00AAAD — white text
    onPrimary = Color.White,
    primaryContainer = RoseSoft,       // Soft teal blush
    onPrimaryContainer = Color(0xFF003839),
    secondary = PlumDeepInk,           // Dark teal text
    onSecondary = Color.White,
    tertiary = AccentBronze,
    background = SoftCream,            // Minty white
    onBackground = Color(0xFF081218),
    surface = Color.White,
    onSurface = Color(0xFF081218),
    surfaceVariant = LightSage,        // Very light teal
    onSurfaceVariant = Color(0xFF536A75),
    outline = Color(0xFFCFDFDB),
    error = Color(0xFFBA1A1A),
  )

@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val dark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }
  val colorScheme = if (dark) DarkColorScheme else LightColorScheme
  val palette = if (dark) VedaDropDarkPalette else VedaDropLightPalette

  CompositionLocalProvider(LocalVedaDropPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = Shapes,
      content = content
    )
  }
}
