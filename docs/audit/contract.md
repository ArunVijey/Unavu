# Round 1: shared contract for the 10 parallel agents

Read `docs/audit/round-1.md` (the defect list) and this file before you start. **All 10 agents work at the same time in the same checkout, `D:/T3 Code/unavu`, on branch `audit-loop`.** The rules below keep you from breaking each other:

1. **Only edit the files you own** (listed in your task). Never edit, revert, reformat, `git checkout`, `git stash` or `git reset` anyone else's files. If you need a change in a file you don't own, write it under "Requests for other owners" in your notes file.
2. **The shared APIs below already exist as working skeletons** (commit "shared API skeleton"). Code against them by name. Their owners replace the bodies with the full behaviour, but the names and signatures don't change. **Don't create stubs or duplicate declarations.**
3. **Strings:** put every user-visible string you move or add in your own file, `app/src/main/res/values/strings_<letter>.xml` (e.g. `strings_e.xml`), and prefix every key with your letter (`e_today_title`). Never edit `strings.xml`.
4. **Building:** Git Bash: `cd "/d/T3 Code/unavu" && export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug testDebugUnitTest --console=plain`. Other agents build at the same time, so Gradle may wait on a lock; that's fine. If the build fails **only** in files you don't own (someone is mid-edit), wait a minute and retry. Don't fix their files. **Your own files must compile** before you finish.
5. **Committing:** commit only your own paths, e.g. `git add <your files> && git commit -m "..." -- <your files>`. Never `git add -A` or `git commit -a`. If `index.lock` exists, wait a few seconds and retry. End each message with `Co-Authored-By: GPT-6 Luna <noreply@openai.com>`. Don't push. Don't change the git identity. Don't touch `../signing`.
6. Write `docs/audit/notes-<letter>.md`: what you changed, what you couldn't do, and requests for other owners.
7. Keep the visual identity: palette, fonts, sketch mode (`U.sketch`), drawn icons, turmeric FAB, Tanglish parser. The JSON data format stays backward compatible (only add fields that have defaults). Tooling versions are fixed (AGP 9.2.1, Kotlin 2.3.20, compileSdk 37, Compose BOM 2026.08.00).
8. Write source files as UTF-8 (PowerShell 5.1 defaults to ANSI, so pass `-Encoding utf8` or use your edit tool). Prefer Git Bash.
9. Searches: stay inside `D:/T3 Code/unavu`. Don't search its parent folders.

## Shared APIs (names are fixed)

### Theme: owner C (`ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/theme/Motion.kt`)
```kotlin
// New colour tokens added to AppColors (every theme):
val chiInk: Color   // red text that passes 4.5:1 on chiS, sf and bg
val blueInk: Color  // blue text that passes 4.5:1 on blueS, sf and bg
val hint: Color     // placeholder text that passes 4.5:1 on bg, sf and sf2
// Text on the accent is always U.c.onAcc. Accent-coloured text on bg/sf is U.c.accInk.

// Type roles: U.t.<role> returns a TextStyle that already has the app font, line height and Excalifont adjustments
data class AppType(
    val display: TextStyle,   // 34sp  big numbers
    val headline: TextStyle,  // 26sp  screen titles
    val title: TextStyle,     // 18sp  card titles
    val titleS: TextStyle,    // 16sp  row titles
    val body: TextStyle,      // 15sp
    val bodyS: TextStyle,     // 14sp  secondary body
    val label: TextStyle,     // 13sp  chips, buttons-small, badges
    val caption: TextStyle,   // 12sp  minimum size for any text
)
// accessed as U.t.body   (object U gets `val t: AppType @Composable get()`)

// Motion (ui/theme/Motion.kt)
object Motion {
    const val SHORT = 150; const val MEDIUM = 250; const val LONG = 300
    val Standard: Easing; val Emphasized: Easing; val EmphasizedDecelerate: Easing; val EmphasizedAccelerate: Easing
}
val LocalReducedMotion: ProvidableCompositionLocal<Boolean>  // true when the system animator scale is 0. Provided inside AppTheme.
@Composable fun <T> motionTween(durationMs: Int = Motion.MEDIUM, easing: Easing = Motion.EmphasizedDecelerate): FiniteAnimationSpec<T> // snap() when reduced motion is on
```

### Feedback / snackbar: owner B (`ui/Feedback.kt`, hosted in MainActivity)
```kotlin
class Feedback { fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) }
val LocalFeedback: ProvidableCompositionLocal<Feedback?>   // use LocalFeedback.current?.show(...)
// Undo pattern for screens:  val before = store.data.value; store.deleteEntry(id); feedback.show(msg, undoLabel) { store.restoreSnapshot(before) }
// store.restoreSnapshot(before) exists for exactly this
```

### Store: owner A (`data/Store.kt`)
```kotlin
fun Store.restoreSnapshot(snapshot: AppData)              // undo: put back an earlier AppData without re-running migration side effects
val Store.loadIssue: StateFlow<LoadIssue?>                 // non-null when the data file was unreadable at start
data class LoadIssue(val keptCopyPath: String)
fun Store.clearLoadIssue()
fun Store.flush()                                          // write pending changes now (MainActivity calls it in onStop)
// All existing Store.* action functions keep their names and signatures.
```

### Shared components: owner D (`ui/Components.kt`, `ui/Widgets2.kt`)
Existing signatures stay. New optional params (defaults keep old call sites compiling):
```kotlin
ArrowButton(dir, modifier, contentDescription: String? = null /* null → localized default per dir */, onClick)
IconCircle(text, modifier, contentDescription: String? = null, onClick)
Fab(modifier, size, contentDescription: String? = null /* default "Add food" */, onClick)
Pill(text, selected, modifier, onClick)          // 48dp min, selectable semantics
Segmented(options, selected, modifier, onSelect) // 48dp min, Role.Tab + selected semantics
SectionLabel(text, modifier)                     // heading() semantics
TopBar(title, onBack, action)                    // title has heading() semantics
Track(fraction, color, modifier, height)         // animated
Modifier.pressScale(): Modifier                  // 0.97 scale on press, off when reduced motion is on
```

### App shell: owner B (`MainActivity.kt`, `ui/Nav.kt`, `ui/AppViewModel.kt`)
`Nav` keeps its public API (`stack`, `tab`, `date`, `quickAdd`, `push`, `pop`, `popTo`, `home`, `store`). It becomes saveable internally. `LocalNav` / `LocalData` keep their names.
