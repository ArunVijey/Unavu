# Round 1 tasks: 6 parallel groups

Each group owns files exclusively (see `contract.md`). Your **letter** names your strings file (`res/values/strings_<letter>.xml`, keys prefixed `<letter>_`) and your notes file (`docs/audit/notes-<letter>.md`).
Commit trailer for this round: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` (it replaces the GPT line in contract.md).

## SCREEN CHECKLIST (groups 3–6 apply all of it to every screen file they own)
1. **A11y:** every icon/emoji-only control gets a contentDescription. Clickable rows/cards are merged (`semantics(mergeDescendants = true)`) with Role.Button. Headings use `heading()`. Every touch target is ≥48dp. Selections expose selected/stateDescription (selectableGroup + Role.Tab/RadioButton). Every Canvas chart/bar gets a text summary contentDescription. Form fields have labels and `error()` semantics.
2. **Type:** `U.t.<role>` (display/headline/title/titleS/body/bodyS/label/caption) instead of literal `fontSize = x.sp`. Nothing under 12sp. No fixed heights around text (`heightIn(min=)`). Must survive 200% font scale.
3. **Color:** no hard-coded `Color(0x…)` (accent swatch values excepted). Red/blue text uses `c.chiInk`/`c.blueInk`, placeholders `c.hint`, text on accent `c.onAcc`.
4. **State:** user input, open dialogs/sheets/menus/quick-looks and selected items use `rememberSaveable` (save ids or enum names, not objects).
5. **Perf:** long lists become `LazyColumn` with stable `key` + `contentType` + `Modifier.animateItem()`. Heavy aggregation goes in `remember(keys)`. Use `data.entriesOn(date)` / `entriesBetween(from,to)` from group 1 (if they're missing when you build, use a local `remember` and leave a note).
6. **Motion:** animate via `motionTween(...)` from `ui/theme/Motion.kt` (it already honours reduced motion). Numbers count up/down, bars fill.
7. **UX:** every destructive action → `val before = store.data.value; …; LocalFeedback.current?.show(msg, undoLabel) { store.restoreSnapshot(before) }`. Logging shows "Added … to <meal>" with Undo. Empty states with one clear action. Errors are shown, never silent. Haptics (`LocalHapticFeedback`) on add/delete.
8. **Code:** split files over ~300 lines into a sub-package folder (keep public composable names and package `app.unavu.ui.screens` so MainActivity compiles). All user-visible strings go to your strings file (plurals for counts).

---

## Group 1: data + background + widgets (letter `a`)
Owns: `data/**`, `work/**`, `widget/**`, `app/src/test/**` except `test/.../ui/ContrastTest.kt`.
1. Store.kt: a file that exists but fails to parse is copied to `unavu-data.corrupt-<ts>.json`, then the `.tmp` is tried, and `loadIssue` is set (the skeleton exists; make it real). Never overwrite the bad file with an empty AppData. Make Store constructible from a File for JVM tests (keep `get`/`peek`).
2. Atomic save: tmp → `fd.sync()` → `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` (fall back to REPLACE_EXISTING). Catch and log IO errors; CoroutineExceptionHandler.
3. A single conflated writer that always saves the newest snapshot. `flush()` really writes. `restoreSnapshot` refreshes FoodDb custom foods + nextId. `useCombo` becomes one `update`.
4. **Do this first, then commit:** `AppData.entriesOn(date)` and `entriesBetween(from,to)` with a cached per-instance index.
5. Export/Report: failures → `Result.failure` with a readable message. Restore validates before replacing.
6. `@Immutable` on model classes that really are immutable. JSON stays backward compatible.
7. work/: receivers use goAsync + load the Store off the main thread. Never post without POST_NOTIFICATIONS. Inexact alarms (or check canScheduleExactAlarms). Handle reboot/TIME_SET/TIMEZONE_CHANGED (put the manifest XML in your notes for group 2). Backup worker retries on IO errors. Notification texts and channel go in strings.
8. widget/: Glance text contrast ≥4.5:1 day/night, 48dp tap targets with descriptions, strings, a "no profile yet" state, refresh never throws. QS tiles: state description, `unlockAndRun`, PendingIntent on API 34+.
9. Tests (JUnit4): corrupt file preserved + loadIssue; atomic round trip; migration v1→v2; last write wins after 1,000 updates; restoreSnapshot; entriesOn; Calc math; Smart weekly gate; Parser edge cases; next-reminder time across midnight.

## Group 2: app shell + theme (letter `b`)
Owns: `MainActivity.kt`, `ui/Nav.kt`, `ui/Feedback.kt`, new `ui/AppViewModel.kt`, new `ui/shell/**`, `ui/theme/Theme.kt`, `Type.kt`, `Motion.kt`, `app/build.gradle.kts`, `AndroidManifest.xml`, `res/values/themes.xml`, `colors.xml`, `res/values-night/**`, `res/mipmap-*`, `res/drawable/ic_launcher*`, `res/xml/widget_*.xml`, `test/.../ui/ContrastTest.kt`.
1. Gradle: add `androidx.core:core-splashscreen:1.0.1` and `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` (do this first).
2. State survives rotation + process death: Page/Tab/QuickAddRequest `@Serializable`, Nav state in AppViewModel via SavedStateHandle (JSON), Nav's public API unchanged. Onboarding step + draft profile saveable.
3. SplashScreen API (`Theme.Unavu.Starting`, day/night backgrounds #FBF8F2 / #141311, keep-on-screen until `Store.peek()!=null`). Remove the blank-box branch. Day/night windowBackground.
4. Manifest: re-save as UTF-8 (the widget labels have a broken dash), labels into strings, plus group 1's receiver actions if they're in their notes. Adaptive icon has `<monochrome>`.
5. Feedback: host a token-styled SnackbarHost above the bottom bar, provide LocalFeedback. Show a loadIssue dialog (Restore from backup → Page.Backup / OK → clearLoadIssue). `store.flush()` in onStop.
6. Motion: page push/pop with AnimatedContent shared-axis X (Motion.LONG), tab fade-through (~180ms), instant when reduced motion is on. PredictiveBackHandler if feasible.
7. Bottom bar: selectableGroup + `selectable(role = Role.Tab)` with ripple, labels readable by TalkBack, animated indicator, ≥48dp, FAB description "Add food", haptic on tab change, labels don't clip at large font scale.
8. Make `LocalData` static, fed from the ViewModel StateFlow.
9. Theme: real values for `chiInk`/`blueInk`/`hint` (≥4.5:1 on their soft bg and on bg/sf, all 3 palettes). `withAccent` uses the actual WCAG ratio for accInk/onAcc. A `contrast(a,b)` helper. Every M3 colorScheme role set from tokens (no default purple).
10. Type: remove the ×1.14 fontScale hack. Provide `LocalAppType` from AppTheme (Excalifont: +1sp per role), make `U.t` read it (update Type.kt's `currentType`), map onto M3 Typography, and a Tamil line-height helper.
11. `LocalReducedMotion` provided in AppTheme from `Settings.Global.ANIMATOR_DURATION_SCALE` (ContentObserver).
12. ContrastTest.kt: every text/bg token pair, 3 palettes × default + AccentPicker swatches, ≥4.5.
13. All strings from your files → strings_b.xml.

## Group 3: shared components + Today (letter `d`)
Owns: `ui/Components.kt`, `ui/Widgets2.kt`, `ui/theme/Sketch.kt`, `ui/screens/Today.kt`, `ui/screens/QuickLook.kt`, new `ui/screens/today/**`.
Components (keep existing signatures; only add optional params): ArrowButton/IconCircle 48dp targets with default descriptions; Fab "Add food"; Pill/Segmented 48dp min + selectable semantics; SectionLabel/TopBar `heading()`; ListRow/AppCard merged + Role.Button; Track animated + progress semantics; StatusBadge uses chiInk/blueInk; a new `SwitchRow` toggleable helper; implement `Modifier.pressScale()` (0.97, off when reduced motion is on) and use it on PrimaryButton/AppCard/Pill; haptics on switch/stepper; steppers 48dp with descriptions; MealPicker selectable. U.t roles, no literal sp.
Today: full SCREEN CHECKLIST, LazyColumn of sections, a budget text summary for TalkBack, animated kcal number, undo on delete/cheat/copy, empty day state, a small loadIssue banner, split into `today/`, remove dead `keepFoodDb` if unused.

## Group 4: quick add, plate builder, meals, foods (letter `f`)
Owns: `ui/screens/QuickAdd.kt`, `PlateBuilder.kt`, `Meals.kt`, `MyFoods.kt`, `FoodDetail.kt`, `Customise.kt`, new `ui/screens/add/**`, `ui/screens/meals/**`.
Full SCREEN CHECKLIST, plus: QuickAdd mode tabs selectableGroup/Role.Tab with `c.onAcc` text (not `Color(0xFF1C1B18)`); placeholder `c.hint`; input labelled for TalkBack; `QuickParser.parse` wrapped in `remember(text, mode, data.portions)`; all sheet state `rememberSaveable`; "Added … " snackbar with Undo + haptic; unknown-word guidance with an "Add to My foods" action (Page.MyFood(name)); weight 20–300 validation; Add disabled with a reason. Meals/foods: undo deletes, empty states (no combos, no custom foods, no search results → add as custom food), editor validation.

## Group 5: insights, smart target, tracking (letter `g`)
Owns: `ui/screens/Insights.kt`, `SmartTargetScreen.kt`, `Tracking.kt`, new `ui/screens/insights/**`, `ui/screens/tracking/**`.
Full SCREEN CHECKLIST, plus: every chart gets a spoken summary; History/weight/liquids lists lazy; undo on weight delete and water; empty states (History, Weight → `nav.quickAdd.value = QuickAddRequest(mode="WEIGHT")`, Liquids, Insights with <2 days of data); PDF report success/failure snackbar; charts animate in once.

## Group 6: settings, accent, onboarding (letter `i`)
Owns: `ui/screens/SettingsScreens.kt`, `AccentPicker.kt`, `Onboarding.kt`, new `ui/screens/settings/**`.
Full SCREEN CHECKLIST, plus: split SettingsScreens.kt into one file per screen (same public names). Switch rows toggleable (Role.Switch), theme/font radio groups, accent swatches named + ≥48dp. Notification permission: ask contextually; handle permanent denial with "Open settings" (ACTION_APP_NOTIFICATION_SETTINGS); Reminders screen shows a warning row when it's off (re-check on resume). Backup/restore result snackbars; restore asks for confirmation and offers Undo. Onboarding: validation (age 10–100, height 100–250, weight 20–300), "Step 1 of 2", ImeAction.Next, button disabled with a reason.
