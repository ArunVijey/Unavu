package app.unavu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import app.unavu.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextOverflow
import app.unavu.data.AppData
import app.unavu.data.LogEntry
import app.unavu.data.fmtInt
import kotlin.math.abs
import app.unavu.R
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.entriesOn
import app.unavu.data.isCheatDay
import app.unavu.data.targets
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.LocalData
import app.unavu.ui.fmtTime
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween
import java.time.LocalTime
import kotlin.math.roundToInt

/** Insights rows: fixed label / value columns so every bar and grid row starts and ends at the same x. */
internal val INSIGHT_LABEL_W = 112.dp
internal val INSIGHT_VALUE_W = 96.dp

/** Sketch (wide Excalifont), large font scales and narrow screens: put the row label above its bar instead of beside it. */
@Composable
internal fun stackedRows(): Boolean =
    U.sketch || LocalDensity.current.fontScale > 1.15f || LocalConfiguration.current.screenWidthDp < 360

/** 7-day grid: one square per day for the main nutrients. */
@Composable
internal fun GapGrid(ds: List<String>) {
    val c = U.c
    val data = LocalData.current
    val t = data.targets() ?: return
    val keys = listOf("protein", "fibre", "iron", "calcium", "sodium", "sugar")
    val okLbl = stringResource(R.string.g_ins_gap_ok); val lowLbl = stringResource(R.string.g_ins_gap_low); val noneLbl = stringResource(R.string.g_ins_gap_none)
    val overLbl = stringResource(R.string.g_ins_gap_over)
    val progressLbl = stringResource(R.string.r7c_day_progress)
    val stacked = stackedRows()
    // Today, while still in progress, is shown faded and never counted as "low" (lunch hasn't happened yet).
    val inProgress = remember(data, ds) { ds.map { data.dayInProgress(it) } }
    AppCard {
        CardTitle(stringResource(R.string.g_ins_gaps_title))
        val lowCounts = mutableMapOf<String, Int>()
        keys.forEach { k ->
            val n = NUTRIENTS.first { it.key == k }
            val cells = ds.mapIndexed { i, d ->
                val es = data.entriesOn(d)
                val v = n.value(es.total()) / n.target(t)
                val partial = inProgress[i]
                val flagged = es.isNotEmpty() && !partial && ((n.limit && v > 1) || (!n.limit && v < 0.9))
                if (flagged) lowCounts[k] = (lowCounts[k] ?: 0) + 1
                val col = when {
                    es.isEmpty() -> c.sf2
                    n.limit -> if (v > 1) c.chi else if (v > 0.85) c.tur else c.leaf
                    else -> if (v >= 0.9) c.leaf else if (v >= 0.6) c.tur else c.turS
                }
                val word = when {
                    es.isEmpty() -> noneLbl
                    partial -> progressLbl
                    flagged -> if (n.limit) overLbl else lowLbl
                    else -> okLbl
                }
                Triple(col, word, partial && es.isNotEmpty())
            }
            val desc = n.name + ": " + cells.joinToString(", ") { it.second }
            val squares: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    cells.forEach { (col, _, faded) ->
                        Box(Modifier.weight(1f).padding(horizontal = 1.5.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(if (faded) col.copy(alpha = 0.4f) else col))
                    }
                }
            }
            if (stacked) {
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).clearAndSetSemantics { contentDescription = desc }) {
                    Text(n.name, style = U.t.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 3.dp))
                    squares()
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clearAndSetSemantics { contentDescription = desc }, verticalAlignment = Alignment.CenterVertically) {
                    Text(n.name, style = U.t.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(INSIGHT_LABEL_W))
                    Box(Modifier.weight(1f)) { squares() }
                }
            }
        }
        val worst = lowCounts.maxByOrNull { it.value }
        if (worst != null && worst.value >= 3) {
            val n = NUTRIENTS.first { it.key == worst.key }
            Text(
                "💡 " + stringResource(if (n.limit) R.string.g_ins_gap_tip_over else R.string.g_ins_gap_tip_low, n.name, worst.value) + " " + n.tip,
                style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** When each meal was logged, and the late-dinner effect. */
@Composable
internal fun MealTiming(ds: List<String>) {
    val c = U.c
    val data = LocalData.current
    val colors = mealColors(c)
    // Only entries whose clock time says when they were eaten (back-filled ones are dropped, see timedEntries).
    val timed = remember(data, ds) { timedEntries(data, ds) }
    val rows = remember(timed, ds) { ds.takeLast(7).map { d -> d to Meal.entries.map { m -> timed.getValue(d).filter { it.meal == m }.minOfOrNull { it.time } } } }
    if (rows.none { r -> r.second.any { it != null } }) return
    // draws in once, the first time this card appears
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val reveal by animateFloatAsState(if (shown) 1f else 0f, motionTween(Motion.LONG), label = "timing")

    val firsts = ds.mapNotNull { d -> timed.getValue(d).minOfOrNull { it.time }?.let(::hourOf) }
    val lasts = ds.mapNotNull { d -> timed.getValue(d).maxOfOrNull { it.time }?.let(::hourOf) }
    val dinnerTimes = ds.filterNot { data.isCheatDay(it) }.mapNotNull { d -> timed.getValue(d).filter { it.meal == Meal.DINNER }.minOfOrNull { it.time }?.let { d to mealHour(Meal.DINNER, it) } }
    val late = dinnerTimes.filter { it.second >= 22f }.map { it.first }
    val normal = dinnerTimes.filter { it.second < 22f }.map { it.first }
    fun fmt(h: Double) = fmtTime(h.toInt(), ((h % 1) * 60).roundToInt().coerceAtMost(59))
    val windowText = if (firsts.isNotEmpty()) stringResource(R.string.g_ins_window, fmt(firsts.average()), fmt(lasts.average())) else null
    val chartDesc = stringResource(R.string.g_ins_timing_desc, rows.count { r -> r.second.any { it != null } }, windowText ?: "")
    val hours = stringArrayResource(R.array.r7c_timing_hours)

    AppCard {
        CardTitle(stringResource(R.string.g_ins_timing_title))
        Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = chartDesc }) {
            fun x(h: Float) = (h - 6f) / 18f * size.width
            listOf(6, 9, 12, 15, 18, 21, 24).forEach { h -> drawLine(c.line, Offset(x(h.toFloat()), 0f), Offset(x(h.toFloat()), size.height), 1f) }
            drawRect(c.chi.copy(alpha = 0.10f), Offset(x(22f), 0f), Size(x(24f) - x(22f), size.height))
            rows.forEachIndexed { i, (_, times) ->
                val y = 10f + i * (size.height - 20f) / 6f
                times.forEachIndexed { mi, t -> if (t != null) drawCircle(colors[mi].copy(alpha = reveal), 5.dp.toPx() * reveal, Offset(x(mealHour(Meal.entries[mi], t).coerceIn(6f, 24f)), y)) }
            }
        }
        // Each hour label is centred on its gridline (the ends are kept inside the card), not spread in equal columns.
        Layout(
            content = { hours.forEach { Text(it, style = U.t.caption, color = c.ink2, maxLines = 1) } },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clearAndSetSemantics { },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
            val w = constraints.maxWidth
            val h = placeables.maxOfOrNull { it.height } ?: 0
            val gaps = (placeables.size - 1).coerceAtLeast(1)
            layout(w, h) {
                placeables.forEachIndexed { i, p ->
                    val cx = w * i / gaps
                    p.placeRelative((cx - p.width / 2).coerceIn(0, (w - p.width).coerceAtLeast(0)), 0)
                }
            }
        }
        if (late.isNotEmpty()) {
            val lateAvg = late.map { data.entriesOn(it).total().kcal }.average()
            val normAvg = normal.map { data.entriesOn(it).total().kcal }.average().takeIf { !it.isNaN() }
            val base = pluralStringResource(R.plurals.g_ins_late_dinners, late.size, late.size)
            val delta = normAvg?.let { (lateAvg - it).roundToInt() }
            Text(
                "🌙 " + base + (delta?.let { " " + stringResource(R.string.r7c_ins_late_delta, (if (it < 0) "−" else "+") + fmtInt(abs(it))) } ?: "."),
                style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (windowText != null) Text(windowText, style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.r7c_ins_timing_note), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.Normal)
    }
}

private fun hourOf(t: String): Float = runCatching { LocalTime.parse(t) }.getOrNull()?.let { it.hour + it.minute / 60f } ?: 12f

/** Like [hourOf], but a dinner logged just after midnight counts as 24+ (a late dinner, not an early one). */
private fun mealHour(m: Meal, t: String): Float = hourOf(t).let { if (m == Meal.DINNER && it < 3f) it + 24f else it }

/**
 * Entries per day whose log time can stand for when they were eaten. An entry is left out when it was back-filled:
 * - it was logged after any later day already had entries (ids only grow, so a smaller id on a later day proves it), or
 * - its time is far outside that meal's usual hours (breakfast logged at 4:30 PM is a catch-up, not a late breakfast).
 */
private fun timedEntries(data: AppData, ds: List<String>): Map<String, List<LogEntry>> {
    val byDate = data.entries.groupBy { it.date }
    val firstIdAfter = HashMap<String, Long>()
    var running = Long.MAX_VALUE
    for (d in byDate.keys.sortedDescending()) {
        firstIdAfter[d] = running
        running = minOf(running, byDate.getValue(d).minOf { it.id })
    }
    return ds.associateWith { d ->
        val limit = firstIdAfter[d] ?: Long.MAX_VALUE
        data.entriesOn(d).filter { e -> e.id < limit && plausibleTime(e.meal, hourOf(e.time)) }
    }
}

private fun plausibleTime(m: Meal, h: Float): Boolean = when (m) {
    Meal.BREAKFAST -> h in 4f..12.5f
    Meal.LUNCH -> h in 10.5f..17f
    Meal.SNACKS -> h in 9f..23.5f
    Meal.DINNER -> h >= 17.5f || h < 3f
}

/** Card/section heading inside Insights: title role, announced as a heading. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = U.t.title, color = U.c.ink, modifier = modifier.padding(bottom = 8.dp).semantics { heading() })
}

/** Lazy item that fades in with a 12dp upward slide, staggered 40ms by [index]; plays once per item (survives scrolling). */
fun LazyListScope.staggerItem(key: String, contentType: String, index: Int, content: @Composable LazyItemScope.() -> Unit) {
    item(key = key, contentType = contentType) {
        val reduced = LocalReducedMotion.current
        var shown by rememberSaveable(key) { mutableStateOf(reduced) }
        val p by animateFloatAsState(if (shown) 1f else 0f, motionTween(Motion.LONG), label = "stagger")
        LaunchedEffect(Unit) { if (!shown) { delay(40L * index.coerceAtMost(10)); shown = true } }
        Box(Modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx() }) { content() }
    }
}
