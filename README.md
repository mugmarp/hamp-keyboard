# Hamp Keyboard

Hamp Keyboard is a fork of the [FUTO Keyboard](https://github.com/futo-org/android-keyboard) (itself a fork of [LatinIME, The Android Open-Source Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME)), customized to install side-by-side with the official FUTO Keyboard app.

- **Display name:** Hamp Keyboard
- **Application ID:** `com.hamp.inputmethod.latin.hamp`
- **Package namespace:** `com.hamp.inputmethod.latin`
- All previously external dependencies (layouts, themes, dictionaries, translations, swipe models, AARs) are vendored directly into this repository — no submodules or GitLab access required.

## Why Hamp Keyboard exists — what it adds over FUTO Keyboard

Hamp's core flexibility upgrade: **the spacebar itself becomes a cursor-control surface.**

Traditional keyboards (including upstream FUTO Keyboard) reserve a dedicated **arrow-key row** for cursor movement. That row costs vertical space on every keyboard view and forces your thumb to travel to the layout's edge for every small adjustment.

Hamp keeps those arrow keys available — they're a standard toggle in Keyboard & Typing ("Show Arrow Keys", off by default) — but adds something upstream lacks: **all-direction swiping directly on the spacebar**, enabled out of the box:

- **Swipe left/right on the spacebar** moves the cursor character-by-character; combined with Hamp's fixes it works consistently across several apps and text fields, including terminals like Termius where cursor handling previously misbehaved.
- **Swipe up/down on the spacebar** moves the cursor line-by-line.
- **Visual swipe indicators** — faint chevrons on the spacebar hint at the available gestures (◄ ► for cursor movement, ▲ ▼ for line-wise movement).
- **You choose your setup**: keep the arrow row if you prefer classic navigation, hide it for a cleaner one-row-shorter keyboard with bigger keys, or use both — the spacebar swipe and the arrow row are fully independent settings.
- The spacebar swipe mode is configurable (cursor movement / language switching / off) under Keyboard & Typing → Spacebar.

The result is optionality upstream doesn't offer: users who never touch arrow keys get a cleaner keyboard with larger keys by default; users who want them can flip them back on at any time. No functionality was taken away — flexibility was added.

Additional differences from upstream:

- **Charcoal & Ember design system**: dark-first "Charcoal & Ember" theme as the default, with grouped/rounded settings cards, bundled Space Grotesk + DM Sans typography, circular icon tiles, and a matching light variant.
- **Independent project hygiene**: no FUTO payment, update-checking, crash reporting, or chat links; help & feedback points to this repository and its maintainer.

## Known issues

- **UI glitch during frame transitions**: there is a known rendering issue where frames transition incorrectly (visual flicker/artifacts when switching between keyboard views/screens). The app is otherwise stable and functional. This is tracked as a known cosmetic issue pending investigation.

## Install

Download the APK from the [latest release](https://github.com/mugmarp/hamp-keyboard/releases/latest), install it, then enable **Hamp Keyboard** in Android's *Settings → System → Languages & input → On-screen keyboard*.

Release builds are currently signed with a debug key, so Android will warn about an unknown source.

## Changelog

### 1.1.0
- **Independent package namespace** migrated from `org.futo.inputmethod.latin` to `com.hamp.inputmethod.latin` — Hamp no longer shares any package identity with upstream FUTO Keyboard.
- **Spacebar swipe indicators** — faint chevrons on the spacebar advertise available swipe gestures (◄ ► for character-wise cursor movement, ▲ ▼ for line-wise movement).
- **Charcoal & Ember design system** as the default theme: dark-first OKLCH-derived palette with a matching light variant, bundled Space Grotesk + DM Sans typography, circular icon tiles.
- **Grouped settings**: home screen rows collected into rounded cards (Typing / Personalize / Other) with hairline dividers.
- **New launcher icon** generated from the Hamp artwork, with a correct adaptive-icon layer split (solid plate background, content-only foreground) and self-contained legacy icons.
- **Help & Feedback** retargeted to this repository and its maintainer; FUTO website, Discord, and Zulip links removed.

### 1.0.0
- Renamed app to "Hamp Keyboard" with application ID `com.hamp.inputmethod.latin.hamp` and package namespace `com.hamp.inputmethod.latin`.
- Removed FUTO-specific payment, update checking, and crash reporting systems.
- Fixed crash on startup caused by manifest references to removed classes (`CrashLoggingApplication`, `PaymentCompleteActivity`).
- Vendored all submodule dependencies into the repository.
- Vertical spacebar swipe for all text fields; Termius spacebar swipe cursor movement fix.

## Building

No recursive clone needed — everything is in this repository:
```
git clone https://github.com/mugmarp/hamp-keyboard.git
```

You can then open the project in Android Studio and build it that way, or use gradle commands:
```
./gradlew assembleStableDebug
./gradlew assembleStableRelease
```

`VERSION_CODE` and `VERSION_NAME` are read from the environment; the build fails with
`versionCode is set to -1` if they are unset, so pass them explicitly:
```
VERSION_CODE=7 VERSION_NAME=1.1.0 ./gradlew assembleStableDebug
```

Launcher icons are generated, not hand-edited — regenerate them with
`python3 tools/gen_launcher_icons.py` after changing the source artwork.

## Merging upstream changes

See [MERGE_WORKFLOW.md](MERGE_WORKFLOW.md). Hamp carries customizations that an
upstream merge can silently revert, so that document lists the protected paths and
the post-merge verification steps.

## Credits

All credit for the original codebase goes to the [80+ contributors of FUTO Keyboard](https://github.com/futo-org/android-keyboard/graphs/contributors), whose work makes up the vast majority of this repository's history. The contributor list shown on GitHub reflects those original authors, not active Hamp Keyboard developers.

## Licensing

The original code is licensed under the [FUTO Source First License 1.1](LICENSE.md).

