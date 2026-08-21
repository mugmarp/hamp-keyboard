# HAMP Keyboard - Gboard-Style Dynamic Header Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Replace FUTO's static top utility bar with a Gboard-style dynamic header toggle and overlay feature drawer with three states (SUGGESTIONS ↔ QUICK_BAR ↔ DRAWER) with smooth AnimatedVisibility transitions, while preserving all native engine callbacks and dictionary/spellcheck connections.

**Architecture:** 
- Implement inside existing Compose UI layer (UixManager.kt's Content() function / ActionBar.kt)
- Use proper Compose state management: `mutableStateOf`, `remember`
- Three states: SUGGESTIONS, QUICK_BAR, DRAWER with AnimatedVisibility transitions
- Hook header toggle button to cycle states
- Preserve SuggestionStripView functionality when visible
- Keep native voice input, dictionary pack listeners, spellcheck connections active in background
- Safe fallbacks for null SuggestionStripView (e.g., password fields)
- Use existing FUTO vector drawables from java/res/drawable/ — no new drawable references without verification
- True overlay drawer using Box with Modifier.fillMaxSize() to prevent touch bleed-through

**Tech Stack:**
- Kotlin, Jetpack Compose, Android Gradle Plugin
- Build command: `VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug`
- Current state: commit `e47f63eb8` (app ID `org.futo.inputmethod.latin.hamp`, name "Hamp Keyboard")

---

## Current State Analysis (Commit e47f63eb8)

**Root Layout:** `java/res/layout/input_view.xml` — Root is `<org.futo.inputmethod.latin.InputView>` with single `<include layout="@layout/main_keyboard_frame" />`

**Compose Entry Point:** `UixManager.kt:Content()` function (around line 1269) — composes the keyboard UI including ActionBar

**Existing ActionBar:** `ActionBar.kt` has `ActionBar` composable with:
- `isActionsExpanded` state (collapsed/expanded suggestions)
- `ActionBarHeight = 40.dp` (expands to 80.dp when `isActionsExpanded`)
- `ExpandActionsButton` toggles `isActionsExpanded`
- Renders suggestions when `actionBarShown || inlineSuggestions.isNotEmpty()`

**Existing Drawables (verified in java/res/drawable/):**
- `clipboard.xml` ✓
- `undo.xml` ✓
- `mic.xml` ✓
- `more_horizontal.xml` ✓ (for drawer toggle)
- `edit_text.xml` ✓ (for cursor select)

---

## Clarifying Questions (Must Answer Before Implementation)

### 1. Header Toggle Integration
**Q1:** Should the header toggle button be:
- (a) A new dedicated button in the ActionBar (replacing or alongside existing expand button)?
- (b) Repurpose the existing `ExpandActionsButton` to cycle SUGGESTIONS → QUICK_BAR → DRAWER?
- (c) A separate button in the header area?

**My recommendation:** (b) Repurpose existing expand button — it already toggles `isActionsExpanded`; extend it to cycle three states.

### 2. Quick Bar Icon Set
**Q2:** The 5 icons for QUICK_BAR — confirm exact set:
1. Undo (`undo.xml`) ✓
2. Cursor/Selection (`edit_text.xml`) ✓
3. Voice Input (`mic.xml`) ✓
4. Clipboard (`clipboard.xml`) ✓
5. Open Drawer / Grid Menu (`more_horizontal.xml`) ✓

**Back button:** Should there be a 6th "Back to Suggestions" button (using `arrow_left_26.xml` or similar)?

### 3. Drawer Content (Tool Matrix)
**Q3:** Confirm 4 categories × tools:
- **Text Tools:** Undo, Redo, Cursor, Select All
- **Voice:** Voice Input, Transcribe
- **Clipboard:** Clipboard History, Paste
- **Settings:** Theme, Layout, Languages

Any additions/removals?

### 4. State Reset Triggers
**Q4:** Reset to SUGGESTIONS on:
- (a) Any keystroke (`onCodeInput` / `sendCodePointEvent`) ✓
- (b) App switch / new input field (`onStartInputView`) ✓
- (c) Dismissing drawer (tap outside, back button) ✓
- Anything else? (e.g., orientation change, keyboard dismissal)

### 5. Drawer Implementation
**Q5:** Drawer implementation approach:
- (a) **True overlay** — `Box(Modifier.fillMaxSize())` with dim background, drawer panel at top, tap-outside-to-dismiss. Prevents touch bleed-through.
- (b) **Layout sibling** — Inside header Column, expands below suggestions (current legacy approach)

**My recommendation:** (a) True overlay using `Box(Modifier.fillMaxSize())` with dim background and tap-outside dismissal — matches Gboard behavior.

### 6. SuggestionStripView Integration
**Q6:** The existing suggestion strip is rendered by Compose (`ActionBar` / `ActionBarWithExpandableCandidates`). How to integrate?
- (a) Keep as-is in SUGGESTIONS state, hide in QUICK_BAR/DRAWER via `AnimatedVisibility`
- (b) Extract into separate composable, reuse in header

**My recommendation:** (a) — The existing ActionBar already renders suggestions conditionally (`actionBarShown || inlineSuggestions.isNotEmpty()`). Just control visibility via header state.

### 7. Animation Spec
**Q7:** Transition animations:
- QUICK_BAR: `slideInVertically` + `fadeIn` (150ms)
- DRAWER: `slideInVertically` + `fadeIn` (200ms, taller)
- SUGGESTIONS return: `fadeIn` only (100ms)

Or use `expandVertically`/`shrinkVertically` for QUICK_BAR (like existing expand)?

### 8. Package & Namespace
**Q8:** Confirm namespace isolation:
- All new classes: `org.futo.inputmethod.latin.hamp.*`
- New Compose functions: `HampHeader`, `HampQuickToolBar`, `HampToolMatrixDrawer`
- State enum: `HampHeaderState { SUGGESTIONS, QUICK_BAR, DRAWER }`
- Strings: `hamp_*` prefix (e.g., `hamp_open_drawer`, `hamp_category_text`)

---

## Proposed Step-by-Step Plan

### Phase 1: Research & Setup (No Code Changes)
- [ ] **Task 1.1:** Verify existing drawables exist (`R.drawable.redo`, `R.drawable.select_all`, `R.drawable.clipboard_manager`, `R.drawable.themes`, `R.drawable.keyboard`, `R.drawable.globe`, `R.drawable.arrow_left_26`)
- [ ] **Task 1.2:** Read `ActionBar.kt` fully — understand `ActionBar`, `ActionBarWithExpandableCandidates`, `isActionsExpanded`, `toggleActionsExpanded`
- [ ] **Task 1.3:** Read `UixManager.kt` — find `Content()` function, `shouldShowSuggestionStrip`, `isActionsExpanded` state
- [ ] **Task 1.4:** Read `LatinIME.kt` — find `onCreateInputView`, `onStartInputView`, `onCodeInput`
- [ ] **Task 1.4:** List missing drawables needed (`redo`, `select_all`, `clipboard_manager`, `themes`, `keyboard`, `globe`, `arrow_left_26`)

### Phase 2: Core Implementation
- [ ] **Task 2.1:** Create `HampHeader.kt` in `org.futo.inputmethod.latin.hamp` or `org.futo.inputmethod.latin.uix` with:
  - `HampHeaderState` enum
  - `HampHeader` composable (main entry)
  - `HampQuickToolBar` composable
  - `HampToolMatrixDrawer` composable
  - `HampDrawerOverlay` composable (true overlay)
- [ ] **Task 2.2:** Add `hamp_*` strings to `strings-appname.xml` (stable + unstable)
- [ ] **Task 2.3:** Modify `ActionBar.kt`:
  - Add `HampHeaderState` parameter
  - Integrate `HampHeader` composable
  - Add `HampDrawerOverlay` as sibling in Column
  - Replace/extend `toggleActionsExpanded` to cycle three states
- [ ] **Task 2.4:** Modify `UixManager.kt`:
  - Add `hampHeaderState = mutableStateOf(HampHeaderState.SUGGESTIONS)`
  - Pass state and callbacks to `ActionBar`
  - Handle state reset on `onStartInputView`, `onCodeInput`
- [ ] **Task 2.5:** Modify `LatinIME.kt`:
  - Ensure state reset in `onStartInputView` and `onCodeInput`/`sendCodePointEvent`

### Phase 3: String Resources & Drawables
- [ ] **Task 3.1:** Add `hamp_*` strings to `java/res/values/strings-appname.xml`
- [ ] **Task 3.2:** Add same to `java/unstable/res/values/strings-appname.xml` (dev build)
- [ ] **Task 3.3:** Verify/create missing drawables (if any)

### Phase 4: Build & Test
- [ ] **Task 4.1:** Run `VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug`
- [ ] **Task 4.2:** Install APK, test on device/emulator
- [ ] **Task 4.3:** Verify: 3-state cycling, animations, drawer overlay, state reset, no flickering
- [ ] **Task 4.4:** Test edge cases: password fields, orientation, app switch

---

## Files Likely to Change

| File | Change Type |
|------|-------------|
| `java/src/org/futo/inputmethod/latin/uix/HampHeader.kt` | **NEW** — Core header composables |
| `java/src/org/futo/inputmethod/latin/uix/ActionBar.kt` | Modify — integrate HampHeader, add drawer overlay |
| `java/src/org/futo/inputmethod/latin/uix/UixManager.kt` | Modify — add state, pass callbacks |
| `java/src/org/futo/inputmethod/latin/LatinIME.kt` | Modify — state reset hooks |
| `java/res/values/strings-appname.xml` | Add `hamp_*` strings |
| `java/unstable/res/values/strings-appname.xml` | Add `hamp_*` strings |
| `java/res/layout/input_view.xml` | **NO CHANGE** (keep InputView root) |

---

## Tests / Validation

| Test | Command |
|------|---------|
| Build | `VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug` |
| Install | `adb install -r build/outputs/apk/stable/debug/latinime-stable-debug.apk` |
| Logcat | `adb logcat -c && adb logcat | grep -E "LatinIME|HampHeader|HampDrawer"` |
| Manual test | Open any text field, tap header toggle 3x, verify cycle |

---

## Risks & Tradeoffs

| Risk | Mitigation |
|------|------------|
| Flickering during transitions | Use `expandVertically`/`shrinkVertically` + `AnimatedVisibility` with proper specs |
| Touch bleed-through in drawer | True overlay `Box(Modifier.fillMaxSize())` with `clickable` + `focusable` |
| SuggestionStripView null in password fields | Null-safe `AnimatedVisibility`, fallback to empty |
| State stuck on app switch | Reset in `onStartInputView` + `onCodeInput` |
| Animation jank | Use `tween(150-200)`, avoid heavy composables in transition |
| Missing drawables | Verify all `R.drawable.*` exist before implementation |

---

## Open Questions (Need Your Input)

Please answer Q1–Q8 above before I proceed. Once clarified, I'll finalize the plan and save to `.hermes/plans/`.

---

**Saved as:** `.hermes/plans/2026-08-17_123000-hamp-keyboard-header-plan.md`