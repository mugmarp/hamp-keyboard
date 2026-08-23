# Hamp Keyboard

Hamp Keyboard is a fork of the [FUTO Keyboard](https://github.com/futo-org/android-keyboard) (itself a fork of [LatinIME, The Android Open-Source Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME)), customized to install side-by-side with the official FUTO Keyboard app.

- **Package name:** `org.futo.inputmethod.latin.hamp`
- **Display name:** Hamp Keyboard
- All previously external dependencies (layouts, themes, dictionaries, translations, swipe models, AARs) are vendored directly into this repository — no submodules or GitLab access required.

## Why Hamp Keyboard exists — what it adds over FUTO Keyboard

Hamp's core flexibility upgrade: **the spacebar itself becomes a cursor-control surface.**

Traditional keyboards (including upstream FUTO Keyboard) reserve a dedicated **arrow-key row** for cursor movement. That row costs vertical space on every keyboard view and forces your thumb to travel to the layout's edge for every small adjustment.

Hamp keeps those arrow keys available — they're a standard toggle in Keyboard & Typing ("Show Arrow Keys", off by default) — but adds something upstream lacks: **all-direction swiping directly on the spacebar**, enabled out of the box:

- **Swipe left/right on the spacebar** moves the cursor character-by-character; combined with Hamp's fixes it works consistently across all apps and text fields, including terminals like Termius where cursor handling previously misbehaved.
- **You choose your setup**: keep the arrow row if you prefer classic navigation, hide it for a cleaner one-row-shorter keyboard with bigger keys, or use both — the spacebar swipe and the arrow row are fully independent settings.
- The spacebar swipe mode is configurable (cursor movement / language switching / off) under Keyboard & Typing → Spacebar.

The result is optionality upstream doesn't offer: users who never touch arrow keys get a cleaner keyboard with larger keys by default; users who want them can flip them back on at any time. No functionality was taken away — flexibility was added.

Additional differences from upstream:

- **Charcoal & Ember design system**: dark-first "Charcoal & Ember" theme as the default, with grouped/rounded settings cards, bundled Space Grotesk + DM Sans typography, circular icon tiles, and a matching light variant.
- **Independent project hygiene**: no FUTO payment, update-checking, crash reporting, or chat links; help & feedback points to this repository and its maintainer.

## Known issues

- **UI glitch during frame transitions**: there is a known rendering issue where frames transition incorrectly (visual flicker/artifacts when switching between keyboard views/screens). The app is otherwise stable and functional. This is tracked as a known cosmetic issue pending investigation.

## Changelog

### 1.0.0 (1)
- Renamed app to "Hamp Keyboard" with applicationId `org.futo.inputmethod.latin.hamp` for side-by-side installation with FUTO Keyboard.
- Removed FUTO-specific payment, update checking, and crash reporting systems.
- Fixed crash on startup caused by manifest references to removed classes (`CrashLoggingApplication`, `PaymentCompleteActivity`).
- Vendored all submodule dependencies into the repository.
- Vertical spacebar swipe for all text fields; Termius spacebar swipe cursor movement fix.

## Building

No recursive clone needed — everything is in this repository:
```
git clone https://github.com/mugmarp/HAMP_KEYBOARD.git
```

You can then open the project in Android Studio and build it that way, or use gradle commands:
```
./gradlew assembleStableDebug
./gradlew assembleStableRelease
```

## Credits

All credit for the original codebase goes to the [80+ contributors of FUTO Keyboard](https://github.com/futo-org/android-keyboard/graphs/contributors), whose work makes up the vast majority of this repository's history. The contributor list shown on GitHub reflects those original authors, not active Hamp Keyboard developers.

## Licensing

The original code is licensed under the [FUTO Source First License 1.1](LICENSE.md).

