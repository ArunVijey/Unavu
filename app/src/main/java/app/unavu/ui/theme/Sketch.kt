package app.unavu.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * "Sketch" style: everything looks hand-drawn, like an Excalidraw whiteboard.
 * Outlines are drawn as two slightly different wobbly strokes (the rough.js trick), deterministic per
 * element so they don't jitter between frames.
 */
val LocalSketch = staticCompositionLocalOf { false }

/** Smooth, seeded wobble in px for position t (px along the outline). */
private fun wobble(t: Float, seed: Int, amp: Float): Float {
    val s = seed * 1.618f
    return amp * (0.6f * sin(t * 0.045f + s) + 0.3f * sin(t * 0.11f + s * 2.3f) + 0.1f * sin(t * 0.27f + s * 4.1f))
}

/** Points along a rounded rectangle, with small overshoot so the start/end don't meet perfectly. */
private fun roughRoundRectPath(size: Size, r: Float, seed: Int, amp: Float, inset: Float): Path {
    val w = size.width - inset * 2; val h = size.height - inset * 2
    val rr = min(r, min(w, h) / 2)
    val pts = ArrayList<Offset>()
    fun seg(x0: Float, y0: Float, x1: Float, y1: Float) {
        val len = hypot(x1 - x0, y1 - y0); val n = max(2, (len / 10f).toInt())
        for (i in 0 until n) { val f = i / n.toFloat(); pts += Offset(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f) }
    }
    fun arc(cx: Float, cy: Float, a0: Float, a1: Float) {
        val n = max(2, (rr * (a1 - a0) / 8f).toInt())
        for (i in 0 until n) { val a = a0 + (a1 - a0) * i / n; pts += Offset(cx + rr * cos(a), cy + rr * sin(a)) }
    }
    val l = inset; val t = inset; val rt = inset + w; val b = inset + h
    seg(l + rr, t, rt - rr, t); arc(rt - rr, t + rr, -PI.toFloat() / 2, 0f)
    seg(rt, t + rr, rt, b - rr); arc(rt - rr, b - rr, 0f, PI.toFloat() / 2)
    seg(rt - rr, b, l + rr, b); arc(l + rr, b - rr, PI.toFloat() / 2, PI.toFloat())
    seg(l, b - rr, l, t + rr); arc(l + rr, t + rr, PI.toFloat(), PI.toFloat() * 1.5f)
    val cx = size.width / 2; val cy = size.height / 2
    val path = Path()
    var dist = 0f
    // go round once plus a little overshoot (like a pen stroke that doesn't close exactly)
    val total = pts.size + max(2, pts.size / 14)
    for (i in 0..total) {
        val p = pts[i % pts.size]
        if (i > 0) dist += hypot(p.x - pts[(i - 1) % pts.size].x, p.y - pts[(i - 1) % pts.size].y)
        // push the point outward/inward from the centre
        val dx = p.x - cx; val dy = p.y - cy; val d = max(1f, hypot(dx, dy))
        val o = wobble(dist, seed, amp) + if (i >= pts.size) amp * 0.8f else 0f
        val q = Offset(p.x + dx / d * o, p.y + dy / d * o)
        if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y)
    }
    return path
}

/** A hand-drawn outline (two passes) inside the given bounds. */
fun DrawScope.roughRect(color: Color, radius: Float, seed: Int, width: Float, amp: Float = 1.4.dp.toPx(), size: Size = this.size) {
    val st = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(roughRoundRectPath(size, radius, seed, amp, width), color, style = st)
    drawPath(roughRoundRectPath(size, radius, seed + 17, amp * 0.8f, width), color.copy(alpha = color.alpha * 0.55f), style = Stroke(width * 0.8f, cap = StrokeCap.Round))
}

/** A hand-drawn straight line. */
fun DrawScope.roughLine(color: Color, from: Offset, to: Offset, seed: Int, width: Float) {
    val len = hypot(to.x - from.x, to.y - from.y); val n = max(2, (len / 10f).toInt())
    val nx = -(to.y - from.y) / max(1f, len); val ny = (to.x - from.x) / max(1f, len)
    val p = Path()
    for (i in 0..n) {
        val f = i / n.toFloat(); val o = wobble(f * len, seed, 1.1.dp.toPx())
        val x = from.x + (to.x - from.x) * f + nx * o; val y = from.y + (to.y - from.y) * f + ny * o
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round))
}

/** A hand-drawn arc (angles in degrees, 0 = 3 o'clock, clockwise). */
fun DrawScope.roughArc(color: Color, center: Offset, radius: Float, startDeg: Float, sweepDeg: Float, seed: Int, width: Float) {
    if (sweepDeg <= 0f) return
    val n = max(8, (sweepDeg / 4f).toInt())
    val p = Path()
    for (i in 0..n) {
        val a = Math.toRadians((startDeg + sweepDeg * i / n).toDouble()).toFloat()
        val r = radius + wobble(i * 9f, seed, 1.3.dp.toPx())
        val x = center.x + r * cos(a); val y = center.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Diagonal hatching that stays INSIDE [rect] (clipped), like Excalidraw's hachure fill. */
fun DrawScope.hachure(color: Color, rect: Rect, gap: Float = 3.4.dp.toPx(), width: Float = 1.3.dp.toPx(), seed: Int = 0) {
    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
        var x = rect.left - rect.height
        var i = 0
        while (x < rect.right) {
            val j = wobble(i * 13f, seed, 0.8.dp.toPx())
            drawLine(color, Offset(x + j, rect.bottom), Offset(x + rect.height + j, rect.top), width, StrokeCap.Round)
            x += gap; i++
        }
    }
}

/**
 * Outline that is a normal border in the clean style and a hand-drawn one in the sketch style.
 * [seed] keeps neighbouring elements from wobbling identically.
 */
fun Modifier.edge(color: Color, radius: Dp, width: Dp = 1.dp, seed: Int = 0): Modifier = composed {
    if (LocalSketch.current) {
        if (color.alpha == 0f) this
        else this.drawWithCache {
            val w = max(width.toPx(), 1.4.dp.toPx())
            val s = seed + (size.width * 7 + size.height * 13).toInt()
            onDrawWithContent { drawContent(); roughRect(color, radius.toPx(), s, w) }
        }
    } else this.border(width, color, RoundedCornerShape(radius))
}

/** Faint dotted-paper background for the sketch style. */
fun Modifier.paperDots(color: Color): Modifier = composed {
    if (!LocalSketch.current) this
    else this.drawBehind {
        val step = 16.dp.toPx(); val r = 0.9.dp.toPx()
        var y = step / 2
        while (y < size.height) { var x = step / 2; while (x < size.width) { drawCircle(color, r, Offset(x, y)); x += step }; y += step }
    }
}
