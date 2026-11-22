package app.unavu.ui

import app.unavu.ui.theme.edge
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
import app.unavu.data.fmtQty
import app.unavu.ui.theme.U

/** Four meal chips. Nothing is picked until the user taps one (no guessing from the clock). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPicker(selected: Meal?, modifier: Modifier = Modifier, onSelect: (Meal) -> Unit) {
    val c = U.c
    FlowRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Meal.entries.forEach { m ->
            val on = m == selected
            Box(Modifier.heightIn(min = 48.dp).pressScale().selectable(selected = on, role = Role.RadioButton) { onSelect(m) }, contentAlignment = Alignment.Center) {
                Text(
                    "${m.emoji} ${m.label}", style = U.t.label, color = if (on) c.accInk else c.ink,
                    modifier = Modifier.edge(if (on) c.acc else Color.Transparent, 50.dp, 1.5.dp, seed = m.ordinal).clip(RoundedCornerShape(50)).background(if (on) c.accS else c.sf2)
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
fun Stepper(value: Double, step: Double, min: Double = step, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    val c = U.c
    val haptic = LocalHapticFeedback.current
    val dec = stringResource(R.string.d_decrease)
    val inc = stringResource(R.string.d_increase)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange((value - step).coerceAtLeast(min)) }
                .semantics { contentDescription = dec },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, c.line, CircleShape), contentAlignment = Alignment.Center) { Text("−", style = U.t.title) }
        }
        Text(fmtQty(value), style = U.t.titleS, fontWeight = FontWeight.ExtraBold, modifier = Modifier.widthIn(min = 36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(value + step) }
                .semantics { contentDescription = inc },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, c.line, CircleShape), contentAlignment = Alignment.Center) { Text("+", style = U.t.title) }
        }
    }
}

@Composable
fun SearchField(value: String, placeholder: String, modifier: Modifier = Modifier, focusModifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Row(
        modifier.fillMaxWidth().edge(if (value.isNotEmpty()) c.acc else c.line, 14.dp, 1.5.dp, seed = 3).clip(RoundedCornerShape(14.dp)).background(c.sf)
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
fun ConfirmDialog(title: String, text: String, confirm: String, destructive: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = U.c
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, style = U.t.title) }, text = { Text(text, style = U.t.body, color = c.ink2) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text(confirm, color = if (destructive) c.chiInk else c.accInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.d_cancel), color = c.ink2, style = U.t.label) } },
    )
}

@Composable
fun InputDialog(title: String, label: String, initial: String, numeric: Boolean = false, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = U.c
    var v by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, style = U.t.title) },
        text = { if (numeric) NumberField(v, label) { v = it } else TextField(v, label) { v = it } },
        confirmButton = { TextButton({ onSave(v); onDismiss() }, enabled = v.isNotBlank()) { Text(stringResource(R.string.d_save), color = c.accInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
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
    val reduced = app.unavu.ui.theme.LocalReducedMotion.current
    val long = app.unavu.ui.theme.Motion.LONG
    val stagger = 30
    val totalMs = long + stagger * (values.size - 1).coerceAtLeast(0)
    val prog = remember { Animatable(if (reduced) 1f else 0f) }
    val progSpec = app.unavu.ui.theme.motionTween<Float>(totalMs, androidx.compose.animation.core.LinearEasing)
    LaunchedEffect(Unit) { if (!reduced) prog.animateTo(1f, progSpec) }
    var sel by remember { mutableStateOf(-1) }
    val chipA by animateFloatAsState(if (sel >= 0) 1f else 0f, motionTween(app.unavu.ui.theme.Motion.SHORT), label = "barChip")
    var shownSel by remember { mutableStateOf(0) }
    if (sel >= 0) shownSel = sel
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val chipStyle = U.t.caption.copy(fontWeight = FontWeight.Bold, color = c.bg)
    val summary = contentDescription ?: stringResource(
        R.string.d_bar_chart,
        values.mapIndexed { i, v -> "${labels.getOrElse(i) { "" }.replace("\n", " ")} ${v.toInt()}" }.joinToString(", ") + (target?.let { ", target ${it.toInt()}" } ?: ""),
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
                    val lay = measurer.measure(v.toInt().toString(), chipStyle)
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
                Text((marks.getOrNull(i)?.let { "$it\n" } ?: "") + l, style = U.t.caption, color = c.ink2, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    val reduced = app.unavu.ui.theme.LocalReducedMotion.current
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) reveal.animateTo(1f, androidx.compose.animation.core.tween(app.unavu.ui.theme.Motion.LONG, easing = app.unavu.ui.theme.Motion.EmphasizedDecelerate)) }
    var sel by remember { mutableStateOf(-1) }
    val chipA by animateFloatAsState(if (sel >= 0) 1f else 0f, motionTween(app.unavu.ui.theme.Motion.SHORT), label = "lineChip")
    var shownSel by remember { mutableStateOf(0) }
    if (sel >= 0) shownSel = sel
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val chipStyle = U.t.caption.copy(fontWeight = FontWeight.Bold, color = c.bg)
    val summary = contentDescription ?: stringResource(R.string.d_line_chart, points.size)
    Canvas(
        modifier.fillMaxWidth().height(height.dp).semantics { this.contentDescription = summary }.pointerInput(points) {
            detectTapGestures { o ->
                if (points.isEmpty()) return@detectTapGestures
                val i = points.indices.minByOrNull { kotlin.math.abs(points[it].first * size.width - o.x) } ?: -1
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
                val pm = androidx.compose.ui.graphics.PathMeasure(); pm.setPath(path, false)
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
