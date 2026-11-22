# Notes: Group 2 (app shell + theme, letter b)

## Done
- Gradle: core-splashscreen 1.0.1, lifecycle-viewmodel-compose 2.10.0.
- Nav: Page/Tab/QuickAddRequest are @Serializable; `Nav(store, restored: String?)` with `saveState()`/`stateFlow()`. Public API unchanged. `AppViewModel` (SavedStateHandle JSON) keeps nav, onboarding step and draft profile across rotation/process death. A recreated activity no longer replays the launch intent action.
- Splash: `Theme.Unavu.Starting` (activity theme) -> `installSplashScreen()`, kept until `Store.peek() != null` (4 s cap). Day/night windowBackground (#FBF8F2 / #141311) via colors.xml + values-night. Blank-box branch removed.
- Manifest re-saved as UTF-8; widget/tile labels moved to strings_b.xml; `enableOnBackInvokedCallback=true`. Monochrome icon already existed.
- Shell (`ui/shell/`): `Main` (AnimatedContent: shared-axis X push/pop 300 ms, 120 ms fade-through tabs, instant under reduced motion, PredictiveBackHandler), `BottomBar` (selectableGroup, Role.Tab, ripple, animated indicator, haptic, heightIn(min=72dp), caption type), `OnboardingHost`, token-styled SnackbarHost above the bar, `LoadIssueDialog` (Restore from backup -> Page.Backup).
- `LocalFeedback` provided; `LocalData` is now static; `store.flush()` in onStop.
- Theme: real chiInk/blueInk/hint for all palettes; light leafInk/turInk darkened to pass 4.5:1 on their soft bgs; `contrast(a,b)`; `withAccent` uses real WCAG ratios (accInk is pushed toward black/white until it passes on bg, sf and accS); full M3 colorScheme from tokens; M3 Typography from our roles; the x1.14 fontScale hack is gone; `LocalAppType` (Excalifont +1sp, +2sp line height) and `TextStyle.forTamil()` in Type.kt; `LocalReducedMotion` from ANIMATOR_DURATION_SCALE via ContentObserver (`rememberReducedMotion()` in Motion.kt).
- `test/.../ui/ContrastTest.kt`: all text/bg pairs x 3 palettes x 8 picker swatches >= 4.5.

## Not done / unverified
- The full build was blocked by other groups' in-progress files while I worked, so ContrastTest was not confirmed green by gradle at the time of writing (values were checked with a Python WCAG script).
- Manifest: no receiver actions added (group 1's notes had none when I looked).
- Per-tab scroll/state is not kept across tab switches (AnimatedContent disposes the outgoing tab); screens should use rememberSaveable.

## Requests for other owners
- Group 6: Switch/rows etc. unaffected. Please keep `AccentPicker` PRESETS in sync with ContrastTest's swatch list.
- Everyone: `Tab.label` (English) still exists; the bar uses strings_b.
