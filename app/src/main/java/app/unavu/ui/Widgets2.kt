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
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Meal.entries.forEach { m ->
            val on = m == selected
            Text(
                "${m.emoji} ${m.label}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.accInk else c.ink,
                modifier = Modifier.edge(if (on) c.acc else Color.Transparent, 50.dp, 1.5.dp, seed = m.ordinal).clip(RoundedCornerShape(50)).background(if (on) c.accS else c.sf2)
                    .clickable { onSelect(m) }.padding(horizontal = 13.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
fun Stepper(value: Double, step: Double, min: Double = step, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    val c = U.c
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, c.line, CircleShape).clickable { onChange((value - step).coerceAtLeast(min)) }, contentAlignment = Alignment.Center) {
            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(fmtQty(value), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, c.line, CircleShape).clickable { onChange(value + step) }, contentAlignment = Alignment.Center) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SearchField(value: String, placeholder: String, modifier: Modifier = Modifier, focusModifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Row(
        modifier.fillMaxWidth().edge(if (value.isNotEmpty()) c.acc else c.line, 14.dp, 1.5.dp, seed = 3).clip(RoundedCornerShape(14.dp)).background(c.sf)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔍", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.ink2, fontSize = 14.sp)
            BasicTextField(
                value, onChange, singleLine = true, cursorBrush = SolidColor(c.acc),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = c.ink), modifier = focusModifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(22.dp).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) { ArrowIcon(Arrow.CLOSE, c.ink2, 14.dp) }
        }
    }
}

@Composable
fun NumberField(value: String, label: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Column(modifier.edge(c.line, 14.dp, 1.dp, seed = 4).clip(RoundedCornerShape(14.dp)).background(c.sf).padding(horizontal = 12.dp, vertical = 8.dp)) {
        BasicTextField(
            value, { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(7)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), cursorBrush = SolidColor(c.acc),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink), modifier = Modifier.fillMaxWidth(),
        )
        Text(label, fontSize = 11.sp, color = c.ink2, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TextField(value: String, label: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Column(modifier.fillMaxWidth().edge(c.line, 14.dp, 1.dp, seed = 4).clip(RoundedCornerShape(14.dp)).background(c.sf).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, color = c.ink2, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value, onChange, singleLine = true, cursorBrush = SolidColor(c.acc),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink), modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, destructive: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = U.c
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, fontWeight = FontWeight.Bold) }, text = { Text(text, color = c.ink2) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text(confirm, color = if (destructive) c.chi else c.accInk, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = c.ink2) } },
    )
}

@Composable
fun InputDialog(title: String, label: String, initial: String, numeric: Boolean = false, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = U.c
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { if (numeric) NumberField(v, label) { v = it } else TextField(v, label) { v = it } },
        confirmButton = { TextButton({ onSave(v); onDismiss() }, enabled = v.isNotBlank()) { Text("Save", color = c.accInk, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = c.ink2) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val c = U.c
    val state = rememberTimePickerState(hour, minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text("Pick a time", fontWeight = FontWeight.Bold) },
        text = {
            TimeInput(state, colors = TimePickerDefaults.colors(
                timeSelectorSelectedContainerColor = c.accS, timeSelectorSelectedContentColor = c.accInk,
                timeSelectorUnselectedContainerColor = c.sf2, timeSelectorUnselectedContentColor = c.ink,
                periodSelectorSelectedContainerColor = c.accS, periodSelectorSelectedContentColor = c.accInk,
                periodSelectorUnselectedContentColor = c.ink2, periodSelectorBorderColor = c.line,
            ))
        },
        confirmButton = { TextButton({ onSave(state.hour, state.minute); onDismiss() }) { Text("Save", color = c.accInk, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = c.ink2) } },
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
) {
    val c = U.c
    val maxV = (values + listOfNotNull(target)).maxOrNull()?.takeIf { it > 0 }?.times(1.12) ?: 1.0
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            Canvas(Modifier.fillMaxWidth().height(height.dp)) {
                val n = values.size.coerceAtLeast(1)
                val slot = size.width / n
                val bw = (slot * 0.62f).coerceAtMost(28.dp.toPx())
                values.forEachIndexed { i, v ->
                    val h = (v / maxV * size.height).toFloat().coerceAtLeast(if (v > 0) 4f else 3f)
                    val x = slot * i + (slot - bw) / 2
                    drawRoundRect(colors.getOrElse(i) { c.leaf }.copy(alpha = if (v > 0) 1f else 0.25f), Offset(x, size.height - h), Size(bw, h), CornerRadius(6.dp.toPx()))
                }
                target?.let {
                    val y = (size.height - it / maxV * size.height).toFloat()
                    drawLine(c.leaf, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text((marks.getOrNull(i)?.let { "$it\n" } ?: "") + l, fontSize = 10.sp, color = c.ink2, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
            }
        }
    }
}

/** Dots (raw points) + a smooth line (trend). x values are 0..1, y in data units. */
@Composable
fun LineChart(points: List<Pair<Float, Double>>, line: List<Pair<Float, Double>>, modifier: Modifier = Modifier, height: Int = 140, color: Color = U.c.leaf) {
    val c = U.c
    val ys = (points + line).map { it.second }
    if (ys.isEmpty()) return
    val minY = ys.min() - 0.4; val maxY = ys.max() + 0.4
    Canvas(modifier.fillMaxWidth().height(height.dp)) {
        fun px(p: Pair<Float, Double>) = Offset(p.first * size.width, (size.height - ((p.second - minY) / (maxY - minY)) * size.height).toFloat())
        val steps = 3
        for (i in 0..steps) {
            val y = size.height * i / steps
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1f)
        }
        points.forEach { drawCircle(c.ink2.copy(alpha = 0.55f), 3.dp.toPx(), px(it)) }
        if (line.size >= 2) {
            val path = Path()
            line.forEachIndexed { i, p -> val o = px(p); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
            drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun ChartAxis(minLabel: String, maxLabel: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(minLabel, fontSize = 10.sp, color = U.c.ink2, modifier = Modifier.weight(1f))
        Text(maxLabel, fontSize = 10.sp, color = U.c.ink2)
    }
}
