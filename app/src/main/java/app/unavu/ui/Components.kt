package app.unavu.ui

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
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = U.c.ink2,
    )
}

@Composable
fun TamilText(text: String, modifier: Modifier = Modifier, size: Float = 11f, color: Color = U.c.ink2) {
    Text(text, modifier = modifier, fontFamily = TamilFamily, fontSize = size.sp, color = color, fontWeight = FontWeight.Medium)
}

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) c.accS else c.sf)
            .border(1.5.dp, if (selected) c.acc else c.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) c.accInk else c.ink)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = U.c
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.sf2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.sf else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.ink else c.ink2)
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
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
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
fun ArrowButton(dir: Arrow, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(U.c.sf2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ArrowIcon(dir) }
}

@Composable
fun IconCircle(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(36.dp).clip(CircleShape).background(U.c.sf2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { EmojiText(text, 16) }
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
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) ArrowButton(Arrow.BACK, onClick = onBack)
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
        Status.OVER -> c.chiS to c.chi
        Status.INFO -> c.blueS to c.blue
    }
    Text(
        text, modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Track(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 7.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(U.c.sf2)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(height)).background(color))
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = U.c
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.acc, checkedThumbColor = c.onAcc,
            uncheckedTrackColor = c.sf2, uncheckedThumbColor = c.ink2, uncheckedBorderColor = c.line,
        ),
    )
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (leading != null) Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(U.c.sf2), contentAlignment = Alignment.Center) { EmojiText(leading, 17) }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.5.sp, color = U.c.ink2)
        }
        if (end != null) end() else if (trailing != null) Text(trailing, color = U.c.ink2)
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(U.c.line))

@Composable
fun Fab(modifier: Modifier = Modifier, size: Dp = 62.dp, onClick: () -> Unit) {
    val c = U.c
    Box(
        modifier
            .size(size)
            .shadow(14.dp, CircleShape, ambientColor = c.acc, spotColor = c.acc)
            .clip(CircleShape)
            .background(c.acc)
            .clickable(onClick = onClick),
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
