# Notes: Group 3 (d), shared components + Today

## Done
- Components.kt / Widgets2.kt: Pill/Segmented/MealPicker/PrimaryButton >=48dp with selectable semantics (RadioButton/Tab) and selectableGroup; ArrowButton/IconCircle/Fab/Stepper 48dp with default descriptions (strings_d); SectionLabel/TopBar heading(); AppCard/ListRow merged + Role.Button; Track animated (fills once, motionTween) with optional contentDescription + progress semantics; StatusBadge uses chiInk/blueInk; new SwitchRow (Role.Switch) and AppSwitch accepts null callback + haptics; pressScale() implemented (0.97, off for reduced motion, does not consume touches); NumberField/TextField got optional `error` (error() semantics); SearchField uses c.hint, 48dp clear button; dialogs use strings and saveable input; BarChart/LineChart take optional `contentDescription` (auto summary otherwise), bars animate in once.
- Today.kt: LazyColumn (keys, contentTypes, animateItem), text summary for TalkBack on the budget hero, animated kcal count, empty-day card, load-issue banner (opens Page.Backup), undo snackbars on delete/cheat/copy/glass/combo, rememberSaveable for all looks/dialogs (ids/enum names). Split into screens/today/ (TodayParts, TodaySheets). Removed dead keepFoodDb.
- QuickLook.kt: 48dp actions with Role.Button, reduced-motion aware, RemoveButton (48dp, described).

## Not done / caveats
- dayTitle() still returns hard-coded English "Today"/"Yesterday" (non-composable, used by others).
- Sketch.kt untouched. Week strip day cells are ~46dp wide (7 across), 56dp tall.
- Full build could not be verified end to end: mergeDebugResources fails on strings_f.xml (f_no_combos_body, f_no_recipes_body: invalid unicode escape), owned by group 4. compileDebugKotlin passed.

## Requests for other owners
- Group 4 (f): fix `\u`-style escapes in strings_f.xml lines 63/66.
- Callers can pass `contentDescription` to Track/BarChart/LineChart for better spoken summaries; SwitchRow is available for settings.
