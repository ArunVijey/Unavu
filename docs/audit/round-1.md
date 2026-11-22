# Audit — Round 1 (baseline v1.1.1, commit 1626645)

Reviewer: Claude (Opus 5.5). Evidence from code; phone not connected, so device-only items are scored from code and marked (code).

| # | Category | Score | Evidence |
|---|----------|------:|----------|
| 1 | UI | 6 | Consistent token system (`ui/theme/Theme.kt` AppColors), but hand-rolled non-M3 controls (`ui/Components.kt` Pill/Segmented/PrimaryButton), hard-coded text colour on accent (`ui/screens/QuickAdd.kt` mode tabs `Color(0xFF1C1B18)`), no shared spacing/type tokens. |
| 2 | UX | 6 | Strong quick-add flow + Tanglish parser; but no undo after deletes (only a water "Undo" tile, `Tracking.kt:125`), no snackbar feedback anywhere (grep Snackbar = 0), blank box while loading (`MainActivity.kt` store==null branch), nav/sheet state lost on rotation. |
| 3 | Animation | 3 | One animation in the whole app (`QuickLook.kt:77`). No page/tab transitions (`MainActivity.kt` `when(page)` hard-swaps), progress `Track` jumps, no reduced-motion handling (grep = 0). |
| 4 | Color | 6 | Light/Dark/AMOLED palettes are coherent. Fails AA: `chi` on `chiS` ≈3.6:1 and `blue` on `blueS` ≈4.3:1 (StatusBadge 11sp text), placeholder `ink2@55%` (`QuickAdd.kt`) ≈2:1. `windowBackground` is always dark `#141311` (`res/values/themes.xml`) → dark flash in light mode. |
| 5 | Typography | 6 | Good font choices, tabular figures. 83 uses of 9–11sp text; ad-hoc sizes (10.5, 12.5, 14.5…) instead of a type scale; Excalifont multiplies `fontScale` ×1.14 (`Theme.kt`), on top of the user's own setting. |
| 6 | Accessibility | 2 | **Zero** `contentDescription`/`semantics`/`Role` in the codebase (grep = 0). Canvas icons (NavIcon, ArrowIcon, Fab) are invisible to TalkBack. Targets under 48dp: `IconCircle` 36dp, `ArrowButton` 40dp, Pill ~36dp, Segmented ~34dp. Bottom tabs have no selected/tab semantics. |
| 7 | Performance | 6 | Good: background store load, R8, profileinstaller. Bad: 0 lazy lists (37 `verticalScroll`), `QuickParser.parse` runs on every recomposition (`QuickAdd.kt`), the whole `AppData` goes through a `compositionLocalOf` (`Nav.kt`), so any write recomposes every reader. |
| 8 | Code quality | 5 | No ViewModel/state holders; Store singleton used directly from UI; 89 `remember{mutableStateOf}` vs 11 saveable; 0 string resources (all text hard-coded); one test file (`ParserTest.kt`). |
| 9 | Robustness | 4 | **Data-loss path**: a corrupt/unreadable JSON → `load()` silently returns `AppData()` (`Store.kt:53`) and the next save overwrites the user's file. Non-atomic fallback `file.delete(); tmp.renameTo(file)` (`Store.kt:61`). Save exceptions crash the IO scope. Nav stack/tab/sheet/onboarding are `remember`, not saved → lost on rotation and process death. Writes can land out of order. |
| 10 | Polish | 5 | Nice drawn icons + turmeric FAB. No ripple on the bottom bar (`indication = null`), haptics only in QuickLook, no SplashScreen API, the manifest is not UTF-8 (widget labels show "Unavu � today"). |

**Average 4.9, so Round 1 fails.** Accessibility (2), Animation (3) and Robustness (4) are under 6, so they get rebuilt.

## Defects by severity
1. Data loss when the file is corrupt, plus a non-atomic save (Store.kt)
2. No TalkBack semantics anywhere; touch targets under 48dp
3. State lost on rotation/process death (Nav, QuickAdd sheet, onboarding)
4. Contrast failures (status badges, placeholders, text on accent)
5. No motion system; no reduced-motion handling
6. No undo/feedback after destructive actions
7. Non-lazy lists; parsing during composition; whole-data CompositionLocal
8. Hard-coded strings; tiny type; fontScale hack
9. Dark window flash in light mode; no SplashScreen; manifest encoding
10. Thin test coverage
