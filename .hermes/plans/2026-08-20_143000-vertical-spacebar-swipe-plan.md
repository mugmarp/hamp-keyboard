# Vertical Spacebar Swipe Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add vertical spacebar swipe (up/down) that sends DPAD_UP/DPAD_DOWN key events through the same code-field detection path that makes horizontal swipe work in Termius.

**Architecture:** Reuse existing PointerTracker → KeyboardActionListener → InputLogic → code-field detection → DPAD key events path. Add vertical axis alongside existing horizontal.

**Tech Stack:** Java (PointerTracker, KeyboardActionListener, InputLogic, InputLogicHandler), Kotlin (actions)

---

## Current State Analysis

**Working Horizontal Path:**
```
PointerTracker.onMoveEventInternal() 
  → onMoveEventInternal() detects horizontal swipe on spacebar
  → calculates steps from x-delta
  → calls sListener.onMovePointer(steps)  [KeyboardActionListener]
  → LatinIMELegacy.onMovePointer(steps)
  → IMEManager.getActiveIME().onMovePointer(steps, false, null)
  → GeneralIME.onMovePointer(steps, false, null)
  → InputLogic.cursorLeft/Right(steps, stepOverWords, select)
  → cursorLeftInternal/RightInternal()
  → cursorStep(±steps, stepOverWords, select)
  → if code-field OR no-cursor-position → sendDownUpKeyEvent(KEYCODE_DPAD_LEFT/RIGHT)
  → else → setSelection() via cursorStep()
```

**Code-field detection (InputAttributes.java):**
```java
// Now includes unconditional Termius check:
if(editorInfo.packageName.startsWith("com.server.auditor.ssh.client"))
    return CODE_FIELD_NO_COMPOSITION;
```

**Missing Vertical Path:**
- No `onMovePointerVertical` in `KeyboardActionListener`
- No vertical detection in `PointerTracker.onMoveEventInternal()`
- No `cursorUp/cursorDown` in `InputLogic`
- Arrow keys exist as actions (CursorActions.kt) sending DPAD_UP/DOWN but not connected to spacebar swipe

---

## Implementation Tasks

### Task 1: Add Vertical Swipe Callback to KeyboardActionListener

**Objective:** Add `onMovePointerVertical(int steps)` to the listener interface

**Files:**
- Modify: `java/src/org/futo/inputmethod/keyboard/KeyboardActionListener.java`

**Step 1: Add method signature to interface**
```java
public void onMovePointerVertical(int steps);
```

**Step 2: Add empty implementation to Adapter**
```java
@Override
public void onMovePointerVertical(int steps) {}
```

**Step 3: Verify compilation**
Run: `./gradlew :compileStableDebugKotlin --no-daemon`

---

### Task 2: Implement Vertical Detection in PointerTracker

**Objective:** Add vertical swipe detection in `onMoveEventInternal()` alongside horizontal

**Files:**
- Modify: `java/src/org/futo/inputmethod/keyboard/PointerTracker.java`

**Step 1: Add constants for vertical threshold**
```java
private static final int VERTICAL_SWIPE_THRESHOLD = 8; // pixels, adjust as needed
private static final int HORIZONTAL_VERTICAL_RATIO = 2; // horizontal must be < vertical/2 for vertical to trigger
```

**Step 2: Add vertical detection logic in onMoveEventInternal()**
In the spacebar handling block (around line 973), add vertical detection alongside horizontal:

```java
// After horizontal step calculation (around line 999)
// Also check for vertical movement when horizontal movement is small
float verticalStepProgress = ((-y) - (-mStartY)) / ((float)pointerStep); // -y because screen coords
int verticalSteps = (int)verticalStepProgress;

if (Math.abs(verticalSteps) > 0 && Math.abs(steps) < Math.abs(verticalSteps) * HORIZONTAL_VERTICAL_RATIO) {
    // Vertical swipe dominates - call vertical handler
    mCursorMoved = true;
    mStartY += verticalSteps * pointerStep;
    sListener.onMovePointerVertical(verticalSteps);
}
```

**Step 3: Verify compilation**
Run: `./gradlew :compileStableDebugKotlin --no-daemon`

---

### Task 3: Add onMovePointerVertical to LatinIMELegacy

**Objective:** Wire the callback through LatinIMELegacy to IMEManager

**Files:**
- Modify: `java/src/org/futo/inputmethod/latin/LatinIMELegacy.java`

**Step 1: Add method to LatinIMELegacy**
```java
@Override
public void onMovePointerVertical(int steps) {
    mImeManager.getActiveIME(mSettings.getCurrent()).onMovePointerVertical(steps);
}
```

**Step 2: Verify compilation**

---

### Task 4: Add onMovePointerVertical to IMEInterface and GeneralIME

**Files:**
- Modify: `java/src/org/futo/inputmethod/engine/IMEInterface.java`
- Modify: `java/src/org/futo/inputmethod/engine/general/GeneralIME.kt`

**Step 1: Add to IMEInterface**
```java
fun onMovePointerVertical(steps: Int)
```

**Step 2: Implement in GeneralIME**
```kotlin
override fun onMovePointerVertical(steps: Int) {
    if (steps < 0) {
        inputLogic.cursorUp(-steps)
    } else {
        inputLogic.cursorDown(steps)
    }
}
```

---

### Task 5: Add cursorUp/cursorDown to InputLogic

**Objective:** Add vertical cursor movement methods that send DPAD_UP/DOWN through code-field path

**Files:**
- Modify: `java/src/org/futo/inputmethod/latin/inputlogic/InputLogic.java`

**Step 1: Add public methods (after cursorRight, around line 3019)**
```java
/**
 * Shifts the cursor up by a number of lines
 * @param steps How many lines to step over
 */
public void cursorUp(int steps) {
    final SettingsValues settingsValues = Settings.getInstance().getCurrent();
    steps = Math.abs(steps);
    if(!mConnection.hasCursorPosition() || settingsValues.mInputAttributes.mIsCodeField) {
        mConnection.finishComposingText();
        int meta = 0;
        // No step-over-words equivalent for vertical
        mConnection.beginBatchEdit();
        for(int i=0; i<steps; i++)
            sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, meta);
        mConnection.endBatchEdit();
    } else {
        // For normal text fields, move up by line
        cursorUpInternal(steps);
    }
}

/**
 * Shifts the cursor down by a number of lines
 * @param steps How many lines to step over
 */
public void cursorDown(int steps) {
    final SettingsValues settingsValues = Settings.getInstance().getCurrent();
    steps = Math.abs(steps);
    if(!mConnection.hasCursorPosition() || settingsValues.mInputAttributes.mIsCodeField) {
        mConnection.finishComposingText();
        int meta = 0;
        mConnection.beginBatchEdit();
        for(int i=0; i<steps; i++)
            sendDownUpKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, meta);
        mConnection.endBatchEdit();
    } else {
        // For normal text fields, move down by line
        cursorDownInternal(steps);
    }
}

private void cursorUpInternal(int steps) {
    // Move up by line in normal text fields
    // Use getUnicodeSteps for vertical? Or custom logic
    // For now, reuse cursorStep with negative steps
    cursorStep(-steps, false, false);
}

private void cursorDownInternal(int steps) {
    // Move down by line in normal text fields
    cursorStep(steps, false, false);
}
```

---

### Task 6: Wire through IMEInterface and InputLogicHandler

**Files:**
- Modify: `java/src/org/futo/inputmethod/engine/IMEInterface.java`
- Modify: `java/src/org/futo/inputmethod/engine/InputLogicHandler.java`

**Step 1: Add to IMEInterface**
```java
fun onMovePointerVertical(steps: Int)
```

**Step 2: Add to InputLogicHandler**
```kotlin
fun onMovePointerVertical(steps: Int) {
    if (steps < 0) {
        inputLogic.cursorUp(-steps)
    } else {
        inputLogic.cursorDown(steps)
    }
}
```

**Step 3: Update GeneralIME to call handler**
```kotlin
override fun onMovePointerVertical(steps: Int) {
    inputLogicHandler.onMovePointerVertical(steps)
}
```

---

### Task 7: Update Settings for Vertical Swipe Mode

**Objective:** Add setting to control vertical swipe behavior (optional but recommended)

**Files:**
- Modify: `java/src/org/futo/inputmethod/latin/settings/Settings.java`
- Modify: `java/src/org/futo/inputmethod/latin/uix/settings/pages/Swipe.kt`

**Step 1: Add setting constants (Settings.java)**
```java
public static final String PREF_SPACEBAR_VERTICAL_SWIPE_MODE = "pref_spacebar_vertical_swipe_mode";
public static final int SPACEBAR_VERTICAL_MODE_OFF = 0;
public static final int SPACEBAR_VERTICAL_MODE_CURSOR = 1; // DPAD up/down
```

**Step 2: Add UI in Swipe.kt settings page**

---

### Task 8: Update PointerTracker to Respect Vertical Setting

**Objective:** Check vertical swipe mode setting before processing vertical swipe

**Files:**
- Modify: `java/src/org/futo/inputmethod/keyboard/PointerTracker.java`

**Step 1: Add check for vertical mode in spacebar handling block**

```java
int verticalSwipeMode = settingsValues.mSpacebarVerticalSwipeMode;
if (verticalSwipeMode != Settings.SPACEBAR_VERTICAL_MODE_OFF) {
    // Process vertical swipe
}
```

---

### Task 9: Integration Testing

**Objective:** Verify vertical swipe works in Termius and other terminals

**Test Steps:**
1. Build: `VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug --no-daemon`
2. Install APK
3. Test in Termius:
   - Spacebar horizontal → cursor left/right (already works)
   - Spacebar vertical up → command history up / vim up
   - Spacebar vertical down → command history down / vim down
4. Test in Termux (should still work)
5. Test in normal text field (should do nothing or line move)
6. Verify arrow key actions still work independently

---

## Files Summary

| File | Change Type |
|------|-------------|
| `KeyboardActionListener.java` | Add `onMovePointerVertical(int steps)` |
| `PointerTracker.java` | Detect vertical swipe, call `onMovePointerVertical` |
| `LatinIMELegacy.java` | Wire `onMovePointerVertical` to IMEManager |
| `IMEInterface.java` | Add `onMovePointerVertical(int steps)` |
| `InputLogicHandler.java` | Add `onMovePointerVertical` |
| `InputLogic.java` | Add `cursorUp/cursorDown` with DPAD key events |
| `GeneralIME.kt` | Call handler.onMovePointerVertical |
| `Settings.java` | Add vertical swipe mode setting |
| `Swipe.kt` | Add UI for vertical swipe mode |

---

## Verification Checklist

- [ ] Build compiles: `./gradlew assembleStableDebug --no-daemon`
- [ ] Termius: spacebar horizontal works (existing)
- [ ] Termius: spacebar vertical up → command history up
- [ ] Termius: spacebar vertical down → command history down
- [ ] Termux: both axes work
- [ ] Normal text field: vertical does nothing or line-move
- [ ] Arrow key actions still work
- [ ] Settings UI for vertical mode works

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking horizontal swipe | Keep horizontal logic unchanged, add vertical as separate branch |
| Accidental diagonal triggers | Require dominant vertical movement (ratio check) |
| Breaking language swipe | Only activate vertical when `SPACEBAR_VERTICAL_MODE_CURSOR` |
| Performance | Reuse existing step accumulation logic |

---

## Dependencies

This plan depends on the **Termius code-field fix** already committed (`37f12a476`) which ensures `com.server.auditor.ssh.client` returns `CODE_FIELD_NO_COMPOSITION` unconditionally.

---

**Ready to execute?** Each task is 10-20 min. Run `subagent-driven-development` to execute sequentially with verification.