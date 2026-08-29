package com.hamp.inputmethod.latin.uix.actions

import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.Subtypes
import com.hamp.inputmethod.latin.uix.Action


val SwitchLanguageAction = Action(
    icon = R.drawable.globe,
    name = R.string.show_language_switch_key,
    simplePressImpl = { manager, _ ->
        if(Subtypes.switchToNextLanguage(manager.getContext(), 1) == null) {
            manager.openInputMethodPicker()
        }
    },
    altPressImpl = { manager, _ ->
        manager.openInputMethodPicker()
    },
    windowImpl = null,
)