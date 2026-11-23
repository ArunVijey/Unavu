# Round 7: agent A notes (plate and food)

Files changed: `ui/screens/FoodDetail.kt`, `ui/screens/PlateBuilder.kt`, `ui/screens/Customise.kt`, `ui/screens/add/AddParts.kt`, `ui/Widgets2.kt`, `ui/Buttons.kt`. New file: `res/values/strings_r7a.xml`. I did not touch `Selection.kt`, `Components.kt` or `strings_f.xml`; they needed no changes.

## Fixed

- **#1 Food detail stepper.** The private `PortionStepper`/`StepBtn` pair is gone. Food detail now uses the shared `Stepper(qty, step, itemName = food.name, slotWidth = 56.dp)`, which has a fixed-width value slot, so "+" always gets its 48 dp. The label ("pieces ≈ 130 g") sits in `Modifier.weight(1f).padding(start = 10.dp)` with `maxLines = 2` and an ellipsis. Customise, Food detail and the plate rows now share one stepper component.
- **#3 State survives rotation and process death.**
  - Plate items use `rememberSaveable(saver = PlateItemsSaver)`. The saver JSON-encodes the list through `ComboItem`, which is `@Serializable` and has the same fields as `NewItem`, so no data-class change was needed.
  - The plate's "unchanged" baseline is saveable too.
  - The picked choices in Food detail and the Customise sheet were plain `remember`. They now use `rememberSaveable(stateSaver = ChoicesSaver)`, defined in `Customise.kt`.
- **#6 Plate discard prompt.**
  - The plate is dirty when its items differ from the baseline. The baseline is an empty plate, the combo being edited, or the plate as last saved as a combo, and the plate stops being dirty once it is logged.
  - While dirty, `BackHandler` and the TopBar arrow show a "Discard this plate?" dialog. It tells you how many items haven't been logged (plural resource) and offers **Keep editing** / **Discard**. In combo mode the dialog is "Discard changes?".
- **#13 Plate list rows.** Each row now shows the name with "2 pieces · 130 kcal" underneath. The shared `Stepper` on the right shows the quantity being stepped (not kcal) in a fixed 48 dp slot, so the column no longer jitters. At the minimum, "−" removes the item (`onBelowMin`), and TalkBack says "Remove Idli". I took `pressScale` off the row, because pressing a stepper button made the whole row shrink.
- **#16 Button casing.** The buttons now read "Log lunch", "Pick a meal" and "Save combo", all from resources. Combo mode now uses the same `AddButton` as the log action (the old ALL-CAPS `AddBtn` is deleted; it had no other callers).
- **#17 Instructions are no longer titles.** `SectionTitle(text, modifier, hint: String? = null)` gained an optional caption-style hint that has no heading semantics, and it stays compatible with existing callers. Changes:
  - The tiles are now "Quick picks", with the hint "Tap to add · tap again for more · hold to customise".
  - Search results are "Results", with the hint "Tap to add · hold for details".
  - The plate list is "On the plate", with the hint "Tap an item to customise it", or "Nothing yet. Tap a food above to add it." when the plate is empty.
  - Food detail's nutrients card is "Nutrients", with a hint.
- **#22 Food detail stepper labels.** These come from the shared `Stepper` with `itemName`: "Decrease Idli" / "Increase Idli" (`f_decrease`/`f_increase`). The stepper's value slot now has `contentDescription = value` and a polite live region, so TalkBack reads the new amount after each tap. Before, the slot was cleared to nothing.
- **#23 Landscape plate.**
  - The quick-pick tiles are an adaptive grid: as many ~110 dp columns as fit (3 to 6), with short last rows padded so tiles keep the same width.
  - When the window is shorter than 480 dp, the footer collapses to a single row (total on the left, main action on the right) and the secondary Save is hidden.
  - "Save combo" is disabled while the plate is empty, in every orientation. `SmallSecondary` gained `enabled: Boolean = true`.
  - I used a chunked grid rather than `LazyVerticalGrid` because the tiles live inside the page's `verticalScroll` column, and a lazy grid can't be nested there.
- **#25 Hard-coded strings.** Every user-visible literal in FoodDetail, PlateBuilder, Customise, AddParts (`ParsedRow` now uses `f_qty_line`) and the BarChart description ("target …") has moved to `strings_r7a.xml`. Nothing literal is left except unit symbols (g/ml) and food/data text from the data layer. `meal!!` is replaced by smart-cast locals.
- **#29 Food detail title.** The TopBar now says "Food details". The header below it keeps the emoji, name, Tamil name and source badge, so the name isn't duplicated in the bar.
- **#4 (my files) Rounding.** Every gram value goes through `fmtNum`: macro rows, the donut's TalkBack description (it now says "protein 8.4 g", matching the screen), the per-100 line, nutrient rows, ingredient rows, the Customise P/C/F line and the plate macro tiles (which were `roundToInt` before).
- **#20 (my files) Thousands separators.** Every kcal or gram integer shown goes through `fmtInt`: donut, plate total, tiles, search results, plate rows, the Customise headline, quick-add parsed rows, and the BarChart description and selection chip.
- **#21 (my files) Gutter.** FoodDetail, PlateBuilder (page and footer) and the Customise sheet use `Space.gutter`, and FoodDetail's card spacing uses `Space.cardGap`.
- **Sketch polish.** Plate macro tiles, stepper rings, `SmallSecondary` and the big `AddButton` now draw with `edge(...)`, so they get the hand-drawn outline in Sketch like `PrimaryButton` does. `AddButton` no longer press-scales while disabled.

## Shared API additions (all source-compatible; existing callers unchanged)

- `ArrowButton(dir, modifier, contentDescription, enabled: Boolean = true, onClick)`: when disabled it draws at 38 % alpha, ignores taps and exposes disabled semantics. This is for C's #24.
- `ConfirmDialog(title, text, confirm, destructive, onDismiss, dismissLabel: String? = null, onConfirm)`: replaces "Cancel", e.g. with "Keep editing". This is for D's discard prompts (#6). The strings `R.string.r7a_discard` / `R.string.r7a_keep_editing` can be reused.
- `SectionTitle(text, modifier, hint: String? = null)` and `SmallSecondary(text, enabled = true, onClick)`.
- `Stepper`: same signature. At the minimum with `onBelowMin`, the "−" button's description becomes "Remove <item>", and the value slot is a live region.

## Not done / why

- **The UI test asserting that "Increase" is ≥ 48 dp (#1).** No `androidTest` source set is assigned to anyone, and `app/src/test/**` belongs to E. Structurally the `Stepper` gives each button a fixed `Modifier.size(48.dp)` and the value slot a fixed width, so the button can't be squeezed to zero any more.
- **No predictive-back preview while the plate is dirty.** While the plate has unsaved items, a plain `BackHandler` takes over from Shell's `PredictiveBackHandler`. The gesture therefore shows no preview, and on release it opens the discard dialog, which is what #6 asks for. A clean plate keeps the normal predictive back.

## Requests for other owners

- **C:** for #24, use `ArrowButton(Arrow.RIGHT, contentDescription = "Next week" (resource), enabled = sel.plusWeeks(1) <= …) { … }` in `TodayParts.kt`, and give the left arrow "Previous week".
- **D:** `ConfirmDialog(..., dismissLabel = stringResource(R.string.r7a_keep_editing))` is ready for the recipe and My food editors' "Discard?" dialogs.
- **B:** plate state also depends on the Shell's per-destination `SaveableStateProvider` (#3) for push/pop. Rotation and process death already work through the root registry and the restored `Nav` stack.
- **E (optional):** `Meal.label`, `FoodSource.label`, `Food.choiceSummary` and `unitLabel` still produce English text from the data layer. I left them alone as the contract says. Sentences that include a meal name use `label.lowercase()` ("Log lunch", "Add to lunch").
