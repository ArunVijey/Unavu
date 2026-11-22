package app.unavu.ui

import androidx.compose.ui.geometry.Offset
import app.unavu.ui.theme.roughLine
import app.unavu.ui.theme.edge
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.unavu.R
import app.unavu.ui.theme.motionTween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.ui.theme.TamilFamily
import app.unavu.ui.theme.U

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

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 48.dp).pressScale().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(shape)
                .background(if (selected) c.accS else c.sf)
                .edge(if (selected) c.acc else c.line, 50.dp, 1.5.dp)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(text, style = U.t.label, color = if (selected) c.accInk else c.ink)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = U.c
    Row(
        modifier.fillMaxWidth().selectableGroup().clip(RoundedCornerShape(12.dp)).background(c.sf2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.sf else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { onSelect(i) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = U.t.label, color = if (on) c.ink else c.ink2, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = U.c.acc,
    textColor: Color = U.c.onAcc,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .then(if (U.sketch) Modifier.edge(U.c.ink.copy(alpha = 0.8f), 16.dp, 1.6.dp, seed = 5) else Modifier)
            .then(if (enabled) Modifier.pressScale() else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = U.t.body, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    PrimaryButton(text, modifier, color = U.c.sf2, textColor = U.c.ink, onClick = onClick)

enum class Arrow { BACK, LEFT, RIGHT, CLOSE }

/** Crisp drawn icons (text glyphs like "←" look thin and off-centre). */
@Composable
fun ArrowIcon(dir: Arrow, color: Color = U.c.ink, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path()
        when (dir) {
            Arrow.BACK -> {
                p.moveTo(w * 0.82f, h * 0.5f); p.lineTo(w * 0.2f, h * 0.5f)
                p.moveTo(w * 0.46f, h * 0.22f); p.lineTo(w * 0.18f, h * 0.5f); p.lineTo(w * 0.46f, h * 0.78f)
            }
            Arrow.LEFT -> { p.moveTo(w * 0.62f, h * 0.2f); p.lineTo(w * 0.32f, h * 0.5f); p.lineTo(w * 0.62f, h * 0.8f) }
            Arrow.RIGHT -> { p.moveTo(w * 0.38f, h * 0.2f); p.lineTo(w * 0.68f, h * 0.5f); p.lineTo(w * 0.38f, h * 0.8f) }
            Arrow.CLOSE -> {
                p.moveTo(w * 0.25f, h * 0.25f); p.lineTo(w * 0.75f, h * 0.75f)
                p.moveTo(w * 0.75f, h * 0.25f); p.lineTo(w * 0.25f, h * 0.75f)
            }
        }
        drawPath(p, color, style = stroke)
    }
}

@Composable
fun ArrowButton(dir: Arrow, modifier: Modifier = Modifier, contentDescription: String? = null, onClick: () -> Unit) {
    val desc = contentDescription ?: stringResource(
        when (dir) {
            Arrow.BACK -> R.string.d_back
            Arrow.LEFT -> R.string.d_previous
            Arrow.RIGHT -> R.string.d_next
            Arrow.CLOSE -> R.string.d_close
        },
    )
    Box(
        modifier.size(48.dp).pressScale().clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { this.contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).then(if (U.sketch) Modifier.edge(U.c.ink.copy(alpha = 0.7f), 20.dp, 1.4.dp, seed = dir.ordinal) else Modifier)
                .clip(CircleShape).background(U.c.sf2),
            contentAlignment = Alignment.Center,
        ) { ArrowIcon(dir) }
    }
}

@Composable
fun IconCircle(text: String, modifier: Modifier = Modifier, contentDescription: String? = null, onClick: () -> Unit) {
    Box(
        modifier.size(48.dp).pressScale().clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).then(if (U.sketch) Modifier.edge(U.c.ink.copy(alpha = 0.7f), 20.dp, 1.4.dp, seed = text.length) else Modifier)
                .clip(CircleShape).background(U.c.sf2),
            contentAlignment = Alignment.Center,
        ) { EmojiText(text, 16) }
    }
}

/**
 * An emoji with room to breathe: generous line height and font padding so it is never clipped or
 * squashed by the app font's tighter metrics.
 */
@Composable
fun EmojiText(e: String, size: Int = 20, modifier: Modifier = Modifier) {
    Text(
        e, modifier = modifier, fontSize = size.sp, lineHeight = (size * 1.35f).sp, textAlign = TextAlign.Center,
        style = androidx.compose.ui.text.TextStyle(
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = true),
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
            ),
        ),
    )
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
        if (end != null) end() else if (trailing != null) Text(trailing, style = U.t.body, color = U.c.ink2, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
fun Divider() {
    if (U.sketch) {
        val col = U.c.ink2.copy(alpha = 0.45f)
        Canvas(Modifier.fillMaxWidth().height(3.dp)) { roughLine(col, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), (size.width).toInt(), 1.dp.toPx()) }
    } else Box(Modifier.fillMaxWidth().height(1.dp).background(U.c.line))
}

@Composable
fun Fab(modifier: Modifier = Modifier, size: Dp = 62.dp, contentDescription: String? = null, onClick: () -> Unit) {
    val c = U.c
    val desc = contentDescription ?: stringResource(R.string.d_add_food)
    Box(
        modifier
            .size(size)
            .pressScale()
            .shadow(14.dp, CircleShape, ambientColor = c.acc, spotColor = c.acc)
            .then(if (U.sketch) Modifier.edge(c.onAcc, size / 2, 1.8.dp, seed = 9) else Modifier)
            .clip(CircleShape)
            .background(c.acc)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.36f)) {
            val w = this.size.width; val sw = w * 0.13f
            drawLine(c.onAcc, androidx.compose.ui.geometry.Offset(w / 2, 0f), androidx.compose.ui.geometry.Offset(w / 2, w), sw, StrokeCap.Round)
            drawLine(c.onAcc, androidx.compose.ui.geometry.Offset(0f, w / 2), androidx.compose.ui.geometry.Offset(w, w / 2), sw, StrokeCap.Round)
        }
    }
}

enum class NavGlyph { TODAY, INSIGHTS, MEALS, SETTINGS }

/** Simple line icons for the bottom bar (drawn, so they tint cleanly and stay crisp). */
@Composable
fun NavIcon(g: NavGlyph, color: Color, active: Boolean, bg: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cr = androidx.compose.ui.geometry.CornerRadius(w * 0.2f)
        fun o(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(w * x, h * y)
        fun sz(x: Float, y: Float) = androidx.compose.ui.geometry.Size(w * x, h * y)
        when (g) {
            NavGlyph.TODAY -> {
                // rounded sheet with three lines (filled when active)
                if (active) drawRoundRect(color, o(0.12f, 0.1f), sz(0.76f, 0.8f), cr)
                else drawRoundRect(color, o(0.12f, 0.1f), sz(0.76f, 0.8f), cr, style = stroke)
                val lc = if (active) bg else color
                listOf(0.33f to 0.7f, 0.5f to 0.7f, 0.67f to 0.52f).forEach { (y, x2) -> drawLine(lc, o(0.3f, y), o(x2, y), sw, StrokeCap.Round) }
            }
            NavGlyph.INSIGHTS -> {
                // three rising bars
                listOf(Triple(0.14f, 0.55f, 0.2f), Triple(0.4f, 0.32f, 0.2f), Triple(0.66f, 0.12f, 0.2f)).forEach { (x, top, bw) ->
                    if (active) drawRoundRect(color, o(x, top), sz(bw, 0.88f - top), androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
                    else drawRoundRect(color, o(x, top), sz(bw, 0.88f - top), androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = stroke)
                }
            }
            NavGlyph.MEALS -> {
                // bowl with steam
                val bowl = Path().apply {
                    moveTo(w * 0.1f, h * 0.5f); lineTo(w * 0.9f, h * 0.5f)
                    cubicTo(w * 0.9f, h * 0.78f, w * 0.7f, h * 0.9f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.3f, h * 0.9f, w * 0.1f, h * 0.78f, w * 0.1f, h * 0.5f); close()
                }
                if (active) drawPath(bowl, color) else drawPath(bowl, color, style = stroke)
                listOf(0.36f, 0.5f, 0.64f).forEach { x ->
                    val st = Path().apply { moveTo(w * x, h * 0.36f); cubicTo(w * (x - 0.07f), h * 0.27f, w * (x + 0.07f), h * 0.2f, w * x, h * 0.1f) }
                    drawPath(st, color, style = stroke)
                }
            }
            NavGlyph.SETTINGS -> {
                // two sliders
                drawLine(color, o(0.12f, 0.32f), o(0.88f, 0.32f), sw, StrokeCap.Round)
                drawLine(color, o(0.12f, 0.68f), o(0.88f, 0.68f), sw, StrokeCap.Round)
                listOf(o(0.36f, 0.32f), o(0.64f, 0.68f)).forEach { p ->
                    drawCircle(if (active) color else bg, w * 0.13f, p)
                    drawCircle(color, w * 0.13f, p, style = stroke)
                }
            }
        }
    }
}

/** Slight shrink (0.97) while pressed. Does not consume the touch; off when reduced motion is on. */
fun Modifier.pressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val reduced = app.unavu.ui.theme.LocalReducedMotion.current
    val scale by animateFloatAsState(if (pressed && !reduced) 0.97f else 1f, motionTween(app.unavu.ui.theme.Motion.SHORT), label = "press")
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                do {
                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                } while (ev.changes.any { it.pressed })
                pressed = false
            }
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
}


enum class LineGlyph { CALENDAR, MORE }

/** Drawn line icon (no emoji, no date-specific text) in a 48dp touch target with a 40dp circle. */
@Composable
fun GlyphCircle(glyph: LineGlyph, contentDescription: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ink = U.c.ink
    Box(
        modifier.size(48.dp).pressScale().clip(CircleShape)
            .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).then(if (U.sketch) Modifier.edge(ink.copy(alpha = 0.7f), 20.dp, 1.4.dp, seed = glyph.ordinal + 3) else Modifier)
                .clip(CircleShape).background(U.c.sf2),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width; val h = size.height
                val sw = w * 0.1f
                val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                when (glyph) {
                    LineGlyph.CALENDAR -> {
                        drawRoundRect(ink, Offset(w * 0.08f, h * 0.17f), androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.75f), androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = st)
                        drawLine(ink, Offset(w * 0.08f, h * 0.43f), Offset(w * 0.92f, h * 0.43f), sw, StrokeCap.Round)
                        drawLine(ink, Offset(w * 0.3f, h * 0.05f), Offset(w * 0.3f, h * 0.27f), sw, StrokeCap.Round)
                        drawLine(ink, Offset(w * 0.7f, h * 0.05f), Offset(w * 0.7f, h * 0.27f), sw, StrokeCap.Round)
                    }
                    LineGlyph.MORE -> listOf(0.2f, 0.5f, 0.8f).forEach { x -> drawCircle(ink, w * 0.09f, Offset(w * x, h * 0.5f)) }
                }
            }
        }
    }
}

/** 48dp circular "+" with an accent outline. Pass a null [onClick] when a parent row handles the tap. */
@Composable
fun PlusRingButton(contentDescription: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val haptic = LocalHapticFeedback.current
    val acc = U.c.acc
    val ink = U.c.ink
    val base = modifier.size(48.dp).clip(CircleShape)
    val click = if (onClick != null) Modifier.pressScale().clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() } else Modifier
    val sem = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(base.then(click).then(sem), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            drawCircle(acc, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            val a = size.width * 0.17f; val m = size.width / 2
            drawLine(ink, Offset(m - a, m), Offset(m + a, m), 2.2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(m, m - a), Offset(m, m + a), 2.2.dp.toPx(), StrokeCap.Round)
        }
    }
}
