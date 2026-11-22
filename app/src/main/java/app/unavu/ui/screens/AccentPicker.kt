package app.unavu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.ui.AppCard
import app.unavu.ui.LocalData
import app.unavu.ui.nav
import app.unavu.ui.theme.DEFAULT_ACCENT
import app.unavu.ui.theme.U
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Accent swatch values are the one place raw colours live (they ARE the data).
private val PRESETS = listOf(
    R.string.i_accent_turmeric to DEFAULT_ACCENT,
    R.string.i_accent_leaf to 0xFF4FAE76.toInt(),
    R.string.i_accent_chilli to 0xFFE5484D.toInt(),
    R.string.i_accent_saffron to 0xFFFF8A1F.toInt(),
    R.string.i_accent_ocean to 0xFF3B82F6.toInt(),
    R.string.i_accent_teal to 0xFF14B8A6.toInt(),
    R.string.i_accent_grape to 0xFF8B5CF6.toInt(),
    R.string.i_accent_rose to 0xFFEC4899.toInt(),
)

/** Appearance › Accent colour: quick presets plus a colour wheel for anything else. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccentSection() {
    val c = U.c
    val store = nav.store
    val current = LocalData.current.settings.accent ?: DEFAULT_ACCENT
    var wheel by rememberSaveable { mutableStateOf(false) }
    fun set(argb: Int?) = store.update { d -> d.copy(settings = d.settings.copy(accent = argb?.takeIf { it != DEFAULT_ACCENT })) }

    AppCard {
        Text(stringResource(R.string.i_accent_desc), style = U.t.bodyS, color = c.ink2)
        Spacer(Modifier.height(10.dp))
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PRESETS.forEach { (nameRes, argb) -> Swatch(Color(argb), stringResource(nameRes), current == argb) { set(argb) } }
            val custom = PRESETS.none { it.second == current }
            Swatch(if (custom) Color(current) else null, stringResource(R.string.i_accent_custom), custom) { wheel = true }
        }
    }
    if (wheel) ColorWheelDialog(Color(current), onDismiss = { wheel = false }) { set(it.toArgb()); wheel = false }
}

/** One accent colour: a 48dp+ touch target named underneath. A null colour is the rainbow "custom" swatch. */
@Composable
private fun Swatch(color: Color?, name: String, on: Boolean, onClick: () -> Unit) {
    val c = U.c
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick).padding(vertical = 4.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .then(if (color == null) Modifier.background(Brush.sweepGradient(HUES)) else Modifier.background(color))
                .border(if (on) 3.dp else 0.dp, if (on) c.ink else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Box(Modifier.size(12.dp).clip(CircleShape).background(if ((color ?: Color.White).luminanceSafe() > 0.42f) Color.Black else Color.White))
        }
        Text(name, style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun Color.luminanceSafe() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private val HUES = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** HSV colour wheel: angle = hue, distance from centre = saturation, slider = brightness. */
@Composable
private fun ColorWheelDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val c = U.c
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by rememberSaveable { mutableFloatStateOf(hsv[0]) }
    var sat by rememberSaveable { mutableFloatStateOf(hsv[1]) }
    var value by rememberSaveable { mutableFloatStateOf(hsv[2].coerceAtLeast(0.35f)) }
    val picked = Color.hsv(hue, sat, value)
    val wheelDesc = stringResource(R.string.i_wheel_cd) + " " + stringResource(R.string.i_wheel_value, hue.roundToInt(), (sat * 100).roundToInt())

    fun pick(o: Offset, w: Float, h: Float) {
        val cx = w / 2; val cy = h / 2; val r = min(w, h) / 2
        val dx = o.x - cx; val dy = o.y - cy
        hue = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360).toFloat()
        sat = (hypot(dx, dy) / r).coerceIn(0f, 1f)
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(stringResource(R.string.i_pick_colour), style = U.t.title.copy(fontWeight = FontWeight.Bold), color = c.ink) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(0.85f).aspectRatio(1f)) {
                    Canvas(
                        Modifier.fillMaxWidth().aspectRatio(1f)
                            .semantics { contentDescription = wheelDesc }
                            .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) } }
                            .pointerInput(Unit) { detectDragGestures { ch, _ -> pick(ch.position, size.width.toFloat(), size.height.toFloat()) } },
                    ) {
                        val r = size.minDimension / 2
                        // hue around the circle (sweep starts at 3 o'clock, matching atan2)
                        drawCircle(Brush.sweepGradient(HUES, center), r)
                        // white in the middle -> saturation
                        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r), r)
                        // darken for brightness
                        drawCircle(Color.Black.copy(alpha = 1f - value), r)
                        // marker
                        val a = Math.toRadians(hue.toDouble())
                        val p = Offset(center.x + (cos(a) * sat * r).toFloat(), center.y + (sin(a) * sat * r).toFloat())
                        drawCircle(Color.White, 13.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                        drawCircle(picked, 10.dp.toPx(), p)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.i_brightness), style = U.t.caption, color = c.ink2, modifier = Modifier.fillMaxWidth())
                Slider(
                    value, { value = it }, valueRange = 0.35f..1f,
                    colors = SliderDefaults.colors(thumbColor = picked, activeTrackColor = picked, inactiveTrackColor = c.sf2),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(picked))
                    Text(
                        "  #%06X".format(picked.toArgb() and 0xFFFFFF), style = U.t.body.copy(fontWeight = FontWeight.Bold), color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.i_sample), style = U.t.label.copy(fontWeight = FontWeight.Bold),
                        color = if (picked.luminanceSafe() > 0.42f) Color.Black else Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(picked).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton({ onPick(picked) }) { Text(stringResource(R.string.i_use_colour), color = c.accInk, style = U.t.label.copy(fontWeight = FontWeight.Bold)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.i_cancel), color = c.ink2, style = U.t.label) } },
    )
}
