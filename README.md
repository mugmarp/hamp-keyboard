# Hamp Keyboard

Hamp Keyboard is a fork of the [FUTO Keyboard](https://github.com/futo-org/android-keyboard) (itself a fork of [LatinIME, The Android Open-Source Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME)), customized to install side-by-side with the official FUTO Keyboard app.

- **Package name:** `org.futo.inputmethod.latin.hamp`
- **Display name:** Hamp Keyboard
- All previously external dependencies (layouts, themes, dictionaries, translations, swipe models, AARs) are vendored directly into this repository — no submodules or GitLab access required.

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

## Licensing

The original code is licensed under the [FUTO Source First License 1.1](LICENSE.md).

