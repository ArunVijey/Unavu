# Notes: group 5 (letter g), insights / smart target / tracking

## Changed
- `Tracking.kt` split into `ui/screens/tracking/` (`Liquids.kt`, `History.kt`, `Weight.kt`, `Common.kt`). Public names `LiquidsScreen`, `HistoryScreen`, `WeightScreen` unchanged.
- `Insights.kt` (screen) + `insights/Sections.kt` (`GapGrid`, `MealTiming`).
- `SmartTargetScreen.kt` rewritten in place.
- All strings in `strings_g.xml` (keys `g_*`, one plural).
- A11y: spoken summaries on bar/line/timing/gap charts, merged semantics on rows/cards, headings, 48dp targets (calendar cells, goal links, delete buttons), selected state on calendar days, content descriptions on arrows and icon buttons.
- Type: only `U.t.*` roles; no literal sp. Colour: `c.chiInk` for red text; no `Color(0x..)` (the Smart target banner still uses `Color.White` on `c.leaf`).
- State: dialogs (goal, note, copy, edit/reset smart target) are `rememberSaveable`.
- Perf: Liquids, History, Weight, Insights and Smart target are `LazyColumn` with keys, contentType and `animateItem()`. Aggregations are in `remember(...)`. The History calendar computes the month's kcal once.
- UX: Undo snackbar + haptic for water add, weigh-in delete, cheat day mark/unmark, copy day, smart target use/reset/manual. Empty states: History day, Weight (opens `QuickAddRequest(mode="WEIGHT")`), Insights with fewer than 2 logged days (opens Quick add). PDF report success/failure goes to the snackbar.
- Fixed a bug: marking a cheat day through the old note dialog overwrote the note on dismiss. Marking is now one tap with Undo, and "Add or edit note" is a separate button.
- Removed the "Undo -250 ml" water tile (the snackbar Undo replaces it).
- Goal weight now validated to 20-300 kg; manual smart target shows a message if outside 1000-6000.
- Motion: the meal-timing canvas fades/grows in once via `motionTween`.

## Not done
- Bars in `BarChart` and the line in `LineChart` do not animate in. They live in `Widgets2.kt` (group 3). Group 3 could add a one-time reveal.
- `entriesBetween` was not available, so I use `entriesOn` per day inside `remember`.
- Build was clean (assembleDebug) at the end.

## Requests for other owners
- Group 3 (`Widgets2.kt`): `BarChart` / `LineChart` should animate in once and use `U.t.caption` for labels (currently 10sp). `StatusBadge` should use `chiInk`/`blueInk`.
- Group 4 (`QuickAdd.kt`): `LiquidTile` has 10.5sp/12sp fixed text. `SmallSecondary` (PlateBuilder.kt) uses a literal 14sp.
- Group 3 (`QuickLook.kt`): `SourceRow` is used by my screens; keep its signature.
