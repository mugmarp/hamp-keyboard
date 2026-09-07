package com.hamp.inputmethod.latin.uix.settings.pages

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hamp.inputmethod.latin.BuildConfig
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.SettingsExporter
import com.hamp.inputmethod.latin.uix.settings.HampInfoBanner
import com.hamp.inputmethod.latin.uix.settings.NavigationItem
import com.hamp.inputmethod.latin.uix.settings.NavigationItemStyle
import com.hamp.inputmethod.latin.uix.settings.ScreenTitle
import com.hamp.inputmethod.latin.uix.settings.UserSetting
import com.hamp.inputmethod.latin.uix.settings.UserSettingsMenu
import com.hamp.inputmethod.latin.uix.settings.userSettingDecorationOnly
import com.hamp.inputmethod.latin.uix.settings.userSettingNavigationItem
import com.hamp.inputmethod.latin.uix.settings.copyToClipboard
import com.hamp.inputmethod.updates.openURI
import androidx.compose.ui.platform.LocalContext

val MiscMenu = UserSettingsMenu(
    title = R.string.misc_settings_title,
    navPath = "misc", registerNavPath = true,
    settings = listOf(
        userSettingDecorationOnly {
            ScreenTitle(stringResource(R.string.settings_export_configuration_title))
        },

        userSettingNavigationItem(
            title = (R.string.settings_export_configuration),
            subtitle = (R.string.settings_export_configuration_subtitle),
            style = NavigationItemStyle.Misc,
            navigateTo = "exportingcfg"
        ).copy(searchTags = R.string.settings_import_export_tags),
        userSettingNavigationItem(
            title = (R.string.settings_import_configuration),
            subtitle = (R.string.settings_import_configuration_subtitle),
            style = NavigationItemStyle.Misc,
            navigate = { nav ->
                SettingsExporter.triggerImportSettings(nav.context)
            }
        ).copy(searchTags = R.string.settings_import_export_tags),

        // Build info: an informational row that shows version + flavor and copies
        // them to clipboard on tap. Helps users file useful bug reports. Pattern
        // adapted from Help.kt's version row.
        UserSetting(
            name = R.string.misc_settings_build_info_title,
            subtitle = R.string.misc_settings_build_info_subtitle,
            searchTagList = listOf(R.string.misc_settings_build_info_title),
        ) {
            val context = LocalContext.current
            val infoText = stringResource(
                R.string.misc_settings_build_info_value,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.FLAVOR
            )
            NavigationItem(
                title = infoText,
                style = NavigationItemStyle.MiscNoArrow,
                navigate = { context.copyToClipboard(infoText) }
            )
        },

        // Open source licenses: navigate to the existing Credits page.
        userSettingNavigationItem(
            title = R.string.misc_settings_open_source_licenses,
            subtitle = R.string.misc_settings_open_source_licenses_subtitle,
            navigateTo = "credits",
            style = NavigationItemStyle.Misc,
        ).copy(searchTags = R.string.help_menu_website_tags),

        // Project repo: external link to the GitHub repo.
        userSettingNavigationItem(
            title = R.string.misc_settings_project_repo,
            subtitle = R.string.misc_settings_project_repo_subtitle,
            style = NavigationItemStyle.ExternalLink,
            navigate = { nav -> nav.context.openURI("https://github.com/mugmarp/hamp-keyboard") }
        ),

        // Footer: a HampInfoBanner that fills the bottom of the page and acts as
        // a graceful "end of list" marker. Without it the screen was 65% empty
        // (verified on a 1640px-tall screen: content ended at y=542, dead zone
        // to y=1611 was 1069px of empty background).
        userSettingDecorationOnly {
            HampInfoBanner(
                title = stringResource(R.string.misc_settings_about_title),
                description = stringResource(R.string.misc_settings_about_description),
                icon = painterResource(R.drawable.help_circle),
            )
        },
    )
)