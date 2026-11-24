package app.unavu.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalDensity
import app.unavu.data.fmtInt
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.R
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.Nutr
import app.unavu.data.NutrientDef
import app.unavu.data.Targets
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.screens.statusOf
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.U
import app.unavu.ui.theme.hachure
import app.unavu.ui.theme.motionTween
import app.unavu.ui.theme.roughArc
import app.unavu.ui.theme.roughRect
import kotlin.math.abs
import kotlin.math.roundToInt

/** Above this font scale, secondary lines wrap under their value instead of sharing a row with it. */
internal const val LARGE_FONT_SCALE = 1.15f

/** Today's hero: kcal left, the per-meal budget bar and the nutrient tiles. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetHero(
    modifier: Modifier, kcal: Int, eaten: Int, left: Int, byMeal: Map<Meal, List<LogEntry>>, colors: List<Color>,
    total: Nutr, tg: Targets, homeChips: List<String>, onBudget: () -> Unit, onChip: (NutrientDef) -> Unit,
) {
    val c = U.c
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { armed = true }
    // Every number here counts to its new value (Motion.LONG) when food is added, removed or the day changes.
    val shownLeft by animateIntAsState(if (armed) abs(left) else 0, motionTween(Motion.LONG), label = "left")
    val shownEaten by animateIntAsState(if (armed) eaten else 0, motionTween(Motion.LONG), label = "eaten")
    val mealKcal = Meal.entries.map { m -> byMeal.getValue(m).total().kcal.roundToInt() }
    // Each meal's segment eases to its new width instead of jumping.
    val parts = Meal.entries.map { m ->
        val target = if (kcal > 0) mealKcal[m.ordinal] / kcal.toFloat() else 0f
        animateFloatAsState(if (armed) target else 0f, motionTween(Motion.LONG), label = "part-${m.name}").value
    }
    val scale = if (parts.sum() > 1f) 1f / parts.sum() else 1f
    val breakdown = Meal.entries.mapIndexed { i, m -> stringResource(R.string.r7c_meal_kcal, m.label, fmtInt(mealKcal[i])) }.joinToString(" ")
    val summary = if (left >= 0) stringResource(R.string.r7c_budget_summary_left, fmtInt(left), fmtInt(kcal), fmtInt(eaten), breakdown)
    else stringResource(R.string.r7c_budget_summary_over, fmtInt(-left), fmtInt(kcal), fmtInt(eaten), breakdown)
    val eatenLine = stringResource(R.string.r7c_eaten_of_line, fmtInt(shownEaten), fmtInt(kcal))
    val openDesc = stringResource(R.string.d_budget_open)
    val stackEaten = LocalDensity.current.fontScale > LARGE_FONT_SCALE

    AppCard(modifier) {
        // "1730 kcal" and "343 eaten of 2073" share one baseline.
        Column(Modifier.clearAndSetSemantics { contentDescription = summary }) {
            Text(stringResource(if (left >= 0) R.string.d_can_still_eat else R.string.d_over_target_by), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2, letterSpacing = 0.8.sp)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val big = U.t.display
                Text(fmtInt(shownLeft), style = big.copy(fontSize = big.fontSize * 1.5f, lineHeight = big.lineHeight * 1.5f), color = if (left >= 0) c.ink else c.chiInk, modifier = Modifier.alignByBaseline())
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.d_kcal_unit), style = U.t.bodyS, fontWeight = FontWeight.SemiBold, color = c.ink2, modifier = Modifier.alignByBaseline())
                Spacer(Modifier.weight(1f))
                if (U.sketch) {
                    val frac = (shownEaten / kcal.toFloat()).coerceIn(0f, 1f)
                    val track = c.ink2.copy(alpha = 0.25f); val ringCol = if (left >= 0) c.acc else c.chi
                    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(92.dp)) {
                            val r = size.minDimension / 2 - 7.dp.toPx()
                            roughArc(track, center, r, 0f, 360f, 3, 7.dp.toPx())
                            roughArc(ringCol, center, r, -90f, 360f * frac, 7, 7.dp.toPx())
                        }
                        Text("${(shownEaten * 100f / kcal).roundToInt()}%", style = U.t.title)
                    }
                } else if (!stackEaten) {
                    Text(eatenLine, style = U.t.label, color = c.ink2, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.alignByBaseline().padding(start = 8.dp))
                }
            }
            // Sketch (ring on the right) and large font scales: the line moves under the number instead of truncating.
            if (U.sketch || stackEaten) Text(eatenLine, style = U.t.label, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
        }
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
            FlowRow(
                Modifier.clearAndSetSemantics {}.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Meal.entries.forEachIndexed { i, m -> LegendDot(colors[i], m.label) }
            }
        }
        Spacer(Modifier.height(14.dp))
        val chips = homeChips.mapNotNull { k -> NUTRIENTS.firstOrNull { it.key == k } }
        chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { d ->
                    val have = d.value(total); val target = d.target(tg)
                    NutrientChip(d, have, target, statusOf(d, have, target, eaten / kcal.toDouble()), Modifier.weight(1f), logged = eaten >= kcal * 0.3) { onChip(d) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
