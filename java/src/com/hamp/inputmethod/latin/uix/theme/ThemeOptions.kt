package com.hamp.inputmethod.latin.uix.theme

import android.content.Context
import androidx.annotation.StringRes
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.KeyboardColorScheme
import com.hamp.inputmethod.latin.uix.actions.BugInfo
import com.hamp.inputmethod.latin.uix.actions.BugViewerState
import com.hamp.inputmethod.latin.uix.theme.presets.AMOLEDDarkPurple
import com.hamp.inputmethod.latin.uix.theme.presets.CatppuccinMocha
import com.hamp.inputmethod.latin.uix.theme.presets.CharcoalEmberDark
import com.hamp.inputmethod.latin.uix.theme.presets.CharcoalEmberLight
import com.hamp.inputmethod.latin.uix.theme.presets.ClassicMaterialDark
import com.hamp.inputmethod.latin.uix.theme.presets.ClassicMaterialLight
import com.hamp.inputmethod.latin.uix.theme.presets.CottonCandy
import com.hamp.inputmethod.latin.uix.theme.presets.DeepSeaDark
import com.hamp.inputmethod.latin.uix.theme.presets.DeepSeaLight
import com.hamp.inputmethod.latin.uix.theme.presets.DefaultDarkScheme
import com.hamp.inputmethod.latin.uix.theme.presets.DefaultLightScheme
import com.hamp.inputmethod.latin.uix.theme.presets.DynamicDarkTheme
import com.hamp.inputmethod.latin.uix.theme.presets.DynamicLightTheme
import com.hamp.inputmethod.latin.uix.theme.presets.DynamicSystemTheme
import com.hamp.inputmethod.latin.uix.theme.presets.Emerald
import com.hamp.inputmethod.latin.uix.theme.presets.Gradient1
import com.hamp.inputmethod.latin.uix.theme.presets.HotDog
import com.hamp.inputmethod.latin.uix.theme.presets.Snowfall
import com.hamp.inputmethod.latin.uix.theme.presets.SteelGray
import com.hamp.inputmethod.latin.uix.theme.presets.Sunflower
import com.hamp.inputmethod.latin.uix.theme.presets.VoiceInputTheme
import com.hamp.inputmethod.latin.uix.theme.presets.DevTheme
import com.hamp.inputmethod.latin.uix.theme.presets.HighContrastYellow

data class ThemeOption(
    val dynamic: Boolean,
    val key: String,
    @StringRes val name: Int,
    val available: (Context) -> Boolean,
    val obtainColors: (Context) -> KeyboardColorScheme,
)

val ThemeOptions = mapOf(
    CharcoalEmberDark.key to CharcoalEmberDark,
    CharcoalEmberLight.key to CharcoalEmberLight,

    DefaultDarkScheme.key to DefaultDarkScheme,
    DefaultLightScheme.key to DefaultLightScheme,

    DynamicSystemTheme.key to DynamicSystemTheme,
    DynamicDarkTheme.key to DynamicDarkTheme,
    DynamicLightTheme.key to DynamicLightTheme,

    ClassicMaterialDark.key to ClassicMaterialDark,
    ClassicMaterialLight.key to ClassicMaterialLight,
    AMOLEDDarkPurple.key to AMOLEDDarkPurple,

    Sunflower.key to Sunflower,
    Snowfall.key to Snowfall,
    SteelGray.key to SteelGray,
    Emerald.key to Emerald,
    CottonCandy.key to CottonCandy,

    DeepSeaLight.key to DeepSeaLight,
    DeepSeaDark.key to DeepSeaDark,

    Gradient1.key to Gradient1,
    VoiceInputTheme.key to VoiceInputTheme,
    HotDog.key to HotDog,
    DevTheme.key to DevTheme,
    HighContrastYellow.key to HighContrastYellow,
    CatppuccinMocha.key to CatppuccinMocha,
)

val ThemeOptionKeys = ThemeOptions.keys

fun defaultThemeOption(context: Context): ThemeOption =
    if(context.resources.getBoolean(R.bool.use_dev_styling)) {
        DevTheme
    } else {
        // Hamp Keyboard default: the Charcoal & Ember dark palette (design-system default).
        if(DynamicSystemTheme.available(context)) {
            CharcoalEmberDark
        } else {
            CharcoalEmberDark
        }
    }

fun getThemeOption(context: Context, key: String): ThemeOption? {
    return ThemeOptions[key] ?: run {
        return ZipThemes.ThemeFileName.fromSetting(key)?.let { name ->
            ThemeOption(
                dynamic = false,
                key = key,
                name = 0,
                available = { true },
                obtainColors = {
                    try {
                        ZipThemes.loadScheme(context, name)
                    } catch(e: Exception) {
                        BugViewerState.pushBug(BugInfo(
                            name = "Theme $name",
                            details = e.toString(),
                        ))
                        defaultThemeOption(context).obtainColors(it)
                    }
                }
            )
        }
    }
}

fun ThemeOption?.orDefault(context: Context): ThemeOption {
    val themeOptionFromSettings = this
    val themeOption = when {
        themeOptionFromSettings == null -> defaultThemeOption(context)
        !themeOptionFromSettings.available(context) -> defaultThemeOption(context)
        else -> themeOptionFromSettings
    }

    return themeOption
}