package com.hamp.inputmethod.latin.uix.actions

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.Action
import com.hamp.inputmethod.latin.uix.ActionWindow
import com.hamp.inputmethod.latin.uix.isDirectBootUnlocked
import com.hamp.inputmethod.latin.uix.settings.SettingsActivity
import com.hamp.inputmethod.latin.uix.theme.selector.ThemePicker

val ThemeAction = Action(
    icon = R.drawable.themes,
    name = R.string.action_theme_switcher_title,
    simplePressImpl = null,
    canShowKeyboard = true,
    windowImpl = { manager, _ ->
        object : ActionWindow() {
            override val onlyShowAboveKeyboard: Boolean = true

            @Composable
            override fun windowName(): String {
                return stringResource(R.string.action_theme_switcher_title)
            }

            @Composable
            override fun WindowContents(keyboardShown: Boolean) {
                val context = LocalContext.current
                val resources = LocalResources.current
                val openSettingsLambda = {
                    if(context.isDirectBootUnlocked && !manager.isDeviceLocked()) {
                        SettingsActivity.openToNavDest(context, "themes")
                    } else {
                        val toast = Toast.makeText(
                            context,
                            resources.getString(R.string.action_clipboard_manager_error_device_locked_title),
                            Toast.LENGTH_SHORT
                        )

                        toast.show()

                    }
                }

                ThemePicker({ openSettingsLambda() }, openSettingsLambda)
            }
        }
    }
)