package app.unavu.ui

import app.unavu.ui.theme.edge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import app.unavu.R
import app.unavu.ui.theme.motionTween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Meal
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.ui.theme.U
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import kotlin.math.abs

/** Four meal chips (plus an optional cheat toggle) in the shared [SelectChip] style. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPicker(selected: Meal?, modifier: Modifier = Modifier, cheatLabel: String? = null, cheat: Boolean = false, onCheat: (() -> Unit)? = null, onSelect: (Meal) -> Unit) {
    FlowRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Meal.entries.forEach { m ->
            SelectChip("${m.emoji} ${m.label}", m == selected, seed = m.ordinal) { onSelect(m) }
        }
        if (cheatLabel != null && onCheat != null) {
            SelectChip(cheatLabel, cheat, role = Role.Checkbox, seed = 9) { onCheat() }
        }
    }
}

/**
 * The one amount stepper used everywhere (Customise, Food detail, plate rows): two 48 dp round buttons around a
 * fixed-width value slot, so nothing shifts as the digits change. The value slides up/down with each step
 * (instant under reduced motion). [itemName] makes TalkBack say "Increase Idli" instead of a bare "Increase".
 * When [onBelowMin] is set, "−" at the minimum calls it (e.g. remove the item) instead of doing nothing.
 */
@Composable
fun Stepper(
    value: Double,
    step: Double,
    min: Double = step,
    modifier: Modifier = Modifier,
    itemName: String? = null,
    slotWidth: androidx.compose.ui.unit.Dp = 48.dp,
    onBelowMin: (() -> Unit)? = null,
    onChange: (Double) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    val aboveMin = value - step >= min - 1e-9
    // at the minimum, "−" removes the item when the caller allows it: say so instead of "Decrease"
    val dec = when {
        itemName != null && !aboveMin && onBelowMin != null -> stringResource(R.string.r7a_remove_item, itemName)
        itemName != null -> stringResource(R.string.f_decrease, itemName)
        else -> stringResource(R.string.d_decrease)
    }
    val inc = if (itemName != null) stringResource(R.string.f_increase, itemName) else stringResource(R.string.d_increase)
    val canDec = aboveMin || onBelowMin != null
    val valueText = fmtQty(value)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepCircle("−", dec, canDec) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (aboveMin) onChange(value - step) else onBelowMin?.invoke()
        }
        // one semantics node for the slot (the outgoing value is still drawn mid-slide), as a polite live region
        // so TalkBack reads the new amount after each tap
        AnimatedContent(
            value, label = "stepperValue", contentAlignment = Alignment.Center,
            modifier = Modifier.width(slotWidth).clearAndSetSemantics { contentDescription = valueText; liveRegion = LiveRegionMode.Polite },
            transitionSpec = {
                if (reduced) EnterTransition.None togetherWith ExitTransition.None
                else {
                    val up = targetState > initialState
                    (slideInVertically(tween(Motion.SHORT, easing = Motion.EmphasizedDecelerate)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(Motion.SHORT))) togetherWith
                        (slideOutVertically(tween(Motion.SHORT, easing = Motion.EmphasizedAccelerate)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(Motion.XSHORT))) using SizeTransform(clip = false)
                }
            },
        ) { q -> Text(fmtQty(q), style = U.t.titleS, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 1) }
        StepCircle("+", inc, true) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(value + step) }
    }
}

@Composable
private fun StepCircle(glyph: String, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val c = U.c
    Box(
        Modifier.size(48.dp).pressScale().clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(34.dp).edge(if (enabled) c.line else c.line.copy(alpha = 0.5f), 17.dp, 1.5.dp, seed = glyph.hashCode()).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, style = U.t.title, color = if (enabled) c.ink else c.ink2.copy(alpha = 0.45f))
        }
    }
}

@Composable
fun SearchField(value: String, placeholder: String, modifier: Modifier = Modifier, focusModifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Row(
        modifier.fillMaxWidth().edge(if (value.isNotEmpty()) c.accInk else c.line, 14.dp, 1.5.dp, seed = 3).clip(RoundedCornerShape(14.dp)).background(c.sf)
            .padding(start = 12.dp, end = 4.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔍", fontSize = 14.sp, modifier = Modifier.clearAndSetSemantics {})
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.hint, style = U.t.bodyS)
            BasicTextField(
                value, onChange, singleLine = true, cursorBrush = SolidColor(c.acc),
                textStyle = U.t.bodyS.copy(color = c.ink), modifier = focusModifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            val clear = stringResource(R.string.d_clear_search)
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { onChange("") }.semantics { contentDescription = clear }, contentAlignment = Alignment.Center) { ArrowIcon(Arrow.CLOSE, c.ink2, 14.dp) }
        }
    }
}

@Composable
fun NumberField(value: String, label: String, modifier: Modifier = Modifier, error: String? = null, onChange: (String) -> Unit) {
    val c = U.c
    Column(modifier.edge(if (error != null) c.chiInk else c.line, 14.dp, 1.dp, seed = 4).clip(RoundedCornerShape(14.dp)).background(c.sf).padding(horizontal = 12.dp, vertical = 8.dp)) {
        BasicTextField(
            value, { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(7)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), cursorBrush = SolidColor(c.acc),
            textStyle = U.t.title.copy(fontWeight = FontWeight.ExtraBold, color = c.ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = label; if (error != null) error(error) },
        )
        Text(label, style = U.t.caption, color = c.ink2, fontWeight = FontWeight.SemiBold)
        if (error != null) Text(error, style = U.t.caption, color = c.chiInk)
    }
}

@Composable
fun TextField(value: String, label: String, modifier: Modifier = Modifier, error: String? = null, onChange: (String) -> Unit) {
    val c = U.c
    Column(modifier.fillMaxWidth().edge(if (error != null) c.chiInk else c.line, 14.dp, 1.dp, seed = 4).clip(RoundedCornerShape(14.dp)).background(c.sf).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, style = U.t.caption, color = c.ink2, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value, onChange, singleLine = true, cursorBrush = SolidColor(c.acc),
            textStyle = U.t.titleS.copy(color = c.ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = label; if (error != null) error(error) },
        )
        if (error != null) Text(error, style = U.t.caption, color = c.chiInk)
    }
}

@Composable
/** Two-button confirmation. [dismissLabel] replaces "Cancel" (e.g. "Keep editing" on a discard prompt). */
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    onConfirm: () -> Unit,
) {
    val c = U.c
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, style = U.t.title) }, text = { Text(text, style = U.t.body, color = c.ink2) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text(confirm, color = if (destructive) c.chiInk else c.accInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(dismissLabel ?: stringResource(R.string.d_cancel), color = c.ink2, style = U.t.label) } },
    )
}

@Composable
/**
 * Single-field dialog. [validate] returns an error message for invalid input (shown under the field, Save disabled),
 * or null when the value is fine.
 */
fun InputDialog(title: String, label: String, initial: String, numeric: Boolean = false, validate: ((String) -> String?)? = null, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = U.c
    var v by rememberSaveable { mutableStateOf(initial) }
    val error = if (v.isBlank()) null else validate?.invoke(v)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, style = U.t.title) },
        text = {
            Column {
                if (numeric) NumberField(v, label) { v = it } else TextField(v, label) { v = it }
                if (error != null) Text(error, style = U.t.caption, color = c.chiInk, modifier = Modifier.padding(top = 6.dp).semantics { error(error) })
            }
        },
        confirmButton = { TextButton({ onSave(v); onDismiss() }, enabled = v.isNotBlank() && error == null) { Text(stringResource(R.string.d_save), color = c.accInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.d_cancel), color = c.ink2, style = U.t.label) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val c = U.c
    val state = rememberTimePickerState(hour, minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(stringResource(R.string.d_pick_time), style = U.t.title) },
        text = {
            TimeInput(state, colors = TimePickerDefaults.colors(
                timeSelectorSelectedContainerColor = c.accS, timeSelectorSelectedContentColor = c.accInk,
                timeSelectorUnselectedContainerColor = c.sf2, timeSelectorUnselectedContentColor = c.ink,
                periodSelectorSelectedContainerColor = c.accS, periodSelectorSelectedContentColor = c.accInk,
                periodSelectorUnselectedContentColor = c.ink2, periodSelectorBorderColor = c.line,
            ))
        },
        confirmButton = { TextButton({ onSave(state.hour, state.minute); onDismiss() }) { Text(stringResource(R.string.d_save), color = c.accInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.d_cancel), color = c.ink2, style = U.t.label) } },
    )
}

fun fmtTime(h: Int, m: Int): String = "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")

// ---------------- charts ----------------

/** Daily bars against a dashed target line. [marks] puts a small label (e.g. 🍕) above a bar. */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    target: Double?,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    marks: List<String?> = emptyList(),
    height: Int = 150,
    contentDescription: String? = null,
) {
    val c = U.c
    val maxV = (values + listOfNotNull(target)).maxOrNull()?.takeIf { it > 0 }?.times(1.12) ?: 1.0
    val reduced = LocalReducedMotion.current
    val long = Motion.LONG
    val stagger = 30
    val totalMs = long + stagger * (values.size - 1).coerceAtLeast(0)
    val prog = remember { Animatable(if (reduced) 1f else 0f) }
    val progSpec = motionTween<Float>(totalMs, LinearEasing)
    LaunchedEffect(Unit) { if (!reduced) prog.animateTo(1f, progSpec) }
    var sel by remember { mutableStateOf(-1) }
    val chipA by animateFloatAsState(if (sel >= 0) 1f else 0f, motionTween(Motion.SHORT), label = "barChip")
    var shownSel by remember { mutableStateOf(0) }
    if (sel >= 0) shownSel = sel
    val measurer = rememberTextMeasurer()
    val chipStyle = U.t.caption.copy(fontWeight = FontWeight.Bold, color = c.bg)
    val targetPart = target?.let { stringResource(R.string.r7a_chart_target, fmtInt(it)) }
    val summary = contentDescription ?: stringResource(
        R.string.d_bar_chart,
        (values.mapIndexed { i, v -> "${labels.getOrElse(i) { "" }.replace("\n", " ")} ${fmtInt(v)}" } + listOfNotNull(targetPart)).joinToString(", "),
    )
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { this.contentDescription = summary }) {
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            Canvas(
                Modifier.fillMaxWidth().height(height.dp).pointerInput(values.size) {
                    detectTapGestures { o ->
                        val i = (o.x / (size.width / values.size.coerceAtLeast(1))).toInt().coerceIn(0, values.size - 1)
                        sel = if (sel == i) -1 else i
                    }
                },
            ) {
                val n = values.size.coerceAtLeast(1)
                val slot = size.width / n
                val bw = (slot * 0.62f).coerceAtMost(28.dp.toPx())
                values.forEachIndexed { i, v ->
                    val raw = ((prog.value * totalMs - stagger * i) / long).coerceIn(0f, 1f)
                    val k = 1f - (1f - raw) * (1f - raw) * (1f - raw)
                    val h = ((v / maxV * size.height).toFloat().coerceAtLeast(if (v > 0) 4f else 3f)) * k
                    val x = slot * i + (slot - bw) / 2
                    val dim = if (sel >= 0 && sel != i) 0.5f else 1f
                    drawRoundRect(colors.getOrElse(i) { c.leaf }.copy(alpha = (if (v > 0) 1f else 0.25f) * dim), Offset(x, size.height - h), Size(bw, h), CornerRadius(6.dp.toPx()))
                }
                target?.let {
                    val y = (size.height - it / maxV * size.height).toFloat()
                    drawLine(c.leaf, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
                if (chipA > 0.01f && shownSel in values.indices) {
                    val v = values[shownSel]
                    val lay = measurer.measure(fmtInt(v), chipStyle)
                    val cw = lay.size.width + 14.dp.toPx(); val ch = lay.size.height + 6.dp.toPx()
                    val barTop = size.height - ((v / maxV * size.height).toFloat().coerceAtLeast(4f))
                    val cx = (slot * shownSel + slot / 2).coerceIn(cw / 2, size.width - cw / 2)
                    val cy = (barTop - ch - 4.dp.toPx()).coerceAtLeast(0f)
                    scale(0.85f + 0.15f * chipA, Offset(cx, cy + ch)) {
                        drawRoundRect(c.ink.copy(alpha = chipA), Offset(cx - cw / 2, cy), Size(cw, ch), CornerRadius(ch / 2))
                        drawText(lay, topLeft = Offset(cx - lay.size.width / 2f, cy + 3.dp.toPx()), alpha = chipA)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text((marks.getOrNull(i)?.let { "$it\n" } ?: "") + l, style = U.t.caption, color = c.ink2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

/** Dots (raw points) + a smooth line (trend). x values are 0..1, y in data units. */
@Composable
fun LineChart(points: List<Pair<Float, Double>>, line: List<Pair<Float, Double>>, modifier: Modifier = Modifier, height: Int = 140, color: Color = U.c.leaf, contentDescription: String? = null) {
    val c = U.c
    val ys = (points + line).map { it.second }
    if (ys.isEmpty()) return
    val minY = ys.min() - 0.4; val maxY = ys.max() + 0.4
    val reduced = LocalReducedMotion.current
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) reveal.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) }
    var sel by remember { mutableStateOf(-1) }
    val chipA by animateFloatAsState(if (sel >= 0) 1f else 0f, motionTween(Motion.SHORT), label = "lineChip")
    var shownSel by remember { mutableStateOf(0) }
    if (sel >= 0) shownSel = sel
    val measurer = rememberTextMeasurer()
    val chipStyle = U.t.caption.copy(fontWeight = FontWeight.Bold, color = c.bg)
    val summary = contentDescription ?: stringResource(R.string.d_line_chart, points.size)
    Canvas(
        modifier.fillMaxWidth().height(height.dp).semantics { this.contentDescription = summary }.pointerInput(points) {
            detectTapGestures { o ->
                if (points.isEmpty()) return@detectTapGestures
                val i = points.indices.minByOrNull { abs(points[it].first * size.width - o.x) } ?: -1
                sel = if (sel == i) -1 else i
            }
        },
    ) {
        fun px(p: Pair<Float, Double>) = Offset(p.first * size.width, (size.height - ((p.second - minY) / (maxY - minY)) * size.height).toFloat())
        val steps = 3
        for (i in 0..steps) {
            val y = size.height * i / steps
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1f)
        }
        points.forEach { if (it.first <= reveal.value + 0.001f) drawCircle(c.ink2.copy(alpha = 0.55f), 3.dp.toPx(), px(it)) }
        if (line.size >= 2) {
            val path = Path()
            line.forEachIndexed { i, p -> val o = px(p); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
            if (reveal.value >= 1f) drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            else {
                val pm = PathMeasure(); pm.setPath(path, false)
                val seg = Path(); pm.getSegment(0f, pm.length * reveal.value, seg, true)
                drawPath(seg, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        if (chipA > 0.01f && shownSel in points.indices) {
            val pt = points[shownSel]; val o = px(pt)
            drawCircle(color.copy(alpha = chipA), 6.dp.toPx(), o)
            val lay = measurer.measure("%.1f".format(pt.second), chipStyle)
            val cw = lay.size.width + 14.dp.toPx(); val ch = lay.size.height + 6.dp.toPx()
            val cx = o.x.coerceIn(cw / 2, size.width - cw / 2)
            val cy = (o.y - ch - 10.dp.toPx()).coerceAtLeast(0f)
            scale(0.85f + 0.15f * chipA, Offset(cx, cy + ch)) {
                drawRoundRect(c.ink.copy(alpha = chipA), Offset(cx - cw / 2, cy), Size(cw, ch), CornerRadius(ch / 2))
                drawText(lay, topLeft = Offset(cx - lay.size.width / 2f, cy + 3.dp.toPx()), alpha = chipA)
            }
        }
    }
}

@Composable
fun ChartAxis(minLabel: String, maxLabel: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(minLabel, style = U.t.caption, color = U.c.ink2, modifier = Modifier.weight(1f))
        Text(maxLabel, style = U.t.caption, color = U.c.ink2)
    }
}
