package com.hamp.inputmethod.latin.uix.actions

import android.view.KeyEvent
import com.hamp.inputmethod.latin.R
import com.hamp.inputmethod.latin.uix.Action

val UndoAction = Action(
    icon = R.drawable.undo,
    name = R.string.action_undo_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON)
    },
    windowImpl = null,
)
val RedoAction = Action(
    icon = R.drawable.redo,
    name = R.string.action_redo_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_Y, KeyEvent.META_CTRL_ON)
    },
    windowImpl = null,
)