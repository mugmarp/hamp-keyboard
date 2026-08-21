# HAMP Keyboard - FUTO Upstream Merge Workflow

This document describes the workflow for safely merging upstream FUTO Keyboard changes into HAMP Keyboard without losing custom functionality.

## Overview

HAMP Keyboard is a fork of FUTO Keyboard (upstream: https://gitlab.futo.org/keyboard/latinime). This workflow ensures safe merging of upstream changes while preserving HAMP-specific customizations.

## Branch Strategy

- `main` - Stable HAMP releases
- `merge-upstream-YYYYMMDD` - Temporary merge branches
- `upstream/master` - Remote tracking branch for FUTO upstream

## Protected Files (Never Overwrite During Merge)

These files contain HAMP-specific customizations that must be preserved:

### Core Customizations
| File | Purpose |
|------|---------|
| `java/src/org/futo/inputmethod/latin/InputAttributes.java` | Termius unconditional code field detection |
| `java/src/org/futo/inputmethod/latin/inputlogic/InputLogic.java` | cursorUp/cursorDown always send DPAD events |
| `java/src/org/futo/inputmethod/keyboard/PointerTracker.java` | Vertical swipe detection logic |
| `java/src/org/futo/inputmethod/keyboard/KeyboardActionListener.java` | `onMovePointerVertical` callback |
| `java/src/org/futo/inputmethod/engine/IMEInterface.kt` | `onMovePointerVertical` interface |
| `java/src/org/futo/inputmethod/engine/general/GeneralIME.kt` | `onMovePointerVertical` implementation |
| `java/src/org/futo/inputmethod/engine/general/ChineseIME.kt` | `onMovePointerVertical` implementation |
| `java/src/org/futo/inputmethod/engine/general/JapaneseIME.kt` | `onMovePointerVertical` implementation |
| `java/src/org/futo/inputmethod/engine/general/ActionInputTransactionIME.kt` | `onMovePointerVertical` implementation |
| `java/src/org/futo/inputmethod/latin/LatinIMELegacy.java` | `onMovePointerVertical` wiring |

### Branding & Icons
| File | Purpose |
|------|---------|
| `java/res/values/ic_launcher_background.xml` | Background color `#1A1F2E` |
| `java/unstable/res/values/ic_launcher_background.xml` | Background color `#1A1F2E` |
| `java/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon config |
| `java/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive icon config |
| `java/unstable/res/mipmap-anydpi-v26/ic_launcher.xml` | Unstable adaptive icon config |
| `java/unstable/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Unstable round adaptive icon config |
| `java/res/drawable/ic_launcher_foreground.png` | PNG foreground (stable) |
| `java/unstable/res/drawable/ic_launcher_foreground.png` | PNG foreground (unstable) |
| `java/res/values/strings-appname.xml` | App name "Hamp Keyboard" |
| `java/unstable/res/values/strings-appname.xml` | App name "Hamp Keyboard [Dev Build]" |
| `build.gradle` | Application ID `org.futo.inputmethod.latin.hamp` |

## Merge Workflow

### Prerequisites
- Upstream remote configured: `git remote add upstream https://gitlab.futo.org/keyboard/latinime.git`
- Clean working directory
- Current branch is `main`

### Step-by-Step Merge Process

```bash
#!/bin/bash
# merge-upstream.sh - Safe upstream merge script

set -e

DATE=$(date +%Y%m%d)
MERGE_BRANCH="merge-upstream-$DATE"

echo "=== HAMP Keyboard Upstream Merge ==="
echo "Date: $DATE"
echo "Merge branch: $MERGE_BRANCH"
echo ""

# 1. Ensure we're on main with clean state
git checkout main
git status --porcelain | grep -q . && { echo "ERROR: Working directory not clean. Commit or stash changes first."; exit 1; }

# 2. Fetch latest upstream
echo "Fetching upstream..."
git fetch upstream

# 3. Show what's new
echo ""
echo "=== Your changes (not in upstream) ==="
git log --oneline upstream/master..HEAD

echo ""
echo "=== Upstream changes (not in your fork) ==="
git log --oneline HEAD..upstream/master

# 4. Create merge branch
echo ""
echo "Creating merge branch: $MERGE_BRANCH"
git checkout -b "$MERGE_BRANCH"

# 5. Attempt merge
echo ""
echo "Attempting merge..."
if git merge upstream/master --no-commit --no-ff; then
    echo "Merge successful (no conflicts)"
else
    echo "Conflicts detected. Resolving..."
    
    # List conflicted files
    CONFLICTS=$(git diff --name-only --diff-filter=U)
    echo "Conflicted files:"
    echo "$CONFLICTS"
    echo ""
    
    # For each protected file, restore our version
    PROTECTED_FILES=(
        "java/src/org/futo/inputmethod/latin/InputAttributes.java"
        "java/src/org/futo/inputmethod/latin/inputlogic/InputLogic.java"
        "java/src/org/futo/inputmethod/keyboard/PointerTracker.java"
        "java/src/org/futo/inputmethod/keyboard/KeyboardActionListener.java"
        "java/src/org/futo/inputmethod/engine/IMEInterface.kt"
        "java/src/org/futo/inputmethod/engine/general/GeneralIME.kt"
        "java/src/org/futo/inputmethod/engine/general/ChineseIME.kt"
        "java/src/org/futo/inputmethod/engine/general/JapaneseIME.kt"
        "java/src/org/futo/inputmethod/engine/general/ActionInputTransactionIME.kt"
        "java/src/org/futo/inputmethod/latin/LatinIMELegacy.java"
        "java/res/values/ic_launcher_background.xml"
        "java/unstable/res/values/ic_launcher_background.xml"
        "java/res/mipmap-anydpi-v26/ic_launcher.xml"
        "java/res/mipmap-anydpi-v26/ic_launcher_round.xml"
        "java/unstable/res/mipmap-anydpi-v26/ic_launcher.xml"
        "java/unstable/res/mipmap-anydpi-v26/ic_launcher_round.xml"
        "java/res/values/strings-appname.xml"
        "java/unstable/res/values/strings-appname.xml"
        "build.gradle"
    )
    
    for file in "${PROTECTED_FILES[@]}"; do
        if git diff --name-only --diff-filter=U | grep -q "^$file$"; then
            echo "Restoring protected file: $file"
            git checkout HEAD -- "$file"
        fi
    done
    
    # For other conflicts, prefer upstream (but review manually)
    REMAINING_CONFLICTS=$(git diff --name-only --diff-filter=U)
    if [ -n "$REMAINING_CONFLICTS" ]; then
        echo ""
        echo "Remaining conflicts requiring manual resolution:"
        echo "$REMAINING_CONFLICTS"
        echo "Please resolve manually, then run:"
        echo "  git add <resolved-files>"
        echo "  git commit"
        exit 1
    fi
fi

# 6. Build test
echo ""
echo "Testing build..."
VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug --no-daemon

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "Next steps:"
    echo "  1. Review changes: git diff HEAD"
    echo "  2. Test APK: ./gradlew installStableDebug"
    echo "  3. Merge to main: git checkout main && git merge $MERGE_BRANCH"
    echo "  4. Push: git push origin main"
    echo "  4. Clean up: git branch -d $MERGE_BRANCH"
else
    echo ""
    echo "❌ Build failed. Fix issues before merging."
    exit 1
fi
```

### Manual Conflict Resolution

If the script exits with conflicts:

```bash
# 1. See conflicted files
git status

# 2. For each conflicted file, open and resolve:
#    - Look for <<<<<<< HEAD (your changes) vs >>>>>>> upstream/master (upstream changes)
#    - Keep HAMP customizations, accept upstream bug fixes

# 3. After resolving each file:
git add <file>

# 4. When all resolved:
git commit -m "Merge upstream/master: resolve conflicts"

# 5. Continue build test
VERSION_CODE=1 VERSION_NAME=1.0.0 ./gradlew assembleStableDebug --no-daemon
```

## Post-Merge Checklist

After successful merge and build:

- [ ] Run full verification: `./verify-all.sh` (or run the verification script)
- [ ] Test vertical swipe in Termius, Termux, and normal text fields
- [ ] Test horizontal swipe
- [ ] Test app icon appears correctly (stable + unstable)
- [ ] Verify Termius spacebar swipe works
- [ ] Run tests if available
- [ ] Merge to main: `git checkout main && git merge merge-upstream-YYYYMMDD`
- [ ] Push: `git push origin main`
- [ ] Clean up: `git branch -d merge-upstream-YYYYMMDD`
- [ ] Tag release if desired: `git tag -a v1.0.X -m "Release v1.0.X"`

## Schedule

- **Weekly**: Check for upstream updates (`git fetch upstream && git log --oneline HEAD..upstream/master`)
- **Bi-weekly/Merge**: Run merge workflow when significant upstream changes exist
- **Pre-release**: Always merge latest upstream before HAMP releases

## Upstream Repository Info

- **Upstream URL**: https://gitlab.futo.org/keyboard/latinime
- **Upstream Remote**: `upstream` (configured via `git remote add upstream https://gitlab.futo.org/keyboard/latinime.git`)
- **Upstream Branch**: `master`
- **Upstream Mirror**: https://github.com/futo-org/android-keyboard (mirror)

## Version History

| Date | Merge Commit | Upstream Commit | Notes |
|------|--------------|-----------------|-------|
| 2026-08-21 | 012b6b8d8 | e47f63eb8 | Initial HAMP fork with Termius fix |
| 2026-08-21 | 012b6b8d8 | - | Added vertical swipe for all fields |

---

## Troubleshooting

### Build Fails After Merge
1. Check for missing imports in Kotlin files
2. Verify all protected files restored correctly
3. Check for API changes in upstream (method signatures, etc.)
4. Run `./gradlew clean assembleStableDebug --no-daemon`

### Tests Fail After Merge
- Check if upstream changed test expectations
- Update HAMP-specific test expectations if needed

### Icon/Resource Issues
- Verify PNG files exist in both stable/unstable drawable folders
- Check adaptive icon XML references correct drawable names
- Verify background color `#1A1F2E` in both stable/unstable

---

*Last updated: 2026-08-21*
*Workflow version: 1.0*