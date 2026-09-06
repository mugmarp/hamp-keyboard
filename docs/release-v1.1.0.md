# v1.1.0 Release Notes

**Published:** 2026-09-04  
**Commit:** `9f45696cc`  
**APK:** hamp-keyboard-v1.1.0.apk (139.4 MB)

## Features
- Independent package namespace (`com.hamp.inputmethod.latin`)
- Spacebar swipe indicators (chevrons for ◄ ► ▲ ▼)
- Charcoal & Ember design system
- Grouped settings cards
- New launcher icon

## Bug Fixes
- **Fixed crash during swipe typing** — added defensive try-catch around all swipe decoder operations (`getSuggestions`, `getPredictions`, `getOrInitDecoder`, `recognize`)
- Added null check for `Settings.getInstance().getCurrent()` in `PointerTracker.onMoveEventInternal`

## Technical
- All swipe decoder operations now have error handling to prevent crashes
- Settings access is null-guarded during early touch events

## Known Issues
- UI glitch during frame transitions (cosmetic)
- Debug-signed APK (release signing pending)
