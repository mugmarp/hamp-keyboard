#!/bin/bash
# merge-upstream.sh - Safe upstream merge script for HAMP Keyboard
# Usage: ./merge-upstream.sh

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
    echo "Build successful!"
    echo ""
    echo "Next steps:"
    echo "  1. Review changes: git diff HEAD"
    echo "  2. Test APK: ./gradlew installStableDebug"
    echo "  3. Merge to main: git checkout main && git merge $MERGE_BRANCH"
    echo "  4. Push: git push origin main"
    echo "  5. Clean up: git branch -d $MERGE_BRANCH"
else
    echo ""
    echo "Build failed. Fix issues before merging."
    exit 1
fi