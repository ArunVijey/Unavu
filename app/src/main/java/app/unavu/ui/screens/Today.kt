package app.unavu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.unavu.data.fmtInt
import app.unavu.data.targetKcalOn
import app.unavu.ui.theme.Space
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import app.unavu.ui.GlyphCircle
import app.unavu.ui.LineGlyph
import app.unavu.ui.AddButton
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Size
import app.unavu.ui.screens.today.BudgetHero
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

/** Fixed height for every meal row on Today (logged or empty). */
private val MEAL_ROW_HEIGHT = 72.dp

fun mealColors(c: AppColors): List<Color> = listOf(c.tur, c.leaf, c.m3, c.blue)

fun statusOf(d: NutrientDef, have: Double, target: Double, pace: Double = 1.0): Status = if (d.limit) {
    if (have > target) Status.OVER else Status.OK
} else {
    if (have >= target * 0.9 * pace.coerceIn(0.0, 1.0)) Status.OK else Status.LOW
}

/**
 * Grams / milligrams: whole numbers from 10 up (53.3 → 53, grouped like [fmtInt]: "3,496"), one decimal below 10
 * (2.4 mg iron). Rounds to one decimal first, so 9.96 shows "10", never "10.0".
 */
fun fmtNum(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (abs(r) >= 10 || r % 1.0 == 0.0) fmtInt(r) else String.format(Locale.ROOT, "%.1f", r)
}

/** "Today", "Yesterday" or "Mon, 21 Nov", from string resources. Prefer this over [dayTitle] in composables. */
@Composable
fun dayLabel(date: String): String = when (date) {
    today() -> stringResource(R.string.d_today)
    LocalDate.now().minusDays(1).format(DATE) -> stringResource(R.string.d_yesterday)
    else -> dayTitle(date)
}

/** For mid-sentence use: "today", "yesterday" or "on Mon, 21 Nov". */
@Composable
fun dayWhen(date: String): String = when (date) {
    today() -> stringResource(R.string.r7c_when_today)
    LocalDate.now().minusDays(1).format(DATE) -> stringResource(R.string.r7c_when_yesterday)
    else -> stringResource(R.string.r7c_when_on, dayTitle(date))
}

/** Non-composable day title (English). Composables should call [dayLabel], which reads the words from resources. */
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
private fun fmtL(ml: Int): String = String.format(Locale.ROOT, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')

/** Kilograms to one decimal: 72.4. */
private fun fmtKg(kg: Double): String = String.format(Locale.ROOT, "%.1f", kg)

/**
 * A 0 → 0.6 → 0 flash when [count] grows on the same [date]: food just landed in this meal (quick add, plate,
 * undo of a delete). The last count is saveable, so coming back from the plate builder still flashes the row.
 */
@Composable
private fun rememberAddPulse(count: Int, date: String): Animatable<Float, AnimationVector1D> {
    val reduced = LocalReducedMotion.current
    val pulse = remember { Animatable(0f) }
    var last by rememberSaveable(date) { mutableIntStateOf(count) }
    LaunchedEffect(count, date) {
        if (count > last && !reduced) {
            pulse.snapTo(0f)
            pulse.animateTo(0.6f, tween(Motion.MEDIUM / 2, easing = Motion.Standard))
            pulse.animateTo(0f, tween(Motion.MEDIUM, easing = Motion.Standard))
        }
        last = count
    }
    return pulse
}

@Composable
fun TodayScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val date = nav.date.value
    val base = data.targets() ?: return
    // A past day is judged against the target that applied on it, not today's.
    val dayKcal = data.targetKcalOn(date) ?: base.kcal
    val t = remember(base, dayKcal) { if (dayKcal == base.kcal) base else base.copy(kcal = dayKcal) }
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
        TopBar(dayLabel(date)) {
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
            Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = 12.dp.toPx()), size = Size(size.width, 12.dp.toPx()), blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
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
                        Text(stringResource(R.string.r7c_new_target, fmtInt(newKcal)), style = U.t.body, fontWeight = FontWeight.Bold, color = c.leafInk)
                        Text(stringResource(R.string.r7c_new_target_sub, fmtInt(base.kcal)), style = U.t.caption, color = c.ink2)
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
                val liqSub = stringResource(R.string.r7c_liquids_sub, fmtL(water), fmtL(goal), fmtInt(drinks))
                AppCard(Modifier.animateItem().staggerIn(3, seen).then(dayFade)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) {}.clickable(role = Role.Button) { nav.push(Page.Liquids) },
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(stringResource(R.string.d_liquids_title, fmtL(water + drinks)), style = U.t.body, fontWeight = FontWeight.Bold)
                            Text(liqSub, style = U.t.caption, color = c.ink2)
                        }
                        AddButton(stringResource(R.string.d_plus_glass), stringResource(R.string.d_plus_glass_desc)) {
                            undoable(sGlass) { store.addWater(date, 250) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Track(water / goal.toFloat(), c.blue, height = 8.dp, contentDescription = liqSub)
                }
            }

            // ---------- Meals ----------
            item(key = "meals", contentType = "meals") {
                val cardPad = 18.dp
                AppCard(Modifier.animateItem().staggerIn(4, seen).then(dayFade), padding = PaddingValues(horizontal = cardPad, vertical = 0.dp)) {
                    Meal.entries.forEachIndexed { i, m -> key(m) {
                        if (i > 0) Divider()
                        val list = byMeal.getValue(m)
                        val kcal = list.total().kcal.roundToInt()
                        val empty = list.isEmpty()
                        val cheatMeal = list.any { it.cheat }
                        val sub = if (empty) stringResource(R.string.d_meal_not_logged) else list.joinToString(" · ") { e -> mealItemText(e) }
                        // TalkBack: "Lunch, 751 kilocalories: 2 idli · sambar" (the number alone never had a unit).
                        val rowDesc = if (empty) stringResource(R.string.r7c_meal_row_empty_desc, m.label)
                        else stringResource(R.string.r7c_meal_row_desc, m.label, fmtInt(kcal), sub) + if (cheatMeal) ", " + stringResource(R.string.d_cheat_meal) else ""
                        val clickLabel = stringResource(if (empty) R.string.r7c_meal_row_add else R.string.r7c_meal_row_open, m.label)
                        val pulse = rememberAddPulse(list.size, date)
                        val pulseColor = c.accS
                        Row(
                            // One fixed height for logged and empty rows, so nothing jumps when a meal is logged.
                            Modifier.fillMaxWidth().height(MEAL_ROW_HEIGHT)
                                // A one-off soft flash on the row that just received food (quick add, plate, undo).
                                .drawBehind {
                                    val a = pulse.value
                                    if (a > 0f) {
                                        val pad = cardPad.toPx()
                                        drawRect(pulseColor.copy(alpha = a), topLeft = Offset(-pad, 0f), size = Size(size.width + 2 * pad, size.height))
                                    }
                                }
                                .semantics(mergeDescendants = true) { contentDescription = rowDesc }
                                .clickable(role = Role.Button, onClickLabel = clickLabel) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (empty) nav.push(Page.Plate(m, date)) else mealLook = m.name
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(if (empty) colors[i].copy(alpha = 0.45f) else colors[i]).border(1.dp, c.ink.copy(alpha = 0.25f), CircleShape))
                            Column(Modifier.weight(1f).clearAndSetSemantics {}) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(m.label, style = U.t.titleS, color = if (empty) c.ink2 else c.ink)
                                    if (cheatMeal) StatusBadge(stringResource(R.string.d_cheat_meal), Status.LOW)
                                }
                                AnimatedContent(sub, transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) }, label = "mealSub") { txt ->
                                    Text(txt, style = U.t.bodyS, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                            if (empty) AddButton(null, null)
                            else {
                                val shownKcal by animateIntAsState(kcal, motionTween(Motion.LONG), label = "mealKcal")
                                Column(Modifier.clearAndSetSemantics {}, horizontalAlignment = Alignment.End) {
                                    Text(fmtInt(shownKcal), style = U.t.titleS, fontWeight = FontWeight.ExtraBold, color = c.ink)
                                    Text(stringResource(R.string.d_kcal_unit), style = U.t.caption, color = c.ink2)
                                }
                            }
                        }
                    } }
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
                            // The headline is the trend, as on Weight and Insights; the last weigh-in is the detail.
                            val headline = trend ?: lastW?.kg
                            Text(
                                if (headline == null) stringResource(R.string.d_weight_none) else stringResource(R.string.r7c_weight_title, fmtKg(headline)),
                                style = U.t.body, fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when {
                                    lastW == null -> stringResource(R.string.d_weight_first)
                                    data.weights.size < 2 -> stringResource(R.string.r7c_weight_sub_first, dayWhen(lastW.date))
                                    else -> stringResource(R.string.r7c_weight_sub, fmtKg(lastW.kg), dayWhen(lastW.date))
                                },
                                style = U.t.caption, color = c.ink2,
                            )
                        }
                        AddButton(stringResource(R.string.d_plus_weighin), stringResource(R.string.d_plus_weighin_desc)) {
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
