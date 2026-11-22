# Notes: Group 1 (a): data, work, widget, tests

## Done
- **Data index**: `AppData.entriesOn(date)` and `entriesBetween(from, to)` use a per-instance lazy index (`entriesByDate`, not serialized). `Smart.loggedDays` uses it.
- **Store**:
  - A data file that exists but won't parse is copied to `unavu-data.corrupt-<ms>.json`.
  - Then `unavu-data.json.tmp` is tried, and `loadIssue` is set.
  - The bad file is not overwritten at start.
  - `Store.forFile(File)` makes a store for JVM tests; `get` and `peek` are unchanged.
- **Atomic save**: tmp, fsync, `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`, with a fallback to plain `REPLACE_EXISTING`. IO errors are logged, never thrown. The scope has a `CoroutineExceptionHandler`.
- **Writer**: one conflated channel, so the newest snapshot always wins. `flush()` writes synchronously on the calling thread.
- **restoreSnapshot**: refreshes custom foods and `nextId`. `useCombo` is now a single `update`.
- **Export/Report**:
  - Failures come back as `Result.failure(IllegalStateException(readable message))`.
  - `Backup.restore` validates first: it parses, rejects files from a newer version, and replaces nothing on failure.
- **@Immutable** is on all 13 model data classes.
- **work/**:
  - `ReminderReceiver` and `BootReceiver` use `goAsync` and a background thread.
  - Notification channel and texts are in `strings_a.xml`.
  - No post without `POST_NOTIFICATIONS`, and none if `areNotificationsEnabled()` is false.
  - Alarms stay inexact (`setAndAllowWhileIdle`) and `SecurityException` is caught.
  - `BackupWorker` retries up to 3 times on IO errors.
- **widget/**:
  - Strings come from `strings_a.xml`, and text is at least 12sp.
  - Contrast is about 14:1 for INK and about 6.4:1 for INK2 on the widget background.
  - Every pill is 48dp tall with a contentDescription, and each widget has a spoken summary.
  - There is a "set up your profile" state, and a snapshot that never throws.
  - The water tile and the add tile use `unlockAndRun`, with a state description on API 30+.
  - The add tile already used a PendingIntent on API 34+.
- **Tests** (JUnit4):
  - `StoreTest`: corrupt file kept, tmp fallback, atomic round trip, v1 to v2 migration, 1,000 updates then last write wins, `restoreSnapshot`, `useCombo`, `entriesOn`/`entriesBetween`.
  - `CalcSmartTest`: Calc math, Smart weekly gate and estimate gate.
  - `ParserEdgeTest`: Parser edge cases.
  - `SchedulerTest`: next reminder time, including across midnight.

## Could not do
- `./gradlew testDebugUnitTest` could not be completed for a while because of another group's resource error (`strings_f.xml` `f_no_combos_body`, invalid unicode escape). Tests were written but the last full run is noted in the final report.

## Manifest XML for group 2 (BootReceiver)
Replace the `BootReceiver` intent filter with:
```xml
<receiver android:name=".work.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        <action android:name="android.intent.action.TIME_SET" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
    </intent-filter>
</receiver>
```
(`exported="true"` is needed to receive these system broadcasts.) Existing `ReminderReceiver` stays `exported="false"`.
The widget labels need UTF-8 and `strings_b.xml` (group 2's job). I did not add new widget actions.

## Requests for other owners
- Group 2: the manifest block above. `MainActivity` should call `store.flush()` in `onStop` and may show `Store.loadIssue`.
- Screens that call `Backup.run` / `Backup.restore` / `Report.save` now get `Result.failure` with a user-readable `message`; show it in a snackbar.
