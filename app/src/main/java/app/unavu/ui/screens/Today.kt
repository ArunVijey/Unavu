package app.unavu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import app.unavu.ui.GlyphCircle
import app.unavu.ui.LineGlyph
import app.unavu.ui.PlusRingButton
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Combo
import app.unavu.data.ComboItem
import app.unavu.data.DATE
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.NewItem
import app.unavu.data.NutrientDef
import app.unavu.data.Smart
import app.unavu.data.addWater
import app.unavu.data.copyDay
import app.unavu.data.copyMeal
import app.unavu.data.deleteEntry
import app.unavu.data.deleteMeal
import app.unavu.data.entriesOn
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.liquidsMl
import app.unavu.data.newKey
import app.unavu.data.saveCombo
import app.unavu.data.setCheatDay
import app.unavu.data.setMealCheat
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.data.updateEntry
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.IconCircle
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.pressScale
import app.unavu.ui.screens.today.BudgetLook
import app.unavu.ui.screens.today.CopyMealDialog
import app.unavu.ui.screens.today.LegendDot
import app.unavu.ui.screens.today.MealLook
import app.unavu.ui.screens.today.NextUpCard
import app.unavu.ui.screens.today.NutrientChip
import app.unavu.ui.screens.today.NutrientLook
import app.unavu.ui.screens.today.WeekStrip
import app.unavu.ui.screens.today.edgeIfSketch
import app.unavu.ui.screens.today.mealItemText
import app.unavu.ui.screens.today.rememberUndo
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.U
import app.unavu.ui.theme.hachure
import app.unavu.ui.theme.motionTween
import app.unavu.ui.theme.roughArc
import app.unavu.ui.theme.roughRect
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun mealColors(c: AppColors): List<Color> = listOf(c.tur, c.leaf, c.m3, c.blue)

fun statusOf(d: app.unavu.data.NutrientDef, have: Double, target: Double): Status = if (d.limit) {
    if (have > target) Status.OVER else Status.OK
} else {
    if (have >= target * 0.9) Status.OK else Status.LOW
}

/** Whole numbers from 10 up (53.3 → 53), one decimal below 10 (2.4 mg iron). */
fun fmtNum(v: Double): String = if (v >= 10 || v % 1.0 == 0.0) "%,d".format(v.roundToInt()) else "%.1f".format(v)

fun dayTitle(date: String): String {
    val d = LocalDate.parse(date)
    return when (date) {
        today() -> "Today"
        LocalDate.now().minusDays(1).format(DATE) -> "Yesterday"
        else -> "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
    }
}

/** Fade + 12dp slide-up on first composition, staggered by [index]. Plays once per screen visit (tracked in [seen]). */
private fun Modifier.staggerIn(index: Int, seen: MutableSet<Int>): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val already = remember { reduced || index in seen }
    val a = remember { Animatable(if (already) 1f else 0f) }
    LaunchedEffect(Unit) {
        seen.add(index)
        if (a.value < 1f) {
            delay(40L * index.coerceAtMost(10))
            a.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate))
        }
    }
    val px = with(LocalDensity.current) { 12.dp.toPx() }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * px }
}

/** Compact litres: 0, 1.25, 2.5. */
private fun fmtL(ml: Int): String = "%.2f".format(ml / 1000.0).trimEnd('0').trimEnd('.')

@Composable
fun TodayScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val date = nav.date.value
    val t = data.targets() ?: return
    val undoable = rememberUndo(store)

    val entries = remember(data, date) { data.entriesOn(date) }
    val total = remember(entries) { entries.total() }
    val byMeal = remember(entries) { Meal.entries.associateWith { m -> entries.filter { it.meal == m } } }
    val eaten = total.kcal.roundToInt()
    val left = t.kcal - eaten
    val colors = mealColors(c)
    val cheatDay = data.isCheatDay(date)
    val loadIssue = store.loadIssue.collectAsState().value
    val haptic = LocalHapticFeedback.current
    val seen = remember { mutableSetOf<Int>() }
    val dayAlpha = remember { Animatable(1f) }
    val fadeSpec = motionTween<Float>(Motion.SHORT)
    val dayFade = Modifier.graphicsLayer { alpha = dayAlpha.value }
    val reducedMotion = LocalReducedMotion.current
    var firstDay by remember { mutableStateOf(true) }
    LaunchedEffect(date) {
        if (firstDay) { firstDay = false } else if (!reducedMotion) {
            dayAlpha.snapTo(0.2f); dayAlpha.animateTo(1f, tween(Motion.SHORT, easing = Motion.Standard))
        }
    }

    // Saveable UI state: ids / enum names, never objects.
    var nutrientKey by rememberSaveable { mutableStateOf<String?>(null) }
    var mealLook by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetLook by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var copyFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var comboFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var cheatNote by rememberSaveable { mutableStateOf(false) }
    fun mealOf(s: String?): Meal? = s?.let { runCatching { Meal.valueOf(it) }.getOrNull() }

    // Strings used inside non-composable lambdas.
    val sMsgDeletedItem = stringResource(R.string.d_msg_deleted_item)
    val sCheatOn = stringResource(R.string.d_msg_cheat_day_on)
    val sCheatOff = stringResource(R.string.d_msg_cheat_day_off)
    val sCheatMeal = stringResource(R.string.d_msg_cheat_meal)
    val sCopiedDay = stringResource(R.string.d_msg_copied_day)
    val sGlass = stringResource(R.string.d_glass_added)
    val sCombo = stringResource(R.string.d_msg_combo_saved)
    val sClearedFmt = stringResource(R.string.d_msg_deleted_meal)
    fun sMealCleared(m: Meal) = sClearedFmt.format(m.label)

    Column(Modifier.fillMaxSize()) {
        TopBar(dayTitle(date)) {
            if (date != today()) IconCircle("⟲", contentDescription = stringResource(R.string.d_back_to_today)) { nav.date.value = today() }
            GlyphCircle(LineGlyph.CALENDAR, stringResource(R.string.d_history)) { nav.push(Page.History) }
            Box {
                GlyphCircle(LineGlyph.MORE, stringResource(R.string.d_more)) { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = c.sf) {
                    DropdownMenuItem({ Text(stringResource(if (cheatDay) R.string.d_menu_unmark_cheat_day else R.string.d_menu_mark_cheat_day), style = U.t.body) }, {
                        menu = false
                        if (cheatDay) undoable(sCheatOff) { store.setCheatDay(date, false) } else cheatNote = true
                    })
                    if (date != today()) DropdownMenuItem({ Text(stringResource(R.string.d_menu_copy_day), style = U.t.body) }, {
                        menu = false
                        undoable(sCopiedDay) { store.copyDay(date, today()) }
                        nav.date.value = today()
                    })
                    DropdownMenuItem({ Text(stringResource(R.string.d_menu_log_weight), style = U.t.body) }, { menu = false; nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") })
                    DropdownMenuItem({ Text(stringResource(R.string.d_menu_liquids), style = U.t.body) }, { menu = false; nav.push(Page.Liquids) })
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "week", contentType = "week") {
                WeekStrip(data, date, Modifier.animateItem().staggerIn(0, seen)) { nav.date.value = it }
            }

            if (loadIssue != null) item(key = "loadIssue", contentType = "banner") {
                AppCard(Modifier.animateItem().then(dayFade), color = c.chiS, border = null, onClick = { nav.push(Page.Backup) }) {
                    Text(stringResource(R.string.d_load_issue_title), style = U.t.body, fontWeight = FontWeight.Bold, color = c.chiInk)
                    Text(stringResource(R.string.d_load_issue_sub), style = U.t.caption, color = c.ink2)
                }
            }

            data.smart.pendingBurn?.let { burn ->
                item(key = "smart", contentType = "banner") {
                    val newKcal = (burn + (data.profile?.goalKcal ?: 0)).coerceAtLeast(1200)
                    AppCard(Modifier.animateItem().then(dayFade), color = c.leafS, border = null, onClick = { nav.push(Page.SmartTarget) }) {
                        Text(stringResource(R.string.d_new_target, newKcal), style = U.t.body, fontWeight = FontWeight.Bold, color = c.leafInk)
                        Text(stringResource(R.string.d_new_target_sub, t.kcal), style = U.t.caption, color = c.ink2)
                    }
                }
            }
            if (cheatDay) item(key = "cheat", contentType = "banner") {
                AppCard(Modifier.animateItem().then(dayFade), color = c.turS, border = null) {
                    Text(stringResource(R.string.d_cheat_day_title, data.dayNotes[date]?.let { " · $it" } ?: ""), style = U.t.body, fontWeight = FontWeight.Bold, color = c.turInk)
                    Text(stringResource(R.string.d_cheat_day_sub), style = U.t.caption, color = c.ink2)
                }
            }

            // ---------- Budget hero ----------
            item(key = "hero", contentType = "hero") {
                BudgetHero(Modifier.animateItem().staggerIn(1, seen).then(dayFade), t.kcal, eaten, left, byMeal, colors, total, t, data.settings.homeChips, { budgetLook = true }, { nutrientKey = it.key })
            }

            item(key = "next", contentType = "next") {
                Box(Modifier.animateItem().staggerIn(2, seen).then(dayFade)) { NextUpCard(date, byMeal, left, total, t) }
            }

            // ---------- Liquids ----------
            item(key = "liquids", contentType = "liquids") {
                val (water, drinks) = data.liquidsMl(date)
                val goal = data.settings.waterGoalMl
                val liqSub = stringResource(R.string.d_liquids_sub, fmtL(water), fmtL(goal), drinks)
                AppCard(Modifier.animateItem().staggerIn(3, seen).then(dayFade)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) {}.clickable(role = Role.Button) { nav.push(Page.Liquids) },
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(stringResource(R.string.d_liquids_title, fmtL(water + drinks)), style = U.t.body, fontWeight = FontWeight.Bold)
                            Text(liqSub, style = U.t.caption, color = c.ink2)
                        }
                        ActionChip(stringResource(R.string.d_plus_glass), c.blueInk, c.blueS, stringResource(R.string.d_plus_glass_desc)) {
                            undoable(sGlass) { store.addWater(date, 250) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Track(water / goal.toFloat(), c.blue, height = 8.dp, contentDescription = liqSub)
                }
            }

            // ---------- Meals ----------
            item(key = "meals", contentType = "meals") {
                AppCard(Modifier.animateItem().staggerIn(4, seen).then(dayFade).animateContentSize(motionTween()), padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    Meal.entries.forEachIndexed { i, m ->
                        if (i > 0) Divider()
                        val list = byMeal.getValue(m)
                        val kcal = list.total().kcal.roundToInt()
                        val empty = list.isEmpty()
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = if (empty) 56.dp else 64.dp)
                                .animateContentSize(motionTween())
                                .semantics(mergeDescendants = true) {}
                                .clickable(role = Role.Button) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (empty) nav.push(Page.Plate(m, date)) else mealLook = m.name
                                }
                                .padding(vertical = if (empty) 4.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(if (empty) colors[i].copy(alpha = 0.45f) else colors[i]))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(m.label, style = U.t.titleS, color = if (empty) c.ink2 else c.ink)
                                    if (list.any { it.cheat }) StatusBadge(stringResource(R.string.d_cheat_meal), Status.LOW)
                                }
                                val sub = if (empty) stringResource(R.string.d_meal_not_logged) else list.joinToString(" · ") { e -> mealItemText(e) }
                                AnimatedContent(sub, transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) }, label = "mealSub") { txt ->
                                    Text(txt, style = U.t.bodyS, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                            if (empty) PlusRingButton(null)
                            else Text("$kcal", style = U.t.titleS, fontWeight = FontWeight.ExtraBold, color = c.ink)
                        }
                    }
                }
            }

            // ---------- Weight ----------
            item(key = "weight", contentType = "weight") {
                val lastW = data.weights.lastOrNull()
                val trend = remember(data.weights) { Smart.trend(data.weights).lastOrNull()?.second }
                AppCard(Modifier.animateItem().staggerIn(5, seen).then(dayFade)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) {}.clickable(role = Role.Button) { nav.push(Page.Weight) },
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(if (lastW == null) stringResource(R.string.d_weight_none) else stringResource(R.string.d_weight_title, "%.1f kg".format(lastW.kg)), style = U.t.body, fontWeight = FontWeight.Bold)
                            Text(
                                if (lastW == null || trend == null) stringResource(R.string.d_weight_first)
                                else stringResource(R.string.d_weight_sub, "%.1f".format(trend), dayTitle(lastW.date).lowercase()),
                                style = U.t.caption, color = c.ink2,
                            )
                        }
                        ActionChip(stringResource(R.string.d_plus_weighin), c.leafInk, c.leafS, stringResource(R.string.d_plus_weighin_desc)) {
                            nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT")
                        }
                    }
                }
            }
        }
    }

    // ---------- quick looks ----------
    NUTRIENTS.firstOrNull { it.key == nutrientKey }?.let { d -> NutrientLook(d, entries, byMeal, total, t) { nutrientKey = null } }

    mealOf(mealLook)?.let { m ->
        val list = byMeal.getValue(m)
        MealLook(
            m, list, t, data.settings.mealSplit,
            onDismiss = { mealLook = null },
            onAddMore = { nav.push(Page.Plate(m, date)) },
            onCopy = { copyFrom = m.name },
            onCombo = { comboFrom = m.name },
            onToggleCheat = { v -> undoable(sCheatMeal) { store.setMealCheat(date, m, v) } },
            onDeleteMeal = { undoable(sMealCleared(m)) { store.deleteMeal(date, m) }; mealLook = null },
            onEdit = { e -> mealLook = null; editId = e.id },
            onDeleteEntry = { e -> undoable(sMsgDeletedItem) { store.deleteEntry(e.id) } },
        )
    }

    if (budgetLook) BudgetLook(data, t, eaten, left, byMeal) { budgetLook = false }

    editId?.let { id ->
        val e = entries.firstOrNull { it.id == id }
        val f = e?.food()
        if (e != null && f != null) {
            CustomiseSheet(f, NewItem(e.foodId, e.qty, e.unit, e.choices), saveLabel = stringResource(R.string.d_save), onDismiss = { editId = null },
                onRemove = { undoable(sMsgDeletedItem) { store.deleteEntry(e.id) }; editId = null }) { ni ->
                store.updateEntry(e.copy(qty = ni.qty, unit = ni.unit, choices = ni.choices)); editId = null
            }
        } else editId = null
    }

    mealOf(copyFrom)?.let { from ->
        CopyMealDialog(from, onDismiss = { copyFrom = null }) { to ->
            undoable(sCopiedDay) { store.copyMeal(date, from, today(), to) }
            nav.date.value = today(); copyFrom = null
        }
    }

    mealOf(comboFrom)?.let { m ->
        InputDialog(stringResource(R.string.d_save_combo_title), stringResource(R.string.d_name), stringResource(R.string.d_my_usual, m.label.lowercase()), onDismiss = { comboFrom = null }) { name ->
            val items = byMeal.getValue(m).map { ComboItem(it.foodId, it.qty, it.unit, it.choices) }
            undoable(sCombo) { store.saveCombo(Combo(newKey("c_"), name.trim(), m.emoji, m, items)) }
        }
    }

    if (cheatNote) {
        InputDialog(stringResource(R.string.d_cheat_note_title), stringResource(R.string.d_cheat_note_label), "", onDismiss = { cheatNote = false }) { note ->
            undoable(sCheatOn) { store.setCheatDay(date, true, note) }
        }
    }
}

@Composable
private fun ActionChip(text: String, fg: Color, bg: Color, description: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.heightIn(min = 48.dp).pressScale().clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = fg, style = U.t.label, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun BudgetHero(
    modifier: Modifier, kcal: Int, eaten: Int, left: Int, byMeal: Map<Meal, List<LogEntry>>, colors: List<Color>,
    total: app.unavu.data.Nutr, tg: app.unavu.data.Targets, homeChips: List<String>, onBudget: () -> Unit, onChip: (NutrientDef) -> Unit,
) {
    val c = U.c
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    val shownLeft by animateIntAsState(if (armed) abs(left) else 0, motionTween(Motion.LONG), label = "left")
    val grow by animateFloatAsState(if (armed) 1f else 0f, motionTween(Motion.LONG), label = "grow")
    val parts = Meal.entries.map { m -> (byMeal.getValue(m).total().kcal / kcal).toFloat() * grow }
    val scale = if (parts.sum() > 1f) 1f / parts.sum() else 1f
    val mealKcal = Meal.entries.map { m -> byMeal.getValue(m).total().kcal.roundToInt() }
    val breakdown = Meal.entries.mapIndexed { i, m -> stringResource(R.string.d_meal_kcal, m.label, mealKcal[i]) }.joinToString(" ")
    val summary = if (left >= 0) stringResource(R.string.d_budget_summary_left, left, kcal, eaten, breakdown)
    else stringResource(R.string.d_budget_summary_over, -left, kcal, eaten, breakdown)
    val openDesc = stringResource(R.string.d_budget_open)

    AppCard(modifier) {
        Row(Modifier.semantics(mergeDescendants = true) { contentDescription = summary }, verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (left >= 0) R.string.d_can_still_eat else R.string.d_over_target_by), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2, letterSpacing = 0.8.sp)
                Row(Modifier.padding(top = 4.dp)) {
                    val big = U.t.display
                    Text("$shownLeft", style = big.copy(fontSize = big.fontSize * 1.5f, lineHeight = big.lineHeight * 1.5f), color = if (left >= 0) c.ink else c.chiInk, modifier = Modifier.alignByBaseline())
                    Text(stringResource(R.string.d_kcal_unit), style = U.t.bodyS, fontWeight = FontWeight.SemiBold, color = c.ink2, modifier = Modifier.alignByBaseline())
                }
            }
            if (U.sketch) {
                val frac = (eaten / kcal.toFloat()).coerceIn(0f, 1f)
                val track = c.ink2.copy(alpha = 0.25f); val ringCol = if (left >= 0) c.acc else c.chi
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(92.dp)) {
                        val r = size.minDimension / 2 - 7.dp.toPx()
                        roughArc(track, center, r, 0f, 360f, 3, 7.dp.toPx())
                        roughArc(ringCol, center, r, -90f, 360f * frac, 7, 7.dp.toPx())
                    }
                    Text("${(eaten * 100f / kcal).roundToInt()}%", style = U.t.title)
                }
            } else {
                Text(stringResource(R.string.d_eaten_of_line, eaten, kcal), style = U.t.label, color = c.ink2, textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        if (U.sketch) Text(stringResource(R.string.d_eaten_of_line, eaten, kcal), style = U.t.label, color = c.ink2, modifier = Modifier.padding(top = 2.dp).clearSem())
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = openDesc, onClick = onBudget),
            contentAlignment = Alignment.Center,
        ) {
            if (U.sketch) {
                val outline = c.ink.copy(alpha = 0.8f)
                Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                    val pad = 2.5.dp.toPx()
                    val inner = Rect(pad, pad, size.width - pad, size.height - pad)
                    var x = inner.left
                    parts.forEachIndexed { i, p ->
                        val w = inner.width * p * scale
                        if (w > 0f) hachure(colors[i], Rect(x, inner.top, x + w, inner.bottom), seed = i)
                        x += w
                    }
                    roughRect(outline, 6.dp.toPx(), 11, 1.5.dp.toPx())
                }
            } else Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(8.dp)).background(c.sf2)) {
                parts.forEachIndexed { i, p -> if (p > 0f) Box(Modifier.fillMaxHeight().weight(p * scale).background(colors[i])) }
                val rest = 1f - parts.sum() * scale
                if (rest > 0.0001f) Spacer(Modifier.weight(rest))
            }
        }
        AnimatedVisibility(eaten > 0) {
            Row(Modifier.clearSem().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Meal.entries.forEachIndexed { i, m -> LegendDot(colors[i], m.label) }
            }
        }
        Spacer(Modifier.height(14.dp))
        val chips = homeChips.mapNotNull { k -> NUTRIENTS.firstOrNull { it.key == k } }
        chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { d ->
                    val have = d.value(total); val target = d.target(tg)
                    NutrientChip(d, have, target, statusOf(d, have, target), Modifier.weight(1f), logged = eaten > 0) { onChip(d) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun Modifier.clearSem(): Modifier = this.semantics { }
