# Hamp Keyboard Settings App — UI Transition Issues Analysis

## Summary

After a thorough comparison of the Hamp settings app against upstream FUTO, I've identified the key differences that could cause UI/transition issues. The settings navigation architecture is **identical** between Hamp and upstream FUTO (same `EnterTransition.None`/`ExitTransition.None`, same `SettingsActivity` structure, same `NavHost` setup). The regressions come from **Hamp-specific visual components** layered on top.

---

## Key Finding: Navigation Architecture Is Identical

Both Hamp and upstream FUTO use:
- `SettingsNavigator.kt` with `EnterTransition.None` / `ExitTransition.None` for all transitions
- `SettingsActivity.kt` with `enableEdgeToEdge()`, `Surface`, `Box(Modifier.safeDrawingPadding())`
- `repeatOnLifecycle(Lifecycle.State.STARTED)` for content updates
- Same `NavHostController` creation and navigation graph structure

**Conclusion:** The transition issues are NOT from navigation animation logic. They're from the visual components Hamp added.

---

## Root Cause Analysis

### 1. HampSection Component (Most Likely Culprit)

Hamp added `HampSection` — a new grouped-card container that wraps settings rows in a `Surface` with `shadowElevation` and `border`:

```kotlin
@Composable
fun HampSection(
    label: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if(label != null) {
            SectionLabel(label)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,  // <-- shadow
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(
                    width = 1.dp,
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(content = content)
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}
```

**Potential issues:**
- `shadowElevation = 2.dp` on a `Surface` inside a `LazyColumn` can cause rendering glitches during scroll
- The `border` with `SolidColor` may not render correctly on all devices/API levels
- The `Surface` creates a new drawing layer — if the list is scrolled quickly, the shadow/boundary may flicker
- The `Column` inside `Surface` may not properly propagate `LocalContentColor`, causing text color inconsistencies

### 2. HampSectionDivider Component

```kotlin
@Composable
fun HampSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}
```

**Potential issues:**
- `outlineVariant` may not be defined in all color schemes, causing fallback to a default color
- The `alpha = 0.35f` may be too faint on some displays, causing the divider to appear/disappear during transitions

### 3. CharcoalEmber Theme

Hamp uses CharcoalEmber as the default theme. The color scheme is:

| Token | Dark Value | Light Value |
|-------|-----------|-------------|
| background | `#0F1318` | `#F2F5FB` |
| surface | `#191E24` | `#E7EBF2` |
| primary | `#92C9F8` | `#075A8E` |
| outline | `#30363E` | (derived) |

**Potential issues:**
- The dark background `#0F1318` is very dark — if the system bar is not properly updated, there could be a visible flash during transitions
- The `surface` color `#191E24` is close to the background — the `HampSection` cards may not be clearly distinguishable, causing visual confusion during transitions
- The `outline` color `#30363E` is close to the surface — the `HampSection` border may not be visible, causing the card to appear/disappear during transitions

### 4. Bundled Fonts (Space Grotesk + DM Sans)

Hamp bundles Space Grotesk and DM Sans variable fonts. These are loaded via `FontVariation` on API 26+.

**Potential issues:**
- If the font is not loaded before the first draw, there could be a flash of the default font
- The `FontVariation` API may not work correctly on all devices, causing font rendering glitches
- The font metrics may differ from the system default, causing text to reflow during transitions

### 5. Home.kt Restructure

Hamp restructured `HomeScreen` into groups (TYPING, PERSONALIZE, OTHER) using `HampSection`. This is a significant change from upstream FUTO's flat list.

**Potential issues:**
- The `HampSection` components are nested inside a `LazyColumn` — if the list is scrolled, the sections may not render correctly
- The `SectionLabel` composable may not be properly aligned with the card, causing visual glitches
- The `Spacer(modifier = Modifier.height(10.dp))` at the end of each `HampSection` may cause spacing issues if the list is scrolled

---

## Comparison: Hamp vs Upstream FUTO

| Component | Upstream FUTO | Hamp | Risk |
|-----------|--------------|------|------|
| Navigation transitions | `EnterTransition.None` / `ExitTransition.None` | Same | None |
| Settings list | Flat list of `NavigationItem` | Grouped into `HampSection` cards | **High** |
| Card container | None (rows directly on background) | `Surface` with `shadowElevation` + `border` | **High** |
| Dividers | None (or simple `HorizontalDivider`) | `HampSectionDivider` with `alpha = 0.35f` | Medium |
| Theme | FUTO default | CharcoalEmber (dark `#0F1318`) | Medium |
| Fonts | System default | Space Grotesk + DM Sans (bundled) | Medium |
| Home screen structure | Flat list | Grouped sections | **High** |

---

## Recommended Investigation Steps

1. **Test HampSection rendering**:
   - Open settings and scroll through the home screen
   - Look for flickering, shadow glitches, or border rendering issues
   - Check if the cards are clearly distinguishable from the background

2. **Test theme transitions**:
   - Switch between dark and light themes
   - Look for flashes of the wrong color
   - Check if the system bar color is properly updated

3. **Test font rendering**:
   - Open settings and look at the text
   - Look for font rendering glitches or flashes of the default font
   - Check if the text is properly aligned

4. **Test navigation**:
   - Navigate between different settings screens
   - Look for visual glitches during navigation
   - Check if the new screen is properly drawn before the old screen is removed

5. **Test screen rotation**:
   - Rotate the device while in settings
   - Look for visual glitches during rotation
   - Check if the layout is properly restored after rotation

---

## Conclusion

The most likely cause of the UI transition issues is the **`HampSection` component** — a new grouped-card container that wraps settings rows in a `Surface` with `shadowElevation` and `border`. This component is not present in upstream FUTO and introduces a new drawing layer that could cause rendering glitches during transitions.

The secondary suspects are the **CharcoalEmber theme** (very dark colors that may not be clearly distinguishable) and the **bundled fonts** (which may not be loaded before the first draw).

I recommend testing the `HampSection` component first, as it's the most significant visual change from upstream FUTO.