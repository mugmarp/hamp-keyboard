# Upstream FUTO Changes (Not Yet Merged)

## Repository: https://gitlab.futo.org/keyboard/latinime

### Pending Commits (7 ahead of our base)

| Commit | Summary | Relevance |
|--------|---------|-----------|
| `afe5e8753` | Hide screenshot option if unsupported on device | Low - UI edge case |
| `eaf0389f9` | Change screenshot trash and path check | Low - screenshot feature |
| `b345f519e` | Treat Russian ie as a language long-press key | Medium - Russian layout users |
| `7ebbf5c8b` | Update contributing section | Low - docs only |
| `1525717f2` | Allow setting specific voice input app | Medium - voice input flexibility |
| `d9e96a33d` | Fix packageinfo crash | **High** - stability fix |
| `a0b84ef9a` | Fix braces | Low - code style |

### Recommendations for Next Release

**Should merge:**
- `d9e96a33d` - Fix packageinfo crash (stability)
- `1525717f2` - Allow setting specific voice input app (user feature)
- `b345f519e` - Russian ie long-press (locale support)

**Optional:**
- `afe5e8753` + `eaf0389f9` - Screenshot handling improvements (if screenshot feature is kept)

**Skip:**
- `7ebbf5c8b` - Contributing section (already customized for Hamp)
- `a0b84ef9a` - Braces (cosmetic, already customized)

### Notes
- Our base: `0246cbe2` (pre-namespace-migration)
- Upstream HEAD: `a0b84ef9a`
- These commits are on top of upstream master which still uses `org.futo.inputmethod.latin` namespace
- Merging will require conflict resolution in any files that touch the namespace or string resources
