package app.unavu.ui.screens.today

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
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
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
    return if (e.unit == "piece" || e.unit == "egg") "${app.unavu.data.fmtQty(e.qty)} ${f.name}" else f.name
}

@Composable
fun NutrientChip(d: NutrientDef, have: Double, target: Double, st: Status, modifier: Modifier, logged: Boolean = true, onClick: () -> Unit) {
    val c = U.c
    val neutral = !logged || have <= 0.0
    val bgT = if (neutral) c.sf2 else when (st) { Status.OK -> c.leafS; Status.LOW -> c.turS; else -> c.chiS }
    val fgT = if (neutral) c.ink2 else when (st) { Status.OK -> c.leafInk; Status.LOW -> c.turInk; else -> c.chiInk }
    val barT = if (neutral) c.ink2.copy(alpha = 0.35f) else when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi }
    val bg by animateColorAsState(bgT, motionTween(Motion.MEDIUM), label = "chipBg")
    val fg by animateColorAsState(fgT, motionTween(Motion.MEDIUM), label = "chipFg")
    val bar by animateColorAsState(barT, motionTween(Motion.MEDIUM), label = "chipBar")
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    val frac by animateFloatAsState(if (armed && target > 0) (have / target).toFloat().coerceIn(0f, 1f) else 0f, motionTween(Motion.LONG), label = "chipFrac")
    Column(
        modifier.edgeIfSketch(fg, 12.dp, d.key.length)
            .heightIn(min = 48.dp).pressScale()
            .clip(RoundedCornerShape(14.dp)).background(if (U.sketch) fg.copy(alpha = 0.12f) else bg)
            .semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!neutral) { Box(Modifier.size(6.dp).clip(CircleShape).background(bar)); Spacer(Modifier.width(5.dp)) }
            Text(d.name, color = fg, style = U.t.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("${fmtNum(have)}/${fmtNum(target)}", color = fg, style = U.t.caption, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
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
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Arrow.LEFT) { onSelect(sel.minusWeeks(1).format(DATE)) }
            Text(stringResource(R.string.d_week_of, "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"), style = U.t.label, color = c.ink2)
            ArrowButton(Arrow.RIGHT) { if (sel.plusWeeks(1) <= todayD.with(DayOfWeek.SUNDAY)) onSelect(minOf(sel.plusWeeks(1), todayD).format(DATE)) }
        }
        val selIdx = java.time.temporal.ChronoUnit.DAYS.between(monday, sel).toInt().coerceIn(0, 6)
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
                    val logged = remember(data.entries, ds) { data.entries.any { it.date == ds } }
                    val cheat = data.isCheatDay(ds)
                    val future = d.isAfter(todayD)
                    val label = "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${d.dayOfMonth}"
                    val desc = when { cheat -> stringResource(R.string.d_day_cheat, label); logged -> stringResource(R.string.d_day_logged, label); else -> label }
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
                        else Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (on) c.bg else if (logged) c.leaf else Color.Transparent))
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
    val time = LocalTime.now()
    val mins = time.hour * 60 + time.minute
    val windowEnd = mapOf(Meal.BREAKFAST to 11 * 60, Meal.LUNCH to 15 * 60 + 30, Meal.SNACKS to 18 * 60 + 30, Meal.DINNER to 24 * 60)
    val unlogged = Meal.entries.filter { byMeal.getValue(it).isEmpty() }
    val nextNow = if (isToday && mins < 22 * 60 && byMeal.getValue(Meal.DINNER).isEmpty()) {
        unlogged.firstOrNull { mins < windowEnd.getValue(it) } ?: unlogged.firstOrNull()
    } else null
    val last = remember { arrayOfNulls<Meal>(1) }
    if (nextNow != null) last[0] = nextNow
    val next = nextNow ?: last[0]
    if (next == null) return
    val proteinLow = total.protein < t.protein * 0.6
    AnimatedVisibility(nextNow != null, enter = expandVertically(motionTween(Motion.MEDIUM)) + fadeIn(motionTween(Motion.MEDIUM)), exit = shrinkVertically(motionTween(Motion.MEDIUM)) + fadeOut(motionTween(Motion.SHORT))) {
        AppCard(color = c.turS, border = null, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); nav.push(Page.Plate(next, date)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Emoji(next.emoji, 44)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.d_next_up_title, next.label, left.coerceAtLeast(0)), style = U.t.body, fontWeight = FontWeight.Bold)
                    Text(
                        if (proteinLow) stringResource(R.string.d_next_up_low_protein) else stringResource(R.string.d_next_up_tap, next.label.lowercase()),
                        style = U.t.caption, color = c.ink2,
                    )
                }
                Text(stringResource(R.string.d_log), color = c.onAcc, style = U.t.label, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.acc).padding(horizontal = 12.dp, vertical = 9.dp))
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
