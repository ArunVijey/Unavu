# Notes: group 6 (letter `i`): settings, accent, onboarding

## Done
- `SettingsScreens.kt` removed and split into `ui/screens/settings/`: SettingsScreen, ProfileScreen, GoalsScreen, AppearanceScreen, PortionsScreen, BackupScreen, RemindersScreen, AboutScreen, plus SettingsCommon (SettingSwitchRow, SettingRadioRow, notificationsAllowed, findActivity). Package is still `app.unavu.ui.screens`; public composable names unchanged.
- A11y: switch rows are whole-row `toggleable(Role.Switch)`; theme and font are `selectableGroup` + `Role.RadioButton`; accent swatches are named, selectable, 72x48dp+ targets; the colour wheel has a spoken hue/saturation description; reminder switches carry "<label> reminder"; decorative emoji tiles are hidden from TalkBack; error() on onboarding fields.
- Type/colour: all `U.t.*` roles, no literal sp, no `Color(0x..)` except accent swatch values; red text uses `chiInk`; the hero cards use leafS/leafInk instead of white-on-leaf (white on the dark-mode leaf failed contrast).
- State: dialogs, editing ids, wheel hue/sat/value, split edits and onboarding fields use `rememberSaveable`.
- UX: backup/restore run off the main thread; results come through `LocalFeedback`; restore confirms, then offers Undo (`restoreSnapshot`); portion removal has Undo; meal-split save and details-saved snackbars; chip limit/minimum messages; haptics.
- Notifications: Reminders screen shows a warning card when notifications are off (re-checked on ON_RESUME), asks for POST_NOTIFICATIONS contextually, and after a permanent denial switches the button to "Open settings" (ACTION_APP_NOTIFICATION_SETTINGS). Settings home shows "notifications blocked" under Reminders.
- Onboarding: age 10-100, height 100-250, weight 20-300 with per-field errors and a reason line under the disabled button; "Step 1 of 2" / "Step 2 of 2"; ImeAction.Next/Done.
- All strings in `res/values/strings_i.xml` (with a plural for the portions count).

## Not done / caveats
- Settings lists are short, so no LazyColumn (the lists are static rows, not unbounded data).
- `AboutYouScreen` fields are saveable, but the step number and draft in MainActivity's onboarding live in group B's file.
- `ProfileScreen` draft `Profile` is `remember` (not saveable); only its text fields survive process death.
- Gradle build was blocked for a while by `strings_f.xml` (group 4) invalid unicode escapes; my files were compile-checked only once that cleared (see final report).

## Requests for other owners
- D: `Stepper` needs spoken +/- descriptions (used on Goals meal split and water); `ListRow` should merge descendants with Role.Button when clickable (Settings uses it everywhere).
- B: onboarding step and draft should be saveable in MainActivity; `data.settings.sketch` Segmented etc. depend on D's 48dp change.
