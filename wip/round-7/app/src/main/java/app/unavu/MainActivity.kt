package app.unavu

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHostState
import android.view.animation.PathInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unavu.data.AppData
import app.unavu.data.Backup
import app.unavu.data.Settings
import app.unavu.data.Smart
import app.unavu.data.Store
import app.unavu.ui.AppViewModel
import app.unavu.ui.Feedback
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.LocalNav
import app.unavu.ui.Nav
import app.unavu.ui.shell.LoadIssueDialog
import app.unavu.ui.shell.Main
import app.unavu.ui.shell.OnboardingHost
import app.unavu.ui.theme.AppTheme
import app.unavu.ui.theme.U
import app.unavu.ui.theme.applySystemBars
import app.unavu.ui.theme.syncSystemBarIcons
import app.unavu.ui.theme.paperDots
import app.unavu.widget.Widgets
import app.unavu.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope

class UnavuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scheduler.ensureChannel(this)
        // Load the data + food database off the main thread, in parallel with the first screen starting up.
        @OptIn(FlowPreview::class)
        MainScope().launch {
            val store = withContext(Dispatchers.Default) { Store.get(this@UnavuApp) }
            // keep home-screen widgets in step with the data (debounced so a burst of edits = one refresh)
            store.data.drop(1).debounce(1500).collect { Widgets.refresh(this@UnavuApp) }
        }
    }
}

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_ACTION = "unavu_action" }

    private val vm: AppViewModel by viewModels()
    private val pendingAction = mutableStateOf<String?>(null)

    /** The saved appearance settings, or null while the store is still loading. */
    private fun savedSettings(): Settings? = Store.peek()?.let { it.data.value.settings }

    override fun onCreate(savedInstanceState: Bundle?) {
        // system splash stays up until the store has loaded (capped, so a failed load can't hang the launch)
        val splash = installSplashScreen()
        val t0 = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition { Store.peek() == null && SystemClock.elapsedRealtime() - t0 < 4000 }
        // brand mark scales to 1.1 while the splash fades out (skipped under reduced motion)
        val motionOff = try { android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { false }
        splash.setOnExitAnimationListener { provider ->
            // the splash library has just re-applied the XML (phone-mode) bar style: the app's own wins again
            applySystemBars(savedSettings())
            val done = {
                provider.remove()
                // remove() re-applies the XML theme's icon flags once more, so restore the app's after it
                applySystemBars(savedSettings())
            }
            if (motionOff) { done(); return@setOnExitAnimationListener }
            val easing = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
            provider.iconView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(SPLASH_EXIT_MS).setInterpolator(easing).start()
            // the splash background follows the phone's mode; fading it (not cutting) keeps a Light app on a dark phone
            // (or the reverse) from flashing the other palette
            provider.view.animate().alpha(0f).setDuration(SPLASH_EXIT_MS).setInterpolator(easing).withEndAction { done() }.start()
        }
        super.onCreate(savedInstanceState)
        // edge-to-edge with the APP theme's bar style from the first frame (the phone's mode only until the store loads)
        applySystemBars(savedSettings())
        // only the first launch reads the intent action; a recreate (rotation) must not replay it
        if (savedInstanceState == null) pendingAction.value = intent?.getStringExtra(EXTRA_ACTION)
        setContent {
            var loaded by remember { mutableStateOf(Store.peek()) }
            LaunchedEffect(Unit) { if (loaded == null) loaded = withContext(Dispatchers.Default) { Store.get(this@MainActivity) } }
            val store = loaded ?: return@setContent // the splash screen covers this moment
            // Keep the State object, don't read it here: reading the data at this level would recompose the whole app
            // on every write. Only the appearance settings and "onboarded" are read here, each as its own derived value.
            val data = store.data.collectAsStateWithLifecycle()
            val settings by remember(data) { derivedStateOf { data.value.settings } }
            val onboarded by remember(data) { derivedStateOf { data.value.profile != null } }
            val nav = remember(store) { vm.nav(store) }
            val scope = rememberCoroutineScope()
            val feedback = remember { Feedback(SnackbarHostState(), scope) }
            AppTheme(settings) {
                ProvideAppLocals(nav, data, feedback) {
                    Box(Modifier.fillMaxSize().background(U.c.bg).paperDots(U.c.line)) {
                        if (!onboarded) OnboardingHost(store, vm)
                        else Main(nav, feedback, pendingAction.value) { pendingAction.value = null }
                    }
                    LoadIssueDialog(store, nav)
                }
            }
            LaunchedEffect(Unit) {
                Scheduler.scheduleReminders(this@MainActivity)
                Scheduler.scheduleBackup(this@MainActivity)
                if (Backup.isDue(store.data.value)) withContext(Dispatchers.IO) { Backup.run(this@MainActivity) }
                // self-correcting target: weekly re-estimate
                val checked = Smart.weeklyCheck(store.data.value)
                if (checked != store.data.value.smart) store.update { it.copy(smart = checked) }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // back from a sheet, dialog, the shade or another app: re-check the bar icons against the app theme (cheap no-op
        // when they already match)
        if (hasFocus) syncSystemBarIcons(savedSettings())
    }

    override fun onStop() {
        Store.peek()?.flush()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction.value = intent.getStringExtra(EXTRA_ACTION)
    }
}

private const val SPLASH_EXIT_MS = 250L

/**
 * Provides nav, data and feedback. Only this small scope reads the data State, and [LocalData] is a dynamic local:
 * a write recomposes just the composables that read `LocalData.current`, while [content] itself is skipped.
 */
@Composable
private fun ProvideAppLocals(nav: Nav, data: State<AppData>, feedback: Feedback, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNav provides nav, LocalData provides data.value, LocalFeedback provides feedback, content = content)
}
