package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ─────────────────────────────────────────────────────────────────────────────
// Veda Drop — "Teal" palette (§715).
//
// Founder theme change: the SINGLE brand colour is now TEAL #009688 (Material
// Teal 500), replacing the §694 Google-Blue. This file is the ONE source of
// truth — these named vals are consumed ~360× across the screen code and wired
// into Material via Theme.kt, so a future re-theme is a single edit here (+ the
// Vue token in tailwind.config.cjs). The value NAMES are kept FROZEN (VedaDropRose
// / DeepPlum / AccentBronze etc. — historically misleading but stable) so not a
// single screen reference needs to change; only the hex shifts. Every Material
// on-colour pairing is preserved: `primary`(teal) keeps white text, dark-mode
// primary lifts to a light teal with dark text. White-on-#009688 ≈ 3.67:1
// (AA-large / bold button labels — the standard Material teal-button look).
// ─────────────────────────────────────────────────────────────────────────────

val VedaDropRose = Color(0xFF0DA19C)     // DOMINANT brand teal — primary (Teal 500); white text
val VedaDropGold = Color(0xFFF9AB00)     // amber accent (rating stars / premium badge only)
val DeepPlum = Color(0xFF0B2E2A)     // deep teal surface (dark)
val DarkSlate = Color(0xFF06211E)    // near-black teal background (dark)
val SoftCream = Color(0xFFF2FBFA)    // teal-tinted near-white app background (light)
val AccentBronze = Color(0xFF4DB6AC) // soft teal tertiary / dark-mode primary (Teal 300)
val LightSage = Color(0xFFE0F2F1)    // muted teal surface variant (Teal 50)
val SuccessGreen = Color(0xFF1E8E3E) // AA-safe success on white (kept non-brand)
val OrderOrange = Color(0xFFE37400)  // AA-safe warning/order amber (kept non-brand)

// Light-scheme brand anchors.
val PlumDeepInk = Color(0xFF00695C)  // deep teal for light-mode secondary/brand text (Teal 800)
val RoseSoft = Color(0xFFB2DFDB)     // teal tint for chips / tonal containers (Teal 100)

// Bold Typography Theme Colors (M3 SPEC) — retained for any references.
val BoldBg = Color(0xFFFEF7FF)
val BoldText = Color(0xFF1D1B20)
val BoldPurple = Color(0xFF6750A4)
val BoldLilac = Color(0xFFEADDFF)
val BoldCardBg = Color(0xFFF7F2FA)
val BoldBorder = Color(0xFFCAC4D0)
val BoldDarkPurple = Color(0xFF21005D)
val BoldBlue = Color(0xFFD3E3FD)
