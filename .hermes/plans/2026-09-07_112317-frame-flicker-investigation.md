# Frame Flicker Investigation & Reverted Fix

> **For Hermes:** This plan documents the flicker investigation, the fix that was attempted and rolled back, and the open questions for the next attempt. Read this before starting any new flicker work — it explains what was tried and why it failed.

**Goal:** Eliminate the visible flicker / "flash of stale screen" when the keyboard appears or the user moves between text fields / app frames.

**Status:** Fix attempt was implemented, built, and tested by the user. User reported the fix did not work. Reverted. Investigation incomplete.

---

## Symptom (user description)

When the user moves from one app's text field to another (or the keyboard appears / re-appears), a brief flash of the previous screen is visible on the new screen. This is described as a "flicker" of the previous screen, not just a brief redraw of the keyboard.

The README at the top-level of the repo already documents this as a known issue:
> "UI glitch during frame transitions (visual flicker/artifacts when switching between keyboard views/screens). This is tracked as a known cosmetic issue pending investigation."

---

## What I investigated (commit history on `improvement-box`)

The full attempt is preserved in the remote `improvement-box` branch on `mugmarp/hamp-keyboard`. Commits in order:

```
cde164dc0  (base)  fix: add missing Settings import to InputAttributes.java
c9dde1543          fix(flicker): stabilize Compose↔View handoff ...
24e6a6dfa          fix: use FrameLayout for placeholder (ViewGroup is abstract)
333fd3d7b          Revert "fix: use FrameLayout for placeholder ..."
9a8001ec0          Revert "fix(flicker): stabilize Compose↔View handoff ..."
```

The first commit was the fix attempt. The second was a build-fix (the placeholder `ViewGroup` was abstract; needed `FrameLayout`). The third and fourth are reverts. CI build of the fix succeeded (run #34111421763, 7m 53s); debug APK was placed in `~/Downloads/bwat/ci-builds/`. User installed the APK and reported the flicker was still present.

---

## The fix that was attempted (the "3-step plan")

The diagnosis was that the legacy `InputView` was being torn down and re-inflated on every focus change, producing a 1-frame gap during which the OS briefly showed a cached image of the previous keyboard.

The fix had three steps:

### Step 1 — `LatinIMELegacy.java`
`onStartInputViewInternal` was calling `switcher.updateKeyboardTheme(mDisplayContext)` unconditionally on every focus change. The guard:
```java
if (!restarting || switcher.isThemeSwitchPending()) {
    switcher.updateKeyboardTheme(mDisplayContext);
}
```
Plus a new `isThemeSwitchPending()` accessor on `KeyboardSwitcher`.

### Step 2 — `LatinIME.kt`
The `LegacyKeyboardView` composable used `key(legacyInputView) { AndroidView(...) }`. The `key()` wrapper forced Compose to dispose and recreate the entire `AndroidView` node every time `legacyInputView.value` changed, with a `removeView` from the old parent. The fix: drop `key()`, keep the `AndroidView` stable for the lifetime of the IME session, and only attach the first `InputView` ever seen.

### Step 3 — `KeyboardSwitcher.java`
`updateKeyboardTheme` was unconditionally inflating a new `InputView` and pushing it to the Compose layer via `updateLegacyView(...)`. The fix: only inflate when `mKeyboardView == null` (very first show); otherwise do an in-place refresh via `updateMainKeyboardViewSettings()` + `invalidateAllKeys()`.

### What this should have done
- On every focus change, `legacyInputView.value` should NOT change → no Compose recreation → no 1-frame gap → no flicker.
- On a real theme change, the InputView is re-used, and the new keyboard is loaded into the existing `MainKeyboardView` via `setKeyboard(...)`.

---

## Why the fix didn't work (my analysis of where the diagnosis was incomplete)

I did not have a device to test on. I was reasoning about code paths, not observing behavior. The user tested on a real device and the flicker was still there. Without a logcat trace or a screen recording from their test, I can only guess. The most likely reasons:

1. **The flicker is not caused by the InputView teardown I targeted.** It may be a different layer entirely — e.g. the IME window's show/hide animation, the `setInputView` call's re-layout, the `MainKeyboardView.setKeyboard(...)` redraw, the `loadKeyboard` call's `KeyboardLayoutSetV2` rebuild, the `WindowInfoTracker.windowLayoutInfo` flow at `UixManager.kt:1484-1486` (which calls `latinIME.invalidateKeyboard(true)` on every layout change).

2. **There is another path that re-inflates the InputView that I missed.** My grep for `LayoutInflater.from(...).inflate` only found one call site (`KeyboardSwitcher.onCreateInputView`), but I may have missed an indirect path — e.g. via reflection, a service restart, or a `setInputView(null)` followed by a fresh `onCreateInputView`.

3. **The fix introduced a regression I didn't catch.** The placeholder `FrameLayout` path is essentially dead code (legacyInputView is set synchronously before first composition), but it exists. The `removeView` in the factory is the same as the old code. The `update` lambda only logs a warning and doesn't actually swap views, so a legitimate re-inflation (if one occurs) would now leak a warning but not actually re-attach. Possible but unlikely the cause.

4. **The user's "flicker" is something other than what I described.** Their description is "flash of the previous screen on the current screen", which could be:
   - A stale frame in the system frame buffer (OS-level, not in our code).
   - A cross-fade issue between the app frame and the IME window.
   - The `windowAnimations` style applied to the IME window.
   - Something related to the `enableOnBackInvokedCallback` in `AndroidManifest.xml:44`.

---

## Open questions for the next investigation

1. **What does the flicker actually look like on the user's device?** A screen recording (or a slow-motion video) would clarify whether it's a 1-frame artifact, a longer flash, or a sustained visual change.
2. **Does the flicker happen on a brand-new text field (fresh show, `restarting = false`) or only on a "switching to a different field" (`restarting = true`)?** This would localize the cause to `onStartInputView` vs. some other lifecycle method.
3. **Does it happen with `kdbg`/`logcat` trace on, so we can see which methods fire when?** `adb logcat -s LatinIME:V KeyboardSwitcher:V` would help.
4. **Is the flicker worse on certain Android versions?** Some Android versions handle IME window animations differently.

---

## What I would do differently next time

- **Test on a real device first**, not just reason about code. I should have asked the user to run an unmodified build and capture the flicker as a video before designing the fix.
- **Make the fix smaller and more reversible.** The 3-step plan touched three files and 130+ lines. A smaller, single-file change with a clearer hypothesis (e.g. "does the `WindowInfoTracker` flow cause the flicker?") would have been easier to roll back and easier to attribute.
- **Verify the fix actually addresses the symptom by reproducing the symptom without the fix and with the fix.** Without a device I couldn't do this. The user implicitly did by testing the built APK, and the flicker was still there.
- **Don't ship a fix without a way to detect the regression.** The "log a warning if `legacyInputView` is reassigned" check was a good instinct, but I never confirmed the warning would actually fire under the conditions that cause the flicker.

---

## Code locations (for the next attempt)

These are the files I touched and the relevant code paths for any future flicker work:

- `java/src/com/hamp/inputmethod/latin/LatinIME.kt`
  - `onCreateInputView()` — line 535: framework callback that creates the `InputView`.
  - `LegacyKeyboardView()` — line 588: the `AndroidView` interop. **This is the most likely place a stable-View approach is needed.**
  - `setInputView()` — line 663: called by the framework; pushes `legacyInputView.value` to `LatinIMELegacy`.
  - `invalidateKeyboard()` — line 366: called from many places (settings changes, layout info, etc.); calls `recreateKeyboard()`.
  - `recreateKeyboard()` — line 224: calls `latinIMELegacy.updateTheme()` which calls `KeyboardSwitcher.updateKeyboardTheme`.
  - `uixManager.setContent()` — line 541: triggers Compose to compose the tree including `LegacyKeyboardView`.

- `java/src/com/hamp/inputmethod/latin/LatinIMELegacy.java`
  - `onCreateInputView()` — line 291: creates the `InputView` via `mKeyboardSwitcher.onCreateInputView()`.
  - `onStartInputViewInternal()` — line 368: called on every focus change; calls `loadKeyboard()` which rebuilds the `KeyboardLayoutSetV2`.
  - `setInputView()` — line 310: stores the `InputView` reference for the framework.

- `java/src/com/hamp/inputmethod/keyboard/KeyboardSwitcher.java`
  - `updateKeyboardTheme()` — line 105: was the unconditional rebuild path; my Step 3 made it in-place.
  - `onCreateInputView()` — line 423: the **only** `LayoutInflater.from(...).inflate(R.layout.input_view, ...)` call site in the entire codebase.
  - `loadKeyboard()` — line 133: rebuilds the `KeyboardLayoutSetV2` and re-loads the keyboard on focus change.

- `java/src/com/hamp/inputmethod/latin/uix/UixManager.kt`
  - line 1484-1486: `WindowInfoTracker.windowLayoutInfo(...).collect { ... invalidateKeyboard(true) }` — **this fires on every layout change** and may be a contributing cause of the flicker.

---

## What is preserved in the remote

- All 5 commits are on `origin/improvement-box` (the 2 fix attempts + 2 reverts). The git log is the audit trail.
- PR #1 at https://github.com/mugmarp/hamp-keyboard/pull/1 is OPEN with the latest run on the revert.
- The debug APK of the (failed) fix is at `~/Downloads/bwat/ci-builds/hamp-keyboard-debug-24e6a6dfa-20260907.apk` if the user wants to inspect it for any reason (e.g., comparing the logcat to a known-good build).
- This plan file is at `.hermes/plans/2026-09-07_112317-frame-flicker-investigation.md`.

---

## Recommendation for next time

Before any further code changes:
1. Ask the user to capture a screen recording of the flicker.
2. Ask the user to run `adb logcat -d` over the duration of a flicker event and share the log.
3. Diagnose from the actual logcat (which methods fire, in what order, with what timestamps) before designing a fix.
4. Make the smallest possible code change that addresses the actual logcat pattern.
5. Verify the fix in the same logcat session — the flicker should be gone AND the relevant method calls should not change.
