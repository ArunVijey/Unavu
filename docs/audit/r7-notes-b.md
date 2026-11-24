# Round 7: agent B notes (shell, theme, performance)

Files changed by B in this pass: `ui/shell/Shell.kt`, `ui/Feedback.kt`, new `app/src/main/baseline-prof.txt`, this file. `MainActivity.kt`, `ui/Nav.kt`, `ui/shell/BottomBar.kt`, `ui/theme/Theme.kt`, `res/values/themes.xml` and `res/values-night/themes.xml` already carried the #2 / #25 / #27 work from the earlier parked round-7 pass (verified, not re-edited). No new strings were needed, so there is no `strings_r7b.xml`.

## Fixed

- **#2 Status-bar icons.** Verified in place: `AppTheme` runs `SideEffect { syncBarIcons(window, dark) }` on every composition; `MainActivity` calls `enableEdgeToEdge` with styles from the saved app theme (`applySystemBars(savedSettings())`), again after the splash exit and on window focus; the 800 ms timer is gone; `values-night/themes.xml` only describes the splash moment and the app overrides it once it draws.
- **#3 State survives push/pop.** `Shell.kt` wraps every destination in `rememberSaveableStateHolder().SaveableStateProvider(key)` (key = `tab:<TAB>` or `page:<depth>:<page>`). A popped page's slot is dropped with `removeState`; the root tabs' slots are kept, so Meals' selected tab, scroll positions and `rememberSaveable` fields come back. The screens must use saveable state (`rememberSaveable`, `rememberLazyListState`, `rememberScrollState`) for that to show; plain `remember` state in a screen is still lost when its page leaves composition.
- **#9 Snackbar double frame.** Slot overload of `Snackbar`, with the fill and the 1 dp hairline drawn on the pill itself (container transparent, `background` then `border`), action as a `TextButton`.
- **#15 Predictive back.** While the gesture runs, the previous destination is composed under the current page (no saved state, `LocalSaveableStateRegistry` null, so it never collides with the real slot), translated -30% to 0 with progress. The shrinking page gets rounded corners (0 to 28 dp). The preview is removed before `nav.pop()` commits, so the real pop transition never shows two copies. Reduced motion: no preview, no scale, as before.
- **#27 LocalData.** `compositionLocalOf` (verified; the only provider is `ProvideAppLocals`, which reads the State, and all readers use `LocalData.current`). Added a starter `baseline-prof.txt` (wildcard rules for start-up, store/data, theme, shell, Today, quick add, plate, other tabs). No new module or tooling change.
- **#25 (my files).** Tab labels already come from `b_tab_*` resources; no literals remain in Nav/Shell/BottomBar/Feedback/Icons.
- **Opus audit items.** The snackbar is no longer dismissed on a tab switch: Shell dismisses only on a stack size change (push/pop), and not when the message was requested within 500 ms of it (`Feedback.requestedAtMs`), so "Added ... Undo" followed by `nav.pop()` survives. The tab fade-through scale-in is 0.98 instead of 0.96.

## Not done / why

- Baseline profile is hand-written, not generated (the contract forbids new modules). The `:baselineprofile` Macrobenchmark module and a measured release cold start remain open.
- `AppData` is not marked `@Immutable` and not moved to `kotlinx.collections.immutable` (`data/Models.kt` is E's).
- Not compiled or run (rules). Things to check at build: `Snackbar` slot overload parameters, `RoundedCornerShape(28.dp * bp)` inside `graphicsLayer`.

## Requests

- Screen owners: use saveable state for anything that should survive push/pop (selected filters/tabs, expanded sections, scroll). Lazy lists should use `rememberLazyListState()`.
- Anyone calling `Feedback.show` right before or after `nav.pop()` is fine; a message shown more than 500 ms before a push/pop is dismissed by it.
