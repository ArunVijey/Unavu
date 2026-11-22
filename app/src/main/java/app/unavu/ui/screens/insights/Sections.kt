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

/** 7-day grid: one square per day for the main nutrients. */
@Composable
internal fun GapGrid(ds: List<String>) {
    val c = U.c
    val data = LocalData.current
    val t = data.targets() ?: return
    val keys = listOf("protein", "fibre", "iron", "calcium", "sodium", "sugar")
    val okLbl = stringResource(R.string.g_ins_gap_ok); val lowLbl = stringResource(R.string.g_ins_gap_low); val noneLbl = stringResource(R.string.g_ins_gap_none)
    val overLbl = stringResource(R.string.g_ins_gap_over)
    AppCard {
        CardTitle(stringResource(R.string.g_ins_gaps_title))
        val lowCounts = mutableMapOf<String, Int>()
        keys.forEach { k ->
            val n = NUTRIENTS.first { it.key == k }
            val cells = ds.map { d ->
                val es = data.entriesOn(d)
                val v = n.value(es.total()) / n.target(t)
                val flagged = es.isNotEmpty() && ((n.limit && v > 1) || (!n.limit && v < 0.9))
                if (flagged) lowCounts[k] = (lowCounts[k] ?: 0) + 1
                val col = when {
                    es.isEmpty() -> c.sf2
                    n.limit -> if (v > 1) c.chi else if (v > 0.85) c.tur else c.leaf
                    else -> if (v >= 0.9) c.leaf else if (v >= 0.6) c.tur else c.turS
                }
                col to (if (es.isEmpty()) noneLbl else if (flagged) (if (n.limit) overLbl else lowLbl) else okLbl)
            }
            val desc = n.name + ": " + cells.joinToString(", ") { it.second }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clearAndSetSemantics { contentDescription = desc }, verticalAlignment = Alignment.CenterVertically) {
                Text(n.name, style = U.t.label, modifier = Modifier.widthIn(min = 88.dp, max = 120.dp))
                cells.forEach { (col, _) -> Box(Modifier.weight(1f).padding(horizontal = 1.5.dp).heightIn(min = 16.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(col)) }
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
    val rows = remember(data.entries, ds) { ds.takeLast(7).map { d -> d to Meal.entries.map { m -> data.entriesOn(d).filter { it.meal == m }.minOfOrNull { it.time } } } }
    if (rows.none { r -> r.second.any { it != null } }) return
    fun hour(t: String) = LocalTime.parse(t).let { it.hour + it.minute / 60f }
    // draws in once, the first time this card appears
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val reveal by animateFloatAsState(if (shown) 1f else 0f, motionTween(Motion.LONG), label = "timing")

    val firsts = ds.mapNotNull { d -> data.entriesOn(d).minOfOrNull { it.time }?.let(::hour) }
    val lasts = ds.mapNotNull { d -> data.entriesOn(d).maxOfOrNull { it.time }?.let(::hour) }
    val dinnerTimes = ds.filterNot { data.isCheatDay(it) }.mapNotNull { d -> data.entriesOn(d).filter { it.meal == Meal.DINNER }.minOfOrNull { it.time }?.let { d to hour(it) } }
    val late = dinnerTimes.filter { it.second >= 22f }.map { it.first }
    val normal = dinnerTimes.filter { it.second < 22f }.map { it.first }
    fun fmt(h: Double) = fmtTime(h.toInt(), ((h % 1) * 60).roundToInt().coerceAtMost(59))
    val windowText = if (firsts.isNotEmpty()) stringResource(R.string.g_ins_window, fmt(firsts.average()), fmt(lasts.average())) else null
    val chartDesc = stringResource(R.string.g_ins_timing_desc, rows.count { r -> r.second.any { it != null } }, windowText ?: "")

    AppCard {
        CardTitle(stringResource(R.string.g_ins_timing_title))
        Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = chartDesc }) {
            fun x(h: Float) = (h - 6f) / 18f * size.width
            listOf(6, 9, 12, 15, 18, 21, 24).forEach { h -> drawLine(c.line, Offset(x(h.toFloat()), 0f), Offset(x(h.toFloat()), size.height), 1f) }
            drawRect(c.chi.copy(alpha = 0.10f), Offset(x(22f), 0f), Size(x(24f) - x(22f), size.height))
            rows.forEachIndexed { i, (_, times) ->
                val y = 10f + i * (size.height - 20f) / 6f
                times.forEachIndexed { mi, t -> if (t != null) drawCircle(colors[mi].copy(alpha = reveal), 5.dp.toPx() * reveal, Offset(x(hour(t).coerceIn(6f, 24f)), y)) }
            }
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) { listOf("6a", "9a", "12p", "3p", "6p", "9p", "12a").forEach { Text(it, style = U.t.caption, color = c.ink2, modifier = Modifier.weight(1f)) } }
        if (late.isNotEmpty()) {
            val lateAvg = late.map { data.entriesOn(it).total().kcal }.average()
            val normAvg = normal.map { data.entriesOn(it).total().kcal }.average().takeIf { !it.isNaN() }
            val base = pluralStringResource(R.plurals.g_ins_late_dinners, late.size, late.size)
            Text(
                "🌙 " + base + (normAvg?.let { " " + stringResource(R.string.g_ins_late_delta, "%+d".format((lateAvg - it).roundToInt())) } ?: "."),
                style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (windowText != null) Text(windowText, style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.g_ins_timing_note), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.Normal)
    }
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
