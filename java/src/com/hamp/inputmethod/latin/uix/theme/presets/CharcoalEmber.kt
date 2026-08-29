package com.hamp.inputmethod.latin.uix.theme.presets

import androidx.compose.ui.graphics.Color
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.extendedDarkColorScheme
import com.hamp.inputmethod.latin.uix.extendedLightColorScheme
import com.hamp.inputmethod.latin.uix.theme.ThemeOption

/**
 * "Charcoal & Ember" design system.
 *
 * Dark-first palette defined as semantic OKLCH tokens in the Hamp Keyboard web prototype,
 * converted to sRGB using the CSS Color 4 OKLab->sRGB transform. The hue family is a cool,
 * slightly blue-shifted charcoal (hue 258 in OKLCH) with a soft light-blue accent
 * (hue 245) — "ember" refers to the restrained warm/cool accent usage, not an orange theme.
 *
 * Token mapping (web token -> Compose slot):
 *   background        -> background / surface base
 *   surface           -> surface / surfaceContainer (grouped cards)
 *   surface-raised    -> surfaceContainerHigh / surfaceContainerHighest
 *   primary           -> primary (soft blue #92C9F8)
 *   primary-soft      -> primaryContainer (primary at 15% alpha over background)
 *   muted-foreground  -> onSurfaceVariant
 *   hairline          -> outlineVariant (10% white-blue overlay)
 *   border            -> outline
 *   success (teal)    -> tertiary
 *   destructive       -> error
 */

// ---------- Dark scheme (the canonical Charcoal & Ember look) ----------
private val charcoalDark = extendedDarkColorScheme(
    primary                    = Color(0xFF92C9F8),   // oklch(0.815 0.088 245)
    onPrimary                  = Color(0xFF0E141D),   // oklch(0.19 0.02 258)
    primaryContainer           = Color(0xFF2A3646),   // primary @ ~15% composited over background
    onPrimaryContainer         = Color(0xFFC9E4FA),
    secondary                  = Color(0xFF272C32),   // oklch(0.29 0.014 258)
    onSecondary                = Color(0xFFF5F7F9),
    secondaryContainer         = Color(0xFF22272F),   // surface-raised
    onSecondaryContainer       = Color(0xFF9DA4AC),
    tertiary                   = Color(0xFF5FCEB3),   // oklch(0.78 0.11 175) "success" teal
    onTertiary                 = Color(0xFF0E141D),
    tertiaryContainer          = Color(0xFF1E3B34),
    onTertiaryContainer        = Color(0xFFB8EADD),
    error                      = Color(0xFFE64343),   // oklch(0.62 0.20 25)
    onError                    = Color(0xFFFFF0F0),
    errorContainer             = Color(0xFF4A1F1F),
    onErrorContainer           = Color(0xFFFFD4D4),
    outline                    = Color(0xFF30363E),   // oklch(0.33 0.016 258) "border"
    outlineVariant             = Color(0xFF3A4149),   // hairline: subtle blue-white divider
    surface                    = Color(0xFF191E24),   // oklch(0.232 0.014 258)
    onSurface                  = Color(0xFFF5F7F9),   // oklch(0.975 0.004 250)
    onSurfaceVariant           = Color(0xFF9DA4AC),   // oklch(0.715 0.014 252) "muted-foreground"
    surfaceContainerHighest    = Color(0xFF22272F),   // surface-raised
    shadow                     = Color(0xFF000000).copy(alpha = 0.9f),
    keyboardSurface            = Color(0xFF191E24),
    keyboardSurfaceDim         = Color(0xFF0F1318),   // background — darkest tone anchors the keys
    keyboardContainer          = Color(0xFF272C32),
    keyboardContainerVariant   = Color(0xFF22272F),
    onKeyboardContainer        = Color(0xFFF5F7F9),
    keyboardPress              = Color(0xFF35404E),
    keyboardFade0              = Color(0xFF0F1318),
    keyboardFade1              = Color(0xFF0F1318),
    primaryTransparent         = Color(0xFF92C9F8).copy(alpha = 0.15f),  // "primary-soft"
    onSurfaceTransparent       = Color(0xFFF5F7F9).copy(alpha = 0.1f)
)

// ---------- Light variant: same hues, lightness flipped ----------
private val charcoalLight = extendedLightColorScheme(
    primary                    = Color(0xFF075A8E),   // oklch(0.45 0.11 245)
    onPrimary                  = Color(0xFFFFFFFF),
    primaryContainer           = Color(0xFFD3E7F7),
    onPrimaryContainer         = Color(0xFF0B2A40),
    secondary                  = Color(0xFFD9DEE6),   // derived raised tone
    onSecondary                = Color(0xFF151B24),
    secondaryContainer         = Color(0xFFE7EBF2),
    onSecondaryContainer       = Color(0xFF4D5660),
    tertiary                   = Color(0xFF177A5F),   // success teal, darkened for contrast
    onTertiary                 = Color(0xFFFFFFFF),
    tertiaryContainer          = Color(0xFFCFF0E4),
    onTertiaryContainer        = Color(0xFF0D3328),
    error                      = Color(0xFFB32222),
    onError                    = Color(0xFFFFFFFF),
    errorContainer             = Color(0xFFFFDADA),
    onErrorContainer           = Color(0xFF5C1616),
    outline                    = Color(0xFFB4BBC4),
    outlineVariant             = Color(0xFFC9CFD8),
    surface                    = Color(0xFFF2F5FB),   // oklch(0.97 0.008 258) derived
    onSurface                  = Color(0xFF151B24),   // oklch(0.22 0.02 258) derived
    onSurfaceVariant           = Color(0xFF4D5660),   // oklch(0.45 0.02 252) derived
    surfaceContainerHighest    = Color(0xFFE0E5ED),
    shadow                     = Color(0xFF000000).copy(alpha = 0.25f),
    keyboardSurface            = Color(0xFFEDF1F7),
    keyboardSurfaceDim         = Color(0xFFE2E7EF),
    keyboardContainer          = Color(0xFFFFFFFF),
    keyboardContainerVariant   = Color(0xFFF5F7FA),
    onKeyboardContainer        = Color(0xFF151B24),
    keyboardPress              = Color(0xFFD4DBE4),
    keyboardFade0              = Color(0xFFF2F5FB),
    keyboardFade1              = Color(0xFFF2F5FB),
    primaryTransparent         = Color(0xFF075A8E).copy(alpha = 0.15f),
    onSurfaceTransparent       = Color(0xFF151B24).copy(alpha = 0.1f)
)

val CharcoalEmberDark = ThemeOption(
    dynamic = false,
    key = "CharcoalEmberDark",
    name = R.string.theme_charcoal_ember_dark,
    available = { true }
) {
    charcoalDark
}

val CharcoalEmberLight = ThemeOption(
    dynamic = false,
    key = "CharcoalEmberLight",
    name = R.string.theme_charcoal_ember_light,
    available = { true }
) {
    charcoalLight
}
