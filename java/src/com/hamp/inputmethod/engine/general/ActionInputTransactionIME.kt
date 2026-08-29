package com.hamp.inputmethod.engine.general

import android.view.inputmethod.EditorInfo
import com.hamp.inputmethod.engine.IMEHelper
import com.hamp.inputmethod.engine.IMEInterface
import com.hamp.inputmethod.event.Event
import com.hamp.inputmethod.latin.InputConnectionInternalComposingWrapper
import com.hamp.inputmethod.latin.SupportsNonComposing
import com.hamp.inputmethod.latin.VoiceInputAlternativeIC
import com.hamp.inputmethod.latin.VoiceInputAlternativeICComposing
import com.hamp.inputmethod.latin.common.Constants
import com.hamp.inputmethod.latin.common.InputPointers
import com.hamp.inputmethod.latin.uix.ActionInputTransaction
import com.hamp.inputmethod.latin.uix.getSetting
import com.hamp.inputmethod.latin.uix.utils.TextContext
import com.hamp.inputmethod.v2keyboard.KeyboardLayoutSetV2

class ActionInputTransactionIME(val helper: IMEHelper) : IMEInterface, ActionInputTransaction {
    val useComposingMode = run {
        val inputType = helper.getCurrentEditorInfo()?.inputType ?: 0
        val inputClass = inputType and EditorInfo.TYPE_MASK_CLASS
        inputClass == EditorInfo.TYPE_CLASS_TEXT
    }

    val ic = if(helper.context.getSetting(VoiceInputAlternativeIC) && SupportsNonComposing && useComposingMode) {
        InputConnectionInternalComposingWrapper(
            helper.context.getSetting(VoiceInputAlternativeICComposing),
            true,
            helper.getCurrentInputConnection())
    } else {
        helper.getCurrentInputConnection()
    }

    override fun onCreate() {}
    override fun onDestroy() {}
    override fun onDeviceUnlocked() {}
    override fun onStartInput() {}
    override fun onOrientationChanged() {}
    override fun onFinishInput() {}
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int
    ) {
        if(ic is InputConnectionInternalComposingWrapper) {
            if(!ic.mightBeBelated(oldSelStart, oldSelEnd, newSelStart, newSelEnd)) {
                ic.cursorUpdated(oldSelStart, oldSelEnd, newSelStart, newSelEnd)
            }
        }
    }

    override fun isGestureHandlingAvailable(): Boolean = false
    override fun onEvent(event: Event) {}
    override fun onStartBatchInput() {}
    override fun onUpdateBatchInput(batchPointers: InputPointers?) {}
    override fun onEndBatchInput(batchPointers: InputPointers?) {}
    override fun onCancelBatchInput() {}
    override fun onCancelInput() {}
    override fun onFinishSlidingInput() {}
    override fun onCustomRequest(requestCode: Int): Boolean = false
    override fun onMovePointer(steps: Int, stepOverWords: Boolean, select: Boolean?) {}
    override fun onMovePointerVertical(steps: Int) {}
    override fun onMoveDeletePointer(steps: Int) {}
    override fun onUpWithDeletePointerActive() {}
    override fun onUpWithPointerActive() {}
    override fun onSwipeLanguage(direction: Int) {}
    override fun onMovingCursorLockEvent(canMoveCursor: Boolean) {}
    override fun clearUserHistoryDictionaries() {}
    override fun requestSuggestionRefresh() {}
    override fun onLayoutUpdated(layout: KeyboardLayoutSetV2) { }

    override val textContext: TextContext = TextContext(
        beforeCursor = ic?.getTextBeforeCursor(Constants.VOICE_INPUT_CONTEXT_SIZE, 0),
        afterCursor = ic?.getTextAfterCursor(Constants.VOICE_INPUT_CONTEXT_SIZE, 0)
    )

    private var isFinished = false
    private var partialText = ""
    override fun updatePartial(text: String) {
        if (isFinished || !useComposingMode) return
        helper.requestCursorUpdate()
        partialText = text
        ic?.setComposingText(
            partialText,
            1
        )

        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    override fun commit(text: String) {
        if (isFinished) return
        helper.requestCursorUpdate()
        isFinished = true
        ic?.commitText(
            text,
            1
        )
        helper.endInputTransaction(this)
        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    override fun cancel() {
        helper.requestCursorUpdate()
        commit(partialText)
        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    fun ensureFinished() {
        isFinished = true
    }
}