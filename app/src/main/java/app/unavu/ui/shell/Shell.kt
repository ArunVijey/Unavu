package app.unavu.ui.shell

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.unavu.R
import app.unavu.data.Meal
import app.unavu.data.Store
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

private data class Dest(val page: Page?, val tab: Tab, val depth: Int)

/** First-run flow (step + draft profile survive rotation and process death via the ViewModel). */
@Composable
fun OnboardingHost(store: Store, vm: AppViewModel) {
    val step by vm.onboardStep.collectAsStateWithLifecycle()
    val profile by vm.draftProfile.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (step == 0) {
            AboutYouScreen(profile) { vm.setDraftProfile(it); vm.setOnboardStep(1) }
        } else {
            BackHandler { vm.setOnboardStep(0) }
            TargetsScreen(profile, onBack = { vm.setOnboardStep(0) }) { store.update { it.copy(profile = profile) } }
        }
    }
}

/** Handles reminder / shortcut / widget taps (the action string arrives in the launch intent). */
@Composable
private fun ActionEffect(nav: Nav, action: String?, consumed: () -> Unit) {
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
}

@Composable
fun Main(nav: Nav, feedback: Feedback, action: String?, consumed: () -> Unit) {
    val tab = nav.tab.value
    val page = nav.stack.lastOrNull()
    val reduced = LocalReducedMotion.current

    ActionEffect(nav, action, consumed)

    // predictive back: the system shows the back preview; we pop when it commits
    PredictiveBackHandler(enabled = page != null || tab != Tab.TODAY) { progress ->
        progress.collect { }
        if (!nav.pop()) nav.tab.value = Tab.TODAY
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            val slidePx = with(androidx.compose.ui.platform.LocalDensity.current) { 36.dp.roundToPx() }
            AnimatedContent(
                targetState = Dest(page, tab, nav.stack.size),
                transitionSpec = {
                    when {
                        reduced -> EnterTransition.None togetherWith ExitTransition.None
                        // tab change: fade-through
                        targetState.depth == 0 && initialState.depth == 0 ->
                            (fadeIn(tween(210, delayMillis = 90, easing = Motion.EmphasizedDecelerate)) + androidx.compose.animation.scaleIn(tween(210, delayMillis = 90, easing = Motion.EmphasizedDecelerate), initialScale = 0.96f)) togetherWith fadeOut(tween(90, easing = Motion.Standard))
                        // push slides in from the right, pop slides back (shared axis X)
                        else -> {
                            val d = if (targetState.depth > initialState.depth) 1 else -1
                            ContentTransform(
                                slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { slidePx * d } + fadeIn(tween(Motion.LONG, easing = Motion.Standard)),
                                slideOutHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { -slidePx * d } + fadeOut(tween(Motion.LONG / 2, easing = Motion.Standard)),
                            )
                        }
                    }
                },
                contentKey = { Triple(it.page, if (it.page == null) it.tab else null, it.depth) },
                label = "pages",
            ) { d ->
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
            // snackbars sit just above the bottom bar
            SnackbarHost(feedback.host, Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 8.dp)) { data ->
                val c = U.c
                Snackbar(
                    data, shape = RoundedCornerShape(14.dp), containerColor = c.sf, contentColor = c.ink, actionColor = c.accInk,
                    modifier = Modifier.border(1.dp, c.line, RoundedCornerShape(14.dp)),
                )
            }
        }
        if (page == null) BottomBar(tab, onTab = { nav.tab.value = it }, onAdd = { nav.quickAdd.value = QuickAddRequest() }, addOpen = nav.quickAdd.value != null)
        else Box(Modifier.navigationBarsPadding())
    }

    nav.quickAdd.value?.let { req -> QuickAddSheet(req) { nav.quickAdd.value = null } }
}

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
