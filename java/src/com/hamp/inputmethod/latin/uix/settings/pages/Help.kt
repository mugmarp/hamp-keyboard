package com.hamp.inputmethod.latin.uix.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.runBlocking
import com.hamp.inputmethod.latin.BuildConfig
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.getSetting
import com.hamp.inputmethod.latin.uix.setSetting
import com.hamp.inputmethod.latin.uix.settings.NavigationItem
import com.hamp.inputmethod.latin.uix.settings.NavigationItemStyle
import com.hamp.inputmethod.latin.uix.settings.Tip
import com.hamp.inputmethod.latin.uix.settings.UserSetting
import com.hamp.inputmethod.latin.uix.settings.UserSettingsMenu
import com.hamp.inputmethod.latin.uix.settings.userSettingDecorationOnly
import com.hamp.inputmethod.latin.uix.settings.userSettingNavigationItem
import com.hamp.inputmethod.updates.openURI

internal fun Context.copyToClipboard(text: CharSequence, label: String = "Copied Text") {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(label, text)
    clipboardManager.setPrimaryClip(clipData)
}

val HelpMenu = UserSettingsMenu(
    title = R.string.help_menu_title,
    navPath = "help", registerNavPath = true,
    settings = listOf(
        UserSetting(
            name = R.string.help_menu_version_name
        ) {
            val context = LocalContext.current
            NavigationItem(
                title = stringResource(R.string.help_menu_version_name, BuildConfig.VERSION_NAME),
                style = NavigationItemStyle.MiscNoArrow,
                navigate = {
                    context.copyToClipboard(BuildConfig.VERSION_NAME)
                }
            )
        },
        UserSetting(
            name = R.string.help_menu_version_code,
            searchTags = R.string.dev_settings_title
        ) {
            val context = LocalContext.current
            val numPresses = remember { mutableIntStateOf(0) }
            var lastToast: MutableState<Toast?> = remember { mutableStateOf(null) }
            NavigationItem(
                title = stringResource(R.string.help_menu_version_code, BuildConfig.VERSION_CODE),
                style = NavigationItemStyle.MiscNoArrow,
                navigate = {
                    val makeToast: (String) -> Unit = { text ->
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).let {
                            lastToast.value?.cancel()
                            lastToast.value = it
                            it.show()
                        }
                    }
                    if(runBlocking { context.getSetting(IS_DEVELOPER) }) {
                        makeToast("You're already a developer")
                    } else {
                        numPresses.intValue += 1
                        if (numPresses.intValue > 3) {
                            val pressesUntilDeveloper = 8 - numPresses.intValue
                            if (pressesUntilDeveloper <= 0) {
                                makeToast("You're now a developer")
                                runBlocking { context.setSetting(IS_DEVELOPER, true) }
                            } else {
                                makeToast("You are $pressesUntilDeveloper steps until becoming a developer")
                            }
                        }
                    }
                }
            )
        },
        userSettingDecorationOnly {
            Tip(stringResource(
                    R.string.help_menu_version_tip,
                    BuildConfig.VERSION_NAME
                ))
        },

        userSettingNavigationItem(
            title = R.string.help_menu_website,
            subtitle = R.string.help_menu_website_subtitle,
            style = NavigationItemStyle.Misc,
            navigate = { nav ->
                nav.context.openURI("https://github.com/mugmarp/HAMP_KEYBOARD")
            }
        ).copy(searchTags = R.string.help_menu_website_tags),

        userSettingNavigationItem(
            title = R.string.help_menu_documentation,
            subtitle = R.string.help_menu_documentation_subtitle,
            style = NavigationItemStyle.Misc,
            navigate = { nav ->
                nav.context.openURI("https://github.com/mugmarp/HAMP_KEYBOARD#readme")
            }
        ),
        userSettingNavigationItem(
            title = R.string.help_menu_github,
            subtitle = R.string.help_menu_github_subtitle,
            style = NavigationItemStyle.Misc,
            navigate = { nav ->
                nav.context.openURI("https://github.com/mugmarp/HAMP_KEYBOARD/issues")
            }
        ),
        userSettingNavigationItem(
            title = R.string.help_menu_email,
            subtitle = R.string.help_menu_email_subtitle,
            style = NavigationItemStyle.Mail,
            navigate = { nav ->
                nav.context.openURI("mailto:markpaulmu@gmail.com")
            }
        ).copy(searchTags = R.string.help_menu_email_tags),
    )
)