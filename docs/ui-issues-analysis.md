# Hamp Keyboard UI Issues Analysis

## Summary

After comparing Hamp Keyboard's UI implementation against upstream FUTO, I've identified several potential UI issues that are **not present in upstream FUTO**. These are regressions introduced during the Hamp-specific modifications.

---

## Issue 1: ActionBar Refactor (Shared Horizontal Strip)

### What Changed
Hamp replaced FUTO's expandable pop-above ActionBar with a HeliBoard-style shared horizontal strip. This is the **most significant architectural change** and the most likely source of UI issues.

### Upstream FUTO Behavior
- `needToUseExpandableSuggestionUi` determines whether to use the expandable UI
- When `true`: `ActionBarWithExpandableCandidates` pops **above** the keyboard, increasing total height
- When `false`: Regular `ActionBar` is used, suggestions are shown in the same space
- The keyboard view is offset by `keyboardViewOffset` to make room for the expandable bar

### Hamp Behavior
- Single fixed-height strip (40dp) for both suggestions and action keys
- Toggling tools swaps content inside the same band
- No height increase when action bar is expanded

### Potential Issues
1. **Overlap when action bar is expanded**: If the action bar content is taller than 40dp, it could overlap with the keyboard or suggestions
2. **Height calculation errors**: The `KeyboardSizingCalculator` was modified to return a single strip height. If this calculation is wrong, the keyboard could be too tall or too short
3. **Suggestion strip visibility**: When the action bar is expanded, suggestions are hidden. If this transition is not smooth, it could cause flickering
4. **Inline candidates**: These are shown only when the action bar is collapsed. If the state is not properly tracked, they could be hidden when they should be visible

### Files to Check
- `java/src/com/hamp/inputmethod/latin/uix/ActionBar.kt`
- `java/src/com/hamp/inputmethod/latin/uix/UixManager.kt`
- `java/src/com/hamp/inputmethod/v2keyboard/KeyboardSizingCalculator.kt`

---

## Issue 2: Suggestion Visibility Override

### What Changed
Hamp added a "Always show suggestions" toggle that overrides the system's `updateVisibility(false)` call.

### Upstream FUTO Behavior
- `updateVisibility(shouldShowSuggestionsStrip, fullscreenMode)` directly sets `shouldShowSuggestionStrip.value`
- No override logic

### Hamp Behavior
- `updateVisibility` checks if the user has enabled "Always show suggestions"
- If enabled, it overrides the system's request to hide suggestions
- **Exception**: Still respects critical suppressions (password fields, etc.)

### Potential Issues
1. **Suggestions showing in password fields**: If the critical suppression check is not comprehensive enough, suggestions could appear in password fields
2. **UI flickering**: If the override is applied inconsistently (e.g., only on some calls to `updateVisibility`), it could cause the suggestion strip to flicker
3. **State desync**: If `shouldShowSuggestionStrip.value` is out of sync with the actual suggestion computation, it could cause visual glitches

### Files to Check
- `java/src/com/hamp/inputmethod/latin/uix/UixManager.kt` (lines ~1394-1420)
- `java/src/com/hamp/inputmethod/latin/InputAttributes.java` (lines ~130-180)

---

## Issue 3: Voice Input Changes

### What Changed
Hamp ported upstream commit `1525717f2` which adds voice input app selection.

### Potential Issues
1. **VoiceInputSwitchActivity not declared correctly**: If the manifest declaration is wrong, the activity could crash
2. **RichInputMethodManager changes**: The `switchToShortcutIme` method now returns a boolean. If callers don't handle the return value correctly, it could cause issues
3. **Settings UI**: The new dropdown picker for voice input engine selection could have layout issues

### Files to Check
- `java/src/com/hamp/inputmethod/latin/uix/settings/VoiceInputSwitchActivity.kt`
- `java/src/com/hamp/inputmethod/latin/RichInputMethodManager.java`
- `java/src/com/hamp/inputmethod/latin/uix/settings/pages/VoiceInput.kt`

---

## Issue 4: Settings Navigation Transitions

### What Changed
Both Hamp and upstream FUTO use `EnterTransition.None` and `ExitTransition.None` for all navigation transitions. This means there are **no animations** when navigating between settings screens.

### Potential Issues
1. **Abrupt transitions**: Without animations, navigation feels jarring
2. **State loss**: If the navigation state is not properly preserved, it could cause settings to reset

### Note
This is **not a regression** — both Hamp and upstream FUTO have the same behavior. However, it's worth noting that the lack of transitions could be perceived as a UI issue.

---

## Issue 5: Theme Changes

### What Changed
Hamp uses the Charcoal & Ember theme as the default, which changes the color scheme and typography.

### Potential Issues
1. **Contrast issues**: If the theme colors don't have sufficient contrast, text could be hard to read
2. **Keyboard color mismatch**: If the keyboard colors don't match the settings theme, it could look inconsistent
3. **Font rendering**: The bundled fonts (Space Grotesk + DM Sans) could have rendering issues on some devices

### Files to Check
- `java/src/com/hamp/inputmethod/latin/uix/theme/presets/CharcoalEmber.kt`
- `java/src/com/hamp/inputmethod/latin/uix/theme/Type.kt`

---

## Recommended Investigation Steps

1. **Test the ActionBar refactor**:
   - Toggle the action bar on/off
   - Check for overlap with keyboard and suggestions
   - Test with number row on/off
   - Test with arrow keys on/off
   - Test in portrait and landscape

2. **Test the suggestion visibility override**:
   - Enable "Always show suggestions"
   - Test in a password field (suggestions should be hidden)
   - Test in a normal text field (suggestions should be visible)
   - Test in an app that requests to hide suggestions (suggestions should be visible)

3. **Test the voice input changes**:
   - Open Voice Input settings
   - Select a different voice input engine
   - Verify the selection is saved
   - Test voice input switching

4. **Test the theme**:
   - Check contrast in both dark and light themes
   - Verify keyboard colors match the settings theme
   - Check font rendering

---

## Conclusion

The most likely source of UI issues is the **ActionBar refactor** (Issue 1). This is a significant architectural change that affects the core keyboard layout. The suggestion visibility override (Issue 2) is also a potential source of issues, but it's more contained.

I recommend testing the ActionBar refactor first, as it's the most impactful change.