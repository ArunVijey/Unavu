# Round 7: agent E notes (data, parser, quick add, widget)

Work was done in two passes (a first agent E was stopped mid-edit; this pass finished it). I did not build, run Gradle or change git state.

## Fixed

- **#7 Water in the parser.** `QuickParser.parse` recognises water / thanni / tanni / neer / தண்ணீர் / தண்ணி (plus kudineer, "sudu thanni", hot/warm/drinking) with an optional amount before or after (glass, tumbler, cup, bottle, ml, l, litre, half litre, Tamil number and unit words) and returns it in `Result.waterMl` (and `Result.water` per mention). "coconut water" and "neer mor" stay foods. Quick add shows a "💧 Water 250 ml → Liquids" row, adds typed water to the tile water on Add, and never offers "Add to My foods" for it. `WaterParserTest.kt` covers English, Tanglish, Tamil script, mixed text and dish names.
- **#11 Per-day target history.**
  - New `AppData.targetHistory: Map<String, Int>` (ISO date a target took effect -> kcal; default empty, so old data and backups decode). `Store.update` calls `withTargetRecorded` on every write, so any change to the daily kcal target (weigh-in, profile, goal, smart target) is recorded with today's date. The first change stores the old target as a baseline at the earliest logged day. Same-day undo prunes the entry. `replaceAll` and `restoreSnapshot` keep the snapshot's own history.
  - `targetKcalOn(date)`: a binary search over a lazily cached sorted key list (`AppData.targetDays`), O(log n), no allocation. C can call it 90 times per frame. Dates on or after the newest change use the live target.
  - `Report` and the CSV export (`Export.kt`, D's file, already calls it) judge each day against its own target.
  - **Choice: option (b), a snackbar, not ask-first.** The weigh-in already feeds the formula and Smart target is about burn estimates, so I kept the weigh-in and made the effect visible and reversible. Quick add's weight save now says "Saved 72.4 kg · daily target 2,058 → 2,064 kcal" with Undo, and Undo restores the weigh-in and the target together. Past days keep their old target, so nothing recolours. Option (a) would stop weight changing BMR at all, which breaks the "weigh in, target follows" expectation and Smart target's own weight use.
- **#3 QuickAdd saveable state.** `overrides` (map saver) and `comboItems` (list saver) survive rotation and process death (JSON via `NewItem`, now `@Serializable`).
- **#6 Quick-add draft.** `AppViewModel.saveQuickAddDraft/quickAddDraft/clearQuickAddDraft` (SavedStateHandle, JSON, 10 min TTL, same mode only). Closing with the X, scrim, back or "Build a plate" link saves text, meal, cheat, tile water, waist, overrides and combo items. Reopening restores them with "Picked up where you left off · Start over". Adding clears the draft.
- **#31 Widget.** Colours come from the app's `LightColors` / `DarkColors` / `AmoledColors` with the user's accent (`withAccent`), through Glance `ColorProvider(day, night)` (System follows the phone, Light/Dark are fixed). The accent is used for the primary pill and the progress fill. Both widget XMLs have `previewLayout` (API 31+) and `previewImage` (older) with day/night colours in `colors_r7e.xml`.
- **#4 Test.** `TargetHistoryTest.mealSharesAreConsistent`: shares equal the rounded exact share, sum to the target within rounding, and the audit's 2064 / 2058 examples (309 snacks). Same file tests target history, old-JSON decoding and `suggestMeal`.
- **#25** Every user-visible string in QuickAdd and the widget is in `strings_r7e.xml` (`r7e_` keys). `Meal.label` left alone.
- **Opus audit.**
  - One `suggestMeal(logged, minutes)` and `mealForMinutes(mins)` in `data/Calc.kt` (the only copy of the meal windows; `Meal.forHour` now delegates). Quick add pre-selects the clock meal, or the next unlogged meal if that one is already logged on the day being logged to.
  - Quick-add line items now sum to the total: the total is the sum of the rounded line values.
- **C's request.** `LogEntry.loggedOn: String? = null` (ISO date of creation), set in `logItems`, `useCombo`, `copyMeal` and `copyDay`.

## Not done / why

- **Report PDF text** (`Report.kt`) is English literals. The PDF is built without a Context. I left it.
- **UI test for the "Increase" size (#1)** is A's item; no androidTest set exists.
- **Past-day macro targets** (protein/carbs/fat) are not historical, only kcal.

## Requests for other owners

- **C (`TodayParts.kt` Next-up):** use the shared meal suggestion: `suggestMeal(logged: Set<Meal>, minutes: Int = nowMinutes()): Meal` in `app.unavu.data`, e.g. `suggestMeal(data.entriesOn(date).map { it.meal }.toSet())`. Import `app.unavu.data.suggestMeal`.
- **C (`Sections.kt` `timedEntries`):** can now use `LogEntry.loggedOn` (null in old data): an entry is back-filled when `loggedOn != null && loggedOn != date`. Keep the heuristic for null.
- **D (`Weight.kt`):** `addWeight` can move the daily target on its own. The Weight screen should show the same "target X -> Y kcal" snackbar with Undo (compare `data.targets()?.kcal` before and after, then `store.restoreSnapshot(before)`), string idea: `r7e_weight_saved_target` (args: kg, old, new). Past days are already protected by the history.
- **D (`Export.kt`):** already uses `targetKcalOn`; no change needed.
- **Coordinator:** `Meal.forHour` changed windows slightly (lunch to 15:30, snacks to 18:30); nothing else in the repo called it.

## Files changed in this pass

`data/Models.kt`, `data/Store.kt`, `data/Calc.kt`, `ui/screens/QuickAdd.kt`, `app/src/test/java/app/unavu/data/TargetHistoryTest.kt` (new), `docs/audit/r7-notes-e.md` (new). Earlier pass (same owner): `data/Parser.kt`, `data/Report.kt`, `ui/AppViewModel.kt`, `widget/Widgets.kt`, `res/xml/widget_big.xml`, `res/xml/widget_small.xml`, `res/layout/widget_preview_*.xml`, `res/drawable/widget_preview_*.xml`, `res/values/colors_r7e.xml`, `res/values-night/colors_r7e.xml`, `res/values/strings_r7e.xml`, `app/src/test/java/app/unavu/data/WaterParserTest.kt`.
