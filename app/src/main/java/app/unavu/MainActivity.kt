package app.unavu

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unavu.data.Backup
import app.unavu.data.Smart
import app.unavu.data.useCombo
import app.unavu.ui.screens.InsightsScreen
import app.unavu.ui.screens.MyFoodEditorScreen
import app.unavu.ui.screens.MyFoodsScreen
import app.unavu.ui.screens.SmartTargetScreen
import app.unavu.widget.Widgets
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import app.unavu.data.Meal
import app.unavu.data.Profile
import app.unavu.data.Store
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.Fab
import app.unavu.ui.NavIcon
import app.unavu.ui.LocalData
import app.unavu.ui.LocalNav
import app.unavu.ui.Nav
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Tab
import app.unavu.ui.TopBar
import app.unavu.ui.screens.AboutScreen
import app.unavu.ui.screens.AboutYouScreen
import app.unavu.ui.screens.AppearanceScreen
import app.unavu.ui.screens.BackupScreen
import app.unavu.ui.screens.FoodDetailScreen
import app.unavu.ui.screens.GoalsScreen
import app.unavu.ui.screens.HistoryScreen
import app.unavu.ui.screens.LiquidsScreen
import app.unavu.ui.screens.MealsScreen
import app.unavu.ui.screens.PlateBuilderScreen
import app.unavu.ui.screens.PortionsScreen
import app.unavu.ui.screens.ProfileScreen
import app.unavu.ui.screens.QuickAddSheet
import app.unavu.ui.screens.RecipeEditorScreen
import app.unavu.ui.screens.RemindersScreen
import app.unavu.ui.screens.SettingsScreen
import app.unavu.ui.screens.TargetsScreen
import app.unavu.ui.screens.TodayScreen
import app.unavu.ui.screens.WeightScreen
import app.unavu.ui.theme.AppTheme
import app.unavu.ui.theme.U
import app.unavu.work.Scheduler
import kotlinx.coroutines.Dispatchers
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

    private val pendingAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingAction.value = intent?.getStringExtra(EXTRA_ACTION)
        setContent {
            // the store (and food DB) load in the background; show the plain background until ready
            var loaded by remember { mutableStateOf(Store.peek()) }
            LaunchedEffect(Unit) { if (loaded == null) loaded = withContext(Dispatchers.Default) { Store.get(this@MainActivity) } }
            val store = loaded
            if (store == null) {
                val dark = androidx.compose.foundation.isSystemInDarkTheme()
                Box(Modifier.fillMaxSize().background(if (dark) androidx.compose.ui.graphics.Color(0xFF000000) else androidx.compose.ui.graphics.Color(0xFFFBF8F2)))
                return@setContent
            }
            val data by store.data.collectAsStateWithLifecycle()
            val nav = remember { Nav(store) }
            AppTheme(data.settings) {
                CompositionLocalProvider(LocalNav provides nav, LocalData provides data) {
                    Box(Modifier.fillMaxSize().background(U.c.bg)) {
                        if (data.profile == null) Onboarding(store) else Main(nav, pendingAction.value) { pendingAction.value = null }
                    }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction.value = intent.getStringExtra(EXTRA_ACTION)
    }
}

@Composable
private fun Onboarding(store: Store) {
    var step by rememberSaveable { mutableStateOf(0) }
    var profile by remember { mutableStateOf(Profile()) }
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (step == 0) {
            AboutYouScreen(profile) { profile = it; step = 1 }
        } else {
            BackHandler { step = 0 }
            TargetsScreen(profile, onBack = { step = 0 }) { store.update { it.copy(profile = profile) } }
        }
    }
}

@Composable
private fun Main(nav: Nav, action: String?, consumed: () -> Unit) {
    val c = U.c
    val tab = nav.tab.value
    val page = nav.stack.lastOrNull()

    // reminder / shortcut taps
    LaunchedEffect(action) {
        if (action == null) return@LaunchedEffect
        nav.stack.clear(); nav.tab.value = Tab.TODAY; nav.date.value = app.unavu.data.today()
        val meal = Meal.entries.firstOrNull { it.name.equals(action, ignoreCase = true) }
        when {
            meal != null -> nav.quickAdd.value = QuickAddRequest(meal)
            action == "water" -> nav.quickAdd.value = QuickAddRequest(mode = "LIQUID")
            action == "weigh" -> nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT")
            action == "add" -> nav.quickAdd.value = QuickAddRequest()
            action.startsWith("combo:") -> {
                val combo = nav.store.data.value.combos.firstOrNull { it.id == action.removePrefix("combo:") }
                when {
                    combo == null -> Unit
                    combo.meal != null -> nav.store.useCombo(combo, combo.meal, nav.date.value)
                    else -> nav.quickAdd.value = QuickAddRequest()
                }
            }
        }
        consumed()
    }

    BackHandler(enabled = page != null || tab != Tab.TODAY) {
        if (!nav.pop()) nav.tab.value = Tab.TODAY
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (page) {
                null -> when (tab) {
                    Tab.TODAY -> TodayScreen()
                    Tab.INSIGHTS -> InsightsScreen()
                    Tab.MEALS -> MealsScreen()
                    Tab.SETTINGS -> SettingsScreen()
                }
                Page.Profile -> ProfileScreen()
                Page.Goals -> GoalsScreen()
                Page.Appearance -> AppearanceScreen()
                Page.Portions -> PortionsScreen()
                Page.Backup -> BackupScreen()
                Page.Reminders -> RemindersScreen()
                Page.About -> AboutScreen()
                Page.History -> HistoryScreen()
                Page.Liquids -> LiquidsScreen()
                Page.Weight -> WeightScreen()
                Page.SmartTarget -> SmartTargetScreen()
                is Page.Plate -> PlateBuilderScreen(page)
                is Page.FoodDetail -> FoodDetailScreen(page)
                is Page.Recipe -> RecipeEditorScreen(page)
                Page.MyFoods -> MyFoodsScreen()
                is Page.MyFood -> MyFoodEditorScreen(page)
            }
        }
        if (page == null) {
            Divider()
            Row(
                Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(start = 4.dp, end = 14.dp).height(78.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { t ->
                    val on = t == tab
                    val col = if (on) c.accInk else c.ink2
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                        ) { nav.tab.value = t },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // indicator line on top of the active tab
                        Box(Modifier.width(30.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (on) c.acc else androidx.compose.ui.graphics.Color.Transparent))
                        Spacer(Modifier.height(13.dp))
                        NavIcon(t.glyph, col, on, c.sf)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            t.label.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = col, maxLines = 1,
                        )
                    }
                }
                Fab(Modifier.padding(start = 6.dp), size = 58.dp) { nav.quickAdd.value = QuickAddRequest() }
            }
        } else {
            Box(Modifier.navigationBarsPadding())
        }
    }

    nav.quickAdd.value?.let { req -> QuickAddSheet(req) { nav.quickAdd.value = null } }
}
