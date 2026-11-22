package app.unavu.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.ui.theme.TamilFamily
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.ui.theme.motionTween
import app.unavu.ui.theme.roughLine

// Cards, labels, rows and status. Buttons live in Buttons.kt, chips/segments in Selection.kt,
// drawn icons in Icons.kt.

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = U.c.sf,
    border: Color? = U.c.line,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    radius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val sk = U.sketch
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale() else Modifier)
            .then(if (sk) Modifier.edge(U.c.ink.copy(alpha = 0.75f), radius, 1.6.dp, seed = radius.value.toInt()) else Modifier)
            .clip(shape)
            .background(color)
            .then(if (border != null && !sk) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (onClick != null) Modifier.semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
        style = U.t.caption, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = U.c.ink2,
    )
}

@Composable
fun TamilText(text: String, modifier: Modifier = Modifier, size: Float = 12f, color: Color = U.c.ink2) {
    Text(text, modifier = modifier, fontFamily = TamilFamily, fontSize = size.coerceAtLeast(12f).sp, color = color, fontWeight = FontWeight.Medium)
}

/** Centred empty state: big emoji, title, body, optional action. */
@Composable
fun EmptyState(emoji: String, title: String, body: String, actionLabel: String? = null, modifier: Modifier = Modifier, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EmptyIllustration(emoji)
        Spacer(Modifier.height(10.dp))
        Text(title, style = U.t.title, textAlign = TextAlign.Center)
        Text(body, style = U.t.bodyS, color = U.c.ink2, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) ArrowButton(Arrow.BACK, onClick = onBack)
        Text(title, style = U.t.headline, modifier = Modifier.weight(1f).semantics { heading() })
        action?.invoke()
    }
}

enum class Status { OK, LOW, OVER, INFO }

@Composable
fun StatusBadge(text: String, status: Status, modifier: Modifier = Modifier) {
    val c = U.c
    val (bg, fg) = when (status) {
        Status.OK -> c.leafS to c.leafInk
        Status.LOW -> c.turS to c.turInk
        Status.OVER -> c.chiS to c.chiInk
        Status.INFO -> c.blueS to c.blueInk
    }
    Text(
        text, modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        color = fg, style = U.t.caption, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Track(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 7.dp, contentDescription: String? = null) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val f by animateFloatAsState(if (shown) fraction.coerceIn(0f, 1f) else 0f, motionTween(), label = "track")
    val sem = if (contentDescription != null) Modifier.semantics {
        this.contentDescription = contentDescription
        progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
    } else Modifier
    Box(modifier.then(sem).fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(U.c.sf2)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(height)).background(color))
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    val c = U.c
    val haptic = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = onChange?.let { cb -> { v: Boolean -> haptic.performHapticFeedback(if (v) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff); cb(v) } },
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.acc, checkedThumbColor = c.onAcc,
            uncheckedTrackColor = c.sf2, uncheckedThumbColor = c.ink2, uncheckedBorderColor = c.line,
        ),
    )
}

/** A whole row that toggles (Role.Switch, merged semantics, 56dp min). */
@Composable
fun SwitchRow(title: String, checked: Boolean, modifier: Modifier = Modifier, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch) { v ->
                haptic.performHapticFeedback(if (v) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff); onChange(v)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = U.t.body, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = U.t.bodyS, color = U.c.ink2)
        }
        AppSwitch(checked, null)
    }
}

@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: String? = null,
    trailing: String? = "›",
    end: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (leading != null) Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(U.c.sf2), contentAlignment = Alignment.Center) { EmojiText(leading, 17) }
        Column(Modifier.weight(1f)) {
            Text(title, style = U.t.body, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = U.t.caption.copy(fontSize = 13.sp), color = U.c.ink2)
        }
        if (end != null) end() else if (trailing != null) {
            if (trailing == "›") ChevronIcon(modifier = Modifier.clearAndSetSemantics {})
            else Text(trailing, style = U.t.body, color = U.c.ink2, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
fun Divider() {
    if (U.sketch) {
        val col = U.c.ink2.copy(alpha = 0.45f)
        Canvas(Modifier.fillMaxWidth().height(3.dp)) { roughLine(col, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), (size.width).toInt(), 1.dp.toPx()) }
    } else Box(Modifier.fillMaxWidth().height(1.dp).background(U.c.line))
}

/** Line-drawn illustration for [EmptyState]: a chart for 📊/📈, otherwise a bowl with steam. */
@Composable
private fun EmptyIllustration(hint: String) {
    val ink = U.c.ink2; val acc = U.c.acc; val fill = U.c.sf2
    val chart = hint.startsWith("📊") || hint.startsWith("📈")
    Canvas(Modifier.size(132.dp).clearAndSetSemantics {}) {
        val w = size.width; val sw = 3.dp.toPx()
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(fill, radius = w * 0.48f, center = center)
        if (chart) {
            val axis = Path().apply { moveTo(w * 0.24f, w * 0.24f); lineTo(w * 0.24f, w * 0.76f); lineTo(w * 0.78f, w * 0.76f) }
            drawPath(axis, ink, style = st)
            val line = Path().apply { moveTo(w * 0.32f, w * 0.64f); lineTo(w * 0.45f, w * 0.50f); lineTo(w * 0.56f, w * 0.58f); lineTo(w * 0.72f, w * 0.36f) }
            drawPath(line, acc, style = st)
            drawCircle(acc, radius = 4.dp.toPx(), center = Offset(w * 0.72f, w * 0.36f))
        } else {
            val bowl = Path().apply {
                moveTo(w * 0.22f, w * 0.52f); lineTo(w * 0.78f, w * 0.52f)
                cubicTo(w * 0.78f, w * 0.74f, w * 0.64f, w * 0.80f, w * 0.50f, w * 0.80f)
                cubicTo(w * 0.36f, w * 0.80f, w * 0.22f, w * 0.74f, w * 0.22f, w * 0.52f)
            }
            drawPath(bowl, ink, style = st)
            drawLine(ink, Offset(w * 0.38f, w * 0.86f), Offset(w * 0.62f, w * 0.86f), sw, StrokeCap.Round)
            listOf(0.38f, 0.50f, 0.62f).forEachIndexed { i, x ->
                val y = w * (0.40f - 0.04f * (i % 2))
                val steam = Path().apply { moveTo(w * x, w * 0.46f); cubicTo(w * (x - 0.05f), y + w * 0.03f, w * (x + 0.05f), y, w * x, y - w * 0.08f) }
                drawPath(steam, acc, style = st)
            }
        }
    }
}
