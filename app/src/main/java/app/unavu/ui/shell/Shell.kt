package app.unavu.ui.shell

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import app.unavu.R
import app.unavu.data.Meal
import app.unavu.data.Store
import app.unavu.data.today
import app.unavu.data.useCombo
import app.unavu.ui.AppViewModel
import app.unavu.ui.Feedback
import app.unavu.ui.Nav
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Tab
import app.unavu.ui.screens.AboutScreen
import app.unavu.ui.screens.AboutYouScreen
import app.unavu.ui.screens.AppearanceScreen
import app.unavu.ui.screens.BackupScreen
import app.unavu.ui.screens.FoodDetailScreen
import app.unavu.ui.screens.GoalsScreen
import app.unavu.ui.screens.HistoryScreen
import app.unavu.ui.screens.InsightsScreen
import app.unavu.ui.screens.LiquidsScreen
import app.unavu.ui.screens.MealsScreen
import app.unavu.ui.screens.MyFoodEditorScreen
import app.unavu.ui.screens.MyFoodsScreen
import app.unavu.ui.screens.PlateBuilderScreen
import app.unavu.ui.screens.PortionsScreen
import app.unavu.ui.screens.ProfileScreen
import app.unavu.ui.screens.QuickAddSheet
import app.unavu.ui.screens.RecipeEditorScreen
import app.unavu.ui.screens.RemindersScreen
import app.unavu.ui.screens.SettingsScreen
import app.unavu.ui.screens.SmartTargetScreen
import app.unavu.ui.screens.TargetsScreen
import app.unavu.ui.screens.TodayScreen
import app.unavu.ui.screens.WeightScreen
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween

private data class Dest(val page: Page?, val tab: Tab, val depth: Int)

/** First-run flow (step + draft profile survive rotation and process death via the ViewModel). */
@Composable
fun OnboardingHost(store: Store, vm: AppViewModel) {
    val step by vm.onboardStep.collectAsStateWithLifecycle()
    val profile by vm.draftProfile.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val reduced = LocalReducedMotion.current
        val slide = with(LocalDensity.current) { 36.dp.roundToPx() }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                if (reduced) EnterTransition.None togetherWith ExitTransition.None
                else (fadeIn(motionTween(false, Motion.LONG)) + slideInHorizontally(motionTween(false, Motion.LONG)) { dir * slide }) togetherWith
                    (fadeOut(motionTween(false, Motion.SHORT, Motion.EmphasizedAccelerate)) + slideOutHorizontally(motionTween(false, Motion.SHORT, Motion.EmphasizedAccelerate)) { -dir * slide })
            },
            label = "onboardStep",
        ) { s ->
            if (s == 0) {
                AboutYouScreen(profile) { vm.setDraftProfile(it); vm.setOnboardStep(1) }
            } else {
                BackHandler { vm.setOnboardStep(0) }
                TargetsScreen(profile, onBack = { vm.setOnboardStep(0) }) { store.update { it.copy(profile = profile) } }
            }
        }
    }
}

/** Handles reminder / shortcut / widget taps (the action string arrives in the launch intent). */
@Composable
private fun ActionEffect(nav: Nav, action: String?, consumed: () -> Unit) {
    LaunchedEffect(action) {
        if (action == null) return@LaunchedEffect
        nav.stack.clear(); nav.tab.value = Tab.TODAY; nav.date.value = today()
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
}

@Composable
fun Main(nav: Nav, feedback: Feedback, action: String?, consumed: () -> Unit) {
    val tab = nav.tab.value
    val page = nav.stack.lastOrNull()
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    ActionEffect(nav, action, consumed)
    LaunchedEffect(tab, page) { feedback.host.currentSnackbarData?.dismiss() }

    // predictive back on pushed pages: the page shrinks with the gesture, then eases back while the pop slides
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = page != null) { progress ->
        try {
            progress.collect { backProgress.snapTo(it.progress) }
            nav.pop()
        } finally {
            // commit or cancel: ease the preview back in step with the slide (never a snap)
            scope.launch { backProgress.animateTo(0f, motionTween(reduced, Motion.LONG, Motion.Emphasized)) }
        }
    }
    // root tabs: plain back to Today, no preview to snap
    BackHandler(enabled = page == null && tab != Tab.TODAY) { nav.tab.value = Tab.TODAY }

    // the bar overlays the content; tab pages reserve its height, pushed pages reserve only the nav-bar inset
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    var barPx by remember { mutableIntStateOf(0) }
    val barHeight = with(density) { if (barPx > 0) barPx.toDp() else BarHeight + navBottom.toDp() }
    val barHidden by animateFloatAsState(
        if (page == null) 0f else 1f,
        motionTween(if (page == null) Motion.MEDIUM else Motion.SHORT, if (page == null) Motion.EmphasizedDecelerate else Motion.EmphasizedAccelerate),
        label = "barHide",
    )

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(
            Modifier.fillMaxSize().statusBarsPadding().graphicsLayer {
                val bp = backProgress.value
                if (!reduced && bp > 0f) {
                    val s = 1f - 0.1f * bp
                    scaleX = s; scaleY = s; translationX = bp * 24.dp.toPx()
                }
            },
        ) {
            AnimatedContent(
                targetState = Dest(page, tab, nav.stack.size),
                transitionSpec = {
                    when {
                        reduced -> EnterTransition.None togetherWith ExitTransition.None
                        // tab change: fade-through
                        targetState.depth == 0 && initialState.depth == 0 ->
                            (fadeIn(motionTween(false, Motion.FADE_THROUGH, delayMs = Motion.XSHORT)) +
                                scaleIn(motionTween(false, Motion.FADE_THROUGH, delayMs = Motion.XSHORT), initialScale = 0.96f)) togetherWith
                                fadeOut(motionTween(false, Motion.XSHORT, Motion.Standard))
                        // push: the new page slides fully in from the right over the old one, which drifts 30% left; pop reverses
                        else -> {
                            val push = targetState.depth > initialState.depth
                            val spec = motionTween<IntOffset>(false, Motion.LONG, Motion.Emphasized)
                            if (push) ContentTransform(
                                slideInHorizontally(spec) { it },
                                slideOutHorizontally(spec) { -it * 3 / 10 },
                                targetContentZIndex = 1f,
                            ) else ContentTransform(
                                slideInHorizontally(spec) { -it * 3 / 10 },
                                slideOutHorizontally(spec) { it },
                                targetContentZIndex = -1f,
                            )
                        }
                    }.using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                contentKey = { Triple(it.page, if (it.page == null) it.tab else null, it.depth) },
                label = "pages",
            ) { d ->
                // opaque, fixed-size page: nothing underneath shows through and nothing resizes mid-slide
                Box(
                    Modifier.fillMaxSize().background(U.c.bg)
                        .then(if (d.page == null) Modifier.padding(bottom = barHeight) else Modifier.navigationBarsPadding()),
                ) {
                    when (val p = d.page) {
                        null -> when (d.tab) {
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
                        is Page.Plate -> PlateBuilderScreen(p)
                        is Page.FoodDetail -> FoodDetailScreen(p)
                        is Page.Recipe -> RecipeEditorScreen(p)
                        Page.MyFoods -> MyFoodsScreen()
                        is Page.MyFood -> MyFoodEditorScreen(p)
                    }
                }
            }
        }
        // bottom bar: overlaid and only translated, so the page never relayouts while it hides or shows
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { if (it.height > 0) barPx = it.height }
                .graphicsLayer { translationY = barHidden * size.height },
        ) {
            if (barHidden < 1f) BottomBar(tab, onTab = { nav.tab.value = it }, onAdd = { nav.quickAdd.value = QuickAddRequest() })
        }
        // snackbars sit just above the bar (or the nav-bar inset on pushed pages)
        SnackbarHost(
            feedback.host,
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = if (page == null) barHeight else with(density) { navBottom.toDp() })
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) { data ->
            val c = U.c
            val shape = RoundedCornerShape(14.dp)
            Snackbar(data, modifier = Modifier.border(1.dp, c.line, shape), shape = shape, containerColor = c.sf, contentColor = c.ink, actionColor = c.accInk)
        }
        // status-bar scrim: scrolled content never shows under the clock and battery
        Box(Modifier.align(Alignment.TopStart).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(U.c.bg))
    }

    nav.quickAdd.value?.let { req -> QuickAddSheet(req) { nav.quickAdd.value = null } }
}

/** Bottom bar height above the nav-bar inset: 1dp divider + 64dp tab row. */
private val BarHeight = 65.dp

/** Shown when the data file existed but could not be read (a copy was kept; nothing was overwritten). */
@Composable
fun LoadIssueDialog(store: Store, nav: Nav?) {
    val issue by store.loadIssue.collectAsStateWithLifecycle()
    val i = issue ?: return
    AlertDialog(
        onDismissRequest = { store.clearLoadIssue() },
        title = { Text(stringResource(R.string.b_load_issue_title), style = U.t.title) },
        text = { Text(stringResource(R.string.b_load_issue_body, i.keptCopyPath), style = U.t.body) },
        confirmButton = {
            TextButton(onClick = { store.clearLoadIssue(); nav?.push(Page.Backup) }) { Text(stringResource(R.string.b_load_issue_restore), style = U.t.label) }
        },
        dismissButton = {
            TextButton(onClick = { store.clearLoadIssue() }) { Text(stringResource(R.string.b_load_issue_ok), style = U.t.label) }
        },
        containerColor = U.c.sf, titleContentColor = U.c.ink, textContentColor = U.c.ink, tonalElevation = 0.dp,
    )
}
