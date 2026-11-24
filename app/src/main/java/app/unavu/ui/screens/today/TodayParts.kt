package app.unavu.ui.screens.today

import app.unavu.data.fmtQty
import app.unavu.data.fmtInt
import app.unavu.data.mealShare
import app.unavu.data.nowMinutes
import app.unavu.data.suggestMeal
import androidx.compose.runtime.SideEffect
import app.unavu.ui.LocalData
import app.unavu.ui.ArrowIcon
import app.unavu.ui.screens.DayDot
import app.unavu.ui.screens.DayState
import app.unavu.ui.screens.judgeDay
import app.unavu.ui.screens.spoken
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.DATE
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.Nutr
import app.unavu.data.NutrientDef
import app.unavu.data.Store
import app.unavu.data.Targets
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.AddButton
import app.unavu.ui.AddTone
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.unavu.ui.Arrow
import app.unavu.ui.LocalFeedback
import app.unavu.ui.Page
import app.unavu.ui.Status
import app.unavu.ui.theme.edge
import androidx.compose.ui.unit.Dp
import app.unavu.ui.nav
import app.unavu.ui.pressScale
import app.unavu.ui.screens.Emoji
import app.unavu.ui.screens.fmtNum
import app.unavu.ui.theme.U
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import java.time.temporal.ChronoUnit

@Composable
fun Modifier.edgeIfSketch(color: Color, radius: Dp, seed: Int, enabled: Boolean = true): Modifier =
    if (U.sketch && enabled) this.edge(color, radius, 1.5.dp, seed = seed) else this

/** Runs [block] and shows a snackbar with Undo that restores the data from before it ran. */
@Composable
fun rememberUndo(store: Store): (String, () -> Unit) -> Unit {
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val undo = stringResource(R.string.d_undo)
    return remember(store, feedback, haptic, undo) {
        { message, block ->
            val before = store.data.value
            block()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            feedback?.show(message, undo) { store.restoreSnapshot(before) }
        }
    }
}

fun mealItemText(e: LogEntry): String {
    val f = e.food() ?: return "?"
    return if (e.unit == "piece" || e.unit == "egg") "${fmtQty(e.qty)} ${f.name}" else f.name
}

/** "g" → "grams", "mg" → "milligrams" for content descriptions. */
@Composable
fun spokenUnit(unit: String): String = when (unit) {
    "g" -> stringResource(R.string.d_unit_g)
    "mg" -> stringResource(R.string.d_unit_mg)
    else -> unit
}

/**
 * Previous / next pager arrow (week strip, History month). Same look as ArrowButton, plus a real disabled state:
 * 38 % alpha, no press feedback, not clickable, and TalkBack hears "disabled".
 */
@Composable
fun PagerArrow(dir: Arrow, contentDescription: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = U.c
    val desc = contentDescription
    val shown by animateFloatAsState(if (enabled) 1f else 0.38f, motionTween(Motion.SHORT), label = "pagerArrow")
    Box(
        Modifier.graphicsLayer { alpha = shown }
            .size(48.dp).then(if (enabled) Modifier.pressScale() else Modifier).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).then(if (U.sketch) Modifier.edge(c.ink.copy(alpha = 0.7f), 20.dp, 1.4.dp, seed = dir.ordinal) else Modifier)
                .clip(CircleShape).background(c.sf2),
            contentAlignment = Alignment.Center,
        ) { ArrowIcon(dir) }
    }
}

@Composable
fun NutrientChip(d: NutrientDef, have: Double, target: Double, st: Status, modifier: Modifier, logged: Boolean = true, onClick: () -> Unit) {
    val c = U.c
    val neutral = !logged || have <= 0.0
    val bgT = if (neutral) c.sf2 else when (st) { Status.OK -> c.leafS; Status.LOW -> c.turS; else -> c.chiS }
    val fgT = if (neutral) c.ink2 else when (st) { Status.OK -> c.leafInk; Status.LOW -> c.turInk; else -> c.chiInk }
    // The bar always carries the status (leaf on track, turmeric low, chilli over a limit) once
    // anything counts; only the tile fill stays neutral early in the day.
    val barT = if (have <= 0.0) c.ink2.copy(alpha = 0.35f) else when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi }
    val bg by animateColorAsState(bgT, motionTween(Motion.MEDIUM), label = "chipBg")
    val fg by animateColorAsState(fgT, motionTween(Motion.MEDIUM), label = "chipFg")
    val bar by animateColorAsState(barT, motionTween(Motion.MEDIUM), label = "chipBar")
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    val desc = stringResource(R.string.d_nutrient_tile_desc, d.name, fmtNum(have), fmtNum(target), spokenUnit(d.unit))
    val frac by animateFloatAsState(if (armed && target > 0) (have / target).toFloat().coerceIn(0f, 1f) else 0f, motionTween(Motion.LONG), label = "chipFrac")
    // At large font scales and in Sketch (wide Excalifont) the value gets its own line and "/ target" a second one,
    // so "3,496/2,000 mg" is never cut to "3,496/2,00…". Both lines also shrink to fit (never below 10 sp).
    val split = U.sketch || LocalDensity.current.fontScale > LARGE_FONT_SCALE
    val valueStyle = U.t.caption.copy(fontWeight = FontWeight.SemiBold)
    val fit = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = valueStyle.fontSize, stepSize = 0.5.sp)
    val ink = ColorProducer { fg }
    Column(
        modifier.edgeIfSketch(fg, 12.dp, d.key.length)
            .heightIn(min = 48.dp).pressScale()
            .clip(RoundedCornerShape(14.dp)).background(if (U.sketch) fg.copy(alpha = 0.12f) else bg)
            .semantics(mergeDescendants = true) { contentDescription = desc }.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
            if (have > 0.0) { Box(Modifier.size(6.dp).clip(CircleShape).background(bar)); Spacer(Modifier.width(5.dp)) }
            Text(d.name, color = fg, style = U.t.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.padding(top = 2.dp).clearAndSetSemantics {}) {
            if (split) {
                BasicText("${fmtNum(have)} ${d.unit}", style = valueStyle.copy(fontWeight = FontWeight.Bold), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, autoSize = fit)
                BasicText(stringResource(R.string.r7c_chip_of, fmtNum(target)), style = valueStyle, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, autoSize = fit)
            } else {
                BasicText("${fmtNum(have)}/${fmtNum(target)} ${d.unit}", style = valueStyle, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, autoSize = fit)
            }
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(fg.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(bar))
        }
    }
}

@Composable
fun WeekStrip(data: AppData, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val c = U.c
    val sel = LocalDate.parse(selected)
    val monday = sel.with(DayOfWeek.MONDAY)
    val todayD = LocalDate.now()
    // Only weeks up to the current one exist; on the current week "Next week" is disabled, not a silent no-op.
    val canNext = monday.isBefore(todayD.with(DayOfWeek.MONDAY))
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PagerArrow(Arrow.LEFT, stringResource(R.string.r7c_week_prev)) { onSelect(sel.minusWeeks(1).format(DATE)) }
            Text(stringResource(R.string.d_week_of, "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"), style = U.t.label, color = c.ink2)
            PagerArrow(Arrow.RIGHT, stringResource(R.string.r7c_week_next), enabled = canNext) { onSelect(minOf(sel.plusWeeks(1), todayD).format(DATE)) }
        }
        val selIdx = ChronoUnit.DAYS.between(monday, sel).toInt().coerceIn(0, 6)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp).height(58.dp)) {
            val gap = 2.dp
            val cellW = (maxWidth - gap * 6) / 7
            val pillX by animateDpAsState((cellW + gap) * selIdx, motionTween(Motion.MEDIUM), label = "pillX")
            Box(
                Modifier.offset(x = pillX).width(cellW).height(58.dp)
                    .edgeIfSketch(c.ink, 10.dp, 1)
                    .clip(RoundedCornerShape(12.dp)).background(if (U.sketch) c.acc else c.ink),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                (0..6).forEach { i ->
                    val d = monday.plusDays(i.toLong())
                    val ds = d.format(DATE)
                    val on = ds == selected
                    // Same verdict as the History calendar, against the target that applied that day.
                    val state = remember(data, ds) { data.judgeDay(ds).state }
                    val logged = state != DayState.NONE
                    val cheat = data.isCheatDay(ds)
                    val future = d.isAfter(todayD)
                    val label = "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${d.dayOfMonth}"
                    val verdict = state.spoken()
                    val desc = when {
                        cheat -> stringResource(R.string.d_day_cheat, label)
                        verdict != null -> stringResource(R.string.r7c_day_desc, label, verdict)
                        logged -> stringResource(R.string.d_day_logged, label)
                        else -> label
                    }
                    Column(
                        Modifier.weight(1f).height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .semantics(mergeDescendants = true) { contentDescription = desc; this.selected = on }
                            .clickable(enabled = !future, role = Role.Button) { onSelect(ds) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val fg by animateColorAsState(if (on) (if (U.sketch) c.onAcc else c.bg) else c.ink2, motionTween(Motion.MEDIUM), label = "dayFg")
                        val num by animateColorAsState(if (on) (if (U.sketch) c.onAcc else c.bg) else if (future) c.ink2 else c.ink, motionTween(Motion.MEDIUM), label = "dayNum")
                        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), style = U.t.caption, fontWeight = FontWeight.SemiBold, color = fg)
                        Text("${d.dayOfMonth}", style = U.t.label, fontWeight = FontWeight.Bold, color = num)
                        if (cheat) Text("🍕", style = U.t.caption)
                        else DayDot(state, on, if (U.sketch) c.onAcc else c.bg, Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NextUpCard(date: String, byMeal: Map<Meal, List<LogEntry>>, left: Int, total: Nutr, t: Targets) {
    val c = U.c
    val nav = nav
    val haptic = LocalHapticFeedback.current
    val isToday = date == today()
    val mins = nowMinutes()
    val logged = Meal.entries.filter { byMeal.getValue(it).isNotEmpty() }.toSet()
    // Same rule as quick add's pre-selection (suggestMeal); hidden overnight so 4 am never suggests "Dinner".
    val nextNow = if (isToday && mins in 5 * 60 until 22 * 60 && Meal.DINNER !in logged) {
        suggestMeal(logged, mins).takeIf { it !in logged }
    } else null
    // Keeps the last meal on screen while the card animates out.
    val lastShown = remember { mutableStateOf<Meal?>(null) }
    if (nextNow != null) SideEffect { lastShown.value = nextNow }
    val next = nextNow ?: lastShown.value
    if (next == null) return
    val split = LocalData.current.settings.mealSplit
    val budget = mealShare(t.kcal, split, next)
    val tip = when {
        total.kcal <= 0.0 -> stringResource(R.string.d_next_up_tap, next.label.lowercase())
        total.protein < t.protein * 0.6 -> stringResource(tipRes(next, 0))
        total.fibre < t.fibre * 0.5 -> stringResource(tipRes(next, 1))
        total.iron < t.iron * 0.5 -> stringResource(tipRes(next, 2))
        else -> stringResource(tipRes(next, 3))
    }
    AnimatedVisibility(nextNow != null, enter = expandVertically(motionTween(Motion.MEDIUM)) + fadeIn(motionTween(Motion.MEDIUM)), exit = shrinkVertically(motionTween(Motion.MEDIUM)) + fadeOut(motionTween(Motion.SHORT))) {
        AppCard(color = c.turS, border = null, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); nav.push(Page.Plate(next, date)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Emoji(next.emoji, 44)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.r7c_next_up_budget, next.label, fmtInt(byMeal.getValue(next).total().kcal), fmtInt(budget)), style = U.t.body, fontWeight = FontWeight.Bold)
                    Text(
                        tip,
                        style = U.t.caption, color = c.ink2,
                    )
                }
                AddButton(stringResource(R.string.d_log), null, tone = AddTone.FILLED)
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color).border(1.dp, U.c.ink.copy(alpha = 0.25f), CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = U.t.caption, color = U.c.ink2, fontWeight = FontWeight.SemiBold)
    }
}

private fun tipRes(m: Meal, gap: Int): Int = when (m) {
    Meal.BREAKFAST -> intArrayOf(R.string.d_tip_b_protein, R.string.d_tip_b_fibre, R.string.d_tip_b_iron, R.string.d_tip_b_ok)
    Meal.LUNCH -> intArrayOf(R.string.d_tip_l_protein, R.string.d_tip_l_fibre, R.string.d_tip_l_iron, R.string.d_tip_l_ok)
    Meal.SNACKS -> intArrayOf(R.string.d_tip_s_protein, R.string.d_tip_s_fibre, R.string.d_tip_s_iron, R.string.d_tip_s_ok)
    Meal.DINNER -> intArrayOf(R.string.d_tip_d_protein, R.string.d_tip_d_fibre, R.string.d_tip_d_iron, R.string.d_tip_d_ok)
}[gap]
