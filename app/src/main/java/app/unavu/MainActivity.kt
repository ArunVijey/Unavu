package app.unavu

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unavu.data.Backup
import app.unavu.data.Smart
import app.unavu.data.Store
import app.unavu.ui.AppViewModel
import app.unavu.ui.Feedback
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.LocalNav
import app.unavu.ui.shell.LoadIssueDialog
import app.unavu.ui.shell.Main
import app.unavu.ui.shell.OnboardingHost
import app.unavu.ui.theme.AppTheme
import app.unavu.ui.theme.U
import app.unavu.ui.theme.paperDots
import app.unavu.widget.Widgets
import app.unavu.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnavuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scheduler.ensureChannel(this)
        // Load the data + food database off the main thread, in parallel with the first screen starting up.
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        kotlinx.coroutines.MainScope().launch {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // system splash stays up until the store has loaded (capped, so a failed load can't hang the launch)
        val splash = installSplashScreen()
        val t0 = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition { Store.peek() == null && SystemClock.elapsedRealtime() - t0 < 4000 }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // only the first launch reads the intent action; a recreate (rotation) must not replay it
        if (savedInstanceState == null) pendingAction.value = intent?.getStringExtra(EXTRA_ACTION)
        setContent {
            var loaded by remember { mutableStateOf(Store.peek()) }
            LaunchedEffect(Unit) { if (loaded == null) loaded = withContext(Dispatchers.Default) { Store.get(this@MainActivity) } }
            val store = loaded ?: return@setContent // the splash screen covers this moment
            val data by store.data.collectAsStateWithLifecycle()
            val nav = remember(store) { vm.nav(store) }
            val scope = rememberCoroutineScope()
            val feedback = remember { Feedback(SnackbarHostState(), scope) }
            AppTheme(data.settings) {
                CompositionLocalProvider(LocalNav provides nav, LocalData provides data, LocalFeedback provides feedback) {
                    Box(Modifier.fillMaxSize().background(U.c.bg).paperDots(U.c.line)) {
                        if (data.profile == null) OnboardingHost(store, vm)
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

    override fun onStop() {
        Store.peek()?.flush()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction.value = intent.getStringExtra(EXTRA_ACTION)
    }
}
