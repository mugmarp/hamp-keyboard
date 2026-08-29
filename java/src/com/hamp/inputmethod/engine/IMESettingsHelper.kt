package com.hamp.inputmethod.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hamp.inputmethod.engine.general.ChineseIMESettings
import com.hamp.inputmethod.engine.general.JapaneseIMESettings
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.Subtypes
import com.hamp.inputmethod.latin.SubtypesSetting
import com.hamp.inputmethod.latin.uix.settings.NavigationItemStyle
import com.hamp.inputmethod.latin.uix.settings.UserSettingsMenu
import com.hamp.inputmethod.latin.uix.settings.useDataStoreValue
import com.hamp.inputmethod.latin.uix.settings.userSettingNavigationItem

@Composable
private fun isVisible(language: String): Boolean {
    val subtypeSet = useDataStoreValue(SubtypesSetting)
    return remember(subtypeSet) {
        subtypeSet.any {
            Subtypes.getLocale(Subtypes.convertToSubtype(it).locale).language == language
        }
    }
}

val SettingsByLanguage = mapOf(
    "zh" to ChineseIMESettings.menu.copy(visibilityCheck = { isVisible("zh") }),
    "ja" to JapaneseIMESettings.menu.copy(visibilityCheck = { isVisible("ja") })
)

@Composable
private fun anyVisible(): Boolean {
    val subtypeSet = useDataStoreValue(SubtypesSetting)
    return remember(subtypeSet) {
        subtypeSet.any {
            SettingsByLanguage.containsKey(Subtypes.getLocale(Subtypes.convertToSubtype(it).locale).language)
        }
    }
}

private val IMESettings = buildList {
    SettingsByLanguage.forEach {
        add(
            userSettingNavigationItem(
                title = it.value.title,
                style = NavigationItemStyle.HomePrimary,
                icon = R.drawable.globe,
                navigateTo = it.value.navPath,
            ).copy(
                visibilityCheck = it.value.visibilityCheck,
                appearInSearchIfVisibilityCheckFailed = false
            )
        )
    }
}

val IMESettingsMenu = UserSettingsMenu(
    title = R.string.language_specific_settings_title,
    navPath = "ime", registerNavPath = true,
    settings = IMESettings, visibilityCheck = { anyVisible() }
)