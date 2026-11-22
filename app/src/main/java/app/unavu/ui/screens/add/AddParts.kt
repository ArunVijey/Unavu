package app.unavu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import app.unavu.ui.pressScale
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.NewItem
import app.unavu.data.fmtQty
import app.unavu.data.nutrFor
import app.unavu.data.unitLabel
import app.unavu.ui.EmojiText
import app.unavu.ui.LocalData
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.data.FoodDb
import kotlin.math.roundToInt

@Composable
internal fun ParsedRow(item: NewItem, onClick: () -> Unit) {
    val food = FoodDb.get(item.foodId) ?: return
    val data = LocalData.current
    val t = U.t
    val remembered = data.portions[item.foodId]?.let { it.qty == item.qty && it.unit == item.unit } == true
    val summary = food.choiceSummary(item.choices)
    val kcal = food.nutrFor(item.qty, item.unit, item.choices).kcal.roundToInt()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${food.emoji} ${food.name} × ${fmtQty(item.qty)} ${unitLabel(item.unit, item.qty)}",
                style = t.bodyS,
            )
            if (summary.isNotBlank()) Text(summary, style = t.caption, color = U.c.leafInk, fontWeight = FontWeight.SemiBold)
            if (remembered || food.liquid) {
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (remembered) StatusBadge(stringResource(R.string.f_your_usual), Status.OK)
                    if (food.liquid) StatusBadge(stringResource(R.string.f_to_liquids), Status.INFO)
                }
            }
        }
        Text(stringResource(R.string.f_kcal, kcal), style = t.bodyS, fontWeight = FontWeight.Bold)
    }
}

/** Big accent button. [enabled] = false dims it; the label morphs as it changes; [success] swaps it for a drawn check. */
@Composable
fun AddButton(text: String, enabled: Boolean, success: Boolean = false, onClick: () -> Unit) {
    val c = U.c
    val bg by animateColorAsState(if (enabled || success) c.acc else c.acc.copy(alpha = 0.35f), motionTween(), label = "addBg")
    val prog = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(success) {
        if (success) prog.animateTo(1f, motionTween(reduced, Motion.LONG)) else prog.snapTo(0f)
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).pressScale().clip(RoundedCornerShape(18.dp)).background(bg)
            .clickable(enabled = enabled && !success, role = Role.Button, onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (success) {
            Canvas(Modifier.size(22.dp)) {
                val p = Path().apply { moveTo(size.width * 0.12f, size.height * 0.55f); lineTo(size.width * 0.4f, size.height * 0.82f); lineTo(size.width * 0.9f, size.height * 0.22f) }
                val seg = PathMeasure().also { it.setPath(p, false) }
                val out = Path()
                seg.getSegment(0f, seg.length * prog.value, out, true)
                drawPath(out, c.onAcc, style = Stroke(size.width * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        } else {
            AnimatedContent(
                text, label = "addLabel",
                transitionSpec = { if (reduced) EnterTransition.None togetherWith ExitTransition.None else (fadeIn(motionTween(false, Motion.SHORT)) + slideInVertically(motionTween(false, Motion.SHORT)) { it / 3 }) togetherWith fadeOut(motionTween(false, Motion.XSHORT, Motion.Standard)) },
            ) { s -> Text(s, style = U.t.body, fontWeight = FontWeight.ExtraBold, color = if (enabled) c.onAcc else c.ink2) }
        }
    }
}

/** Segmented control with an accent thumb that slides between the options. */
@Composable
fun <T> SlidingSegments(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val c = U.c
    val idx = options.indexOf(selected).coerceAtLeast(0)
    val segW = 92.dp
    val off by animateDpAsState(segW * idx, motionTween(Motion.MEDIUM, Motion.Emphasized), label = "segThumb")
    Box(Modifier.clip(RoundedCornerShape(50)).background(c.sf2).padding(4.dp).selectableGroup()) {
        Box(Modifier.offset(x = off).size(segW, 44.dp).clip(RoundedCornerShape(50)).background(c.acc))
        Row {
            options.forEach { o ->
                val on = o == selected
                val col by animateColorAsState(if (on) c.onAcc else c.ink2, motionTween(), label = "segCol")
                Box(
                    Modifier.size(segW, 44.dp).clip(RoundedCornerShape(50)).selectable(selected = on, role = Role.Tab) { if (!on) onSelect(o) },
                    contentAlignment = Alignment.Center,
                ) { Text(label(o), style = U.t.label, fontWeight = FontWeight.Bold, color = col) }
            }
        }
    }
}

@Composable
fun LiquidTile(emoji: String, name: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    val t = U.t
    val fill by animateColorAsState(if (on) c.blueS else c.sf, motionTween(), label = "liqFill")
    val line by animateColorAsState(if (on) c.blue else c.line, motionTween(), label = "liqLine")
    Column(
        modifier.pressScale().edge(line, 14.dp, 1.5.dp, seed = name.length).clip(RoundedCornerShape(14.dp)).background(fill)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick).heightIn(min = 48.dp).padding(top = 12.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmojiText(emoji, 24)
        Spacer(Modifier.height(4.dp))
        Text(name, style = t.label, fontWeight = FontWeight.Bold)
        Text(sub, style = t.caption, color = c.ink2)
    }
}

/** Fades + slides its content in after a 40ms-per-index stagger. */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) p.animateTo(1f, motionTween(false, Motion.MEDIUM, delayMs = index * 40)) }
    Box(Modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 10.dp.toPx() }) { content() }
}
