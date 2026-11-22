package app.unavu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.ui.theme.U

/** Drawn right-pointing chevron for list rows. */
@Composable
fun ChevronIcon(color: Color = U.c.ink2, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val p = Path().apply { moveTo(w * 0.36f, h * 0.2f); lineTo(w * 0.66f, h * 0.5f); lineTo(w * 0.36f, h * 0.8f) }
        drawPath(p, color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

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

/**
 * An emoji with room to breathe: generous line height and font padding so it is never clipped or
 * squashed by the app font's tighter metrics.
 */
@Composable
fun EmojiText(e: String, size: Int = 20, modifier: Modifier = Modifier) {
    Text(
        e, modifier = modifier, fontSize = size.sp, lineHeight = (size * 1.35f).sp, textAlign = TextAlign.Center,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = true),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        ),
    )
}

enum class NavGlyph { TODAY, INSIGHTS, MEALS, SETTINGS }

/** Simple line icons for the bottom bar (drawn, so they tint cleanly and stay crisp). */
@Composable
fun NavIcon(g: NavGlyph, color: Color, active: Boolean, bg: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cr = CornerRadius(w * 0.2f)
        fun o(x: Float, y: Float) = Offset(w * x, h * y)
        fun sz(x: Float, y: Float) = Size(w * x, h * y)
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
                    if (active) drawRoundRect(color, o(x, top), sz(bw, 0.88f - top), CornerRadius(w * 0.06f))
                    else drawRoundRect(color, o(x, top), sz(bw, 0.88f - top), CornerRadius(w * 0.06f), style = stroke)
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

enum class LineGlyph { CALENDAR, MORE }
