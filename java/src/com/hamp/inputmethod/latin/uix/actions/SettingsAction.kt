package com.hamp.inputmethod.latin.uix.actions

import android.content.Intent
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.Action
import com.hamp.inputmethod.latin.uix.settings.SettingsActivity

val SettingsAction = Action(
    icon = R.drawable.settings,
    name = R.string.go_to_settings,
    simplePressImpl = { manager, _ ->
        val intent = Intent()
        intent.setClass(manager.getContext(), SettingsActivity::class.java)
        intent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        manager.getContext().startActivity(intent)
    },
    windowImpl = null,
)