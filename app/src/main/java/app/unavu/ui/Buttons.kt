package app.unavu.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.ui.theme.motionTween

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
            .background(if (enabled) color else U.c.sf2)
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) textColor else U.c.ink2, style = U.t.body, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    PrimaryButton(text, modifier, color = U.c.sf2, textColor = U.c.ink, onClick = onClick)

/**
 * Round arrow button (back, close, previous/next). [enabled] = false draws it at 38 % alpha, ignores taps and
 * exposes disabled semantics (e.g. "Next week" on the current week).
 */
@Composable
fun ArrowButton(dir: Arrow, modifier: Modifier = Modifier, contentDescription: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val desc = contentDescription ?: stringResource(
        when (dir) {
            Arrow.BACK -> R.string.d_back
            Arrow.LEFT -> R.string.d_previous
            Arrow.RIGHT -> R.string.d_next
            Arrow.CLOSE -> R.string.d_close
        },
    )
    Box(
        modifier.size(48.dp).then(if (enabled) Modifier.pressScale() else Modifier).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { this.contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).graphicsLayer { alpha = if (enabled) 1f else 0.38f }
                .then(if (U.sketch) Modifier.edge(U.c.ink.copy(alpha = 0.7f), 20.dp, 1.4.dp, seed = dir.ordinal) else Modifier)
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
                val st = Stroke(width = sw, cap = StrokeCap.Round)
                when (glyph) {
                    LineGlyph.CALENDAR -> {
                        drawRoundRect(ink, Offset(w * 0.08f, h * 0.17f), Size(w * 0.84f, h * 0.75f), CornerRadius(w * 0.16f), style = st)
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

/** The two add-button tones: [FILLED] for the one main call to action on a card, [SOFT] for everything else. */
enum class AddTone { FILLED, SOFT }

/**
 * The one "add" affordance: a drawn plus with an optional label, in a 48dp touch target.
 * [text] null gives a round icon-only button (then pass [contentDescription], or null when a
 * parent row handles the tap and announces itself). Pass a null [onClick] when a parent handles it.
 */
@Composable
fun AddButton(
    text: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: AddTone = AddTone.SOFT,
    onClick: (() -> Unit)? = null,
) {
    val c = U.c
    val haptic = LocalHapticFeedback.current
    val (fill, ink, stroke) = when (tone) {
        AddTone.FILLED -> Triple(c.acc, c.onAcc, if (U.sketch) c.ink.copy(alpha = 0.8f) else Color.Transparent)
        AddTone.SOFT -> Triple(c.accS, c.accInk, c.accInk)
    }
    val click = if (onClick != null) {
        Modifier.pressScale().clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
    } else Modifier
    val sem = when {
        contentDescription != null -> Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
        onClick == null -> Modifier.clearAndSetSemantics {}
        else -> Modifier
    }
    Box(modifier.heightIn(min = 48.dp).then(if (text == null) Modifier.size(48.dp) else Modifier).clip(RoundedCornerShape(50)).then(click).then(sem), contentAlignment = Alignment.Center) {
        val surface = Modifier.edge(stroke, 50.dp, 1.5.dp, seed = 11).clip(RoundedCornerShape(50)).background(fill)
        if (text == null) {
            Box(surface.size(40.dp), contentAlignment = Alignment.Center) { PlusGlyph(ink, 16.dp) }
        } else {
            Row(
                surface.height(36.dp).padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PlusGlyph(ink, 12.dp)
                Text(text, color = ink, style = U.t.label, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlusGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val m = w / 2; val sw = 2.2.dp.toPx()
        drawLine(color, Offset(m, 0f), Offset(m, w), sw, StrokeCap.Round)
        drawLine(color, Offset(0f, m), Offset(w, m), sw, StrokeCap.Round)
    }
}

/** Icon-only [AddButton]. Kept for existing callers. */
@Composable
fun PlusRingButton(contentDescription: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) =
    AddButton(null, contentDescription, modifier, AddTone.SOFT, onClick)

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
            drawLine(c.onAcc, Offset(w / 2, 0f), Offset(w / 2, w), sw, StrokeCap.Round)
            drawLine(c.onAcc, Offset(0f, w / 2), Offset(w, w / 2), sw, StrokeCap.Round)
        }
    }
}

/** Slight shrink (0.97) while pressed. Does not consume the touch; off when reduced motion is on. */
fun Modifier.pressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (pressed && !reduced) 0.97f else 1f, motionTween(Motion.SHORT), label = "press")
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
