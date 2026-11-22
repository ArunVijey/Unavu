package app.unavu.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.ui.theme.motionTween

/**
 * The one selected style for chips, pills and segments: a soft accent fill with a dark-ochre
 * ([accInk]) outline and text. accInk is tuned to pass 4.5:1 on bg, sf and accS, so the outline
 * clears the 3:1 non-text minimum that the bright accent alone does not.
 */
data class SelectionColors(val fill: Color, val stroke: Color, val text: Color)

@Composable
fun selectionColors(selected: Boolean): SelectionColors {
    val c = U.c
    return if (selected) SelectionColors(c.accS, c.accInk, c.accInk) else SelectionColors(c.sf, c.line, c.ink)
}

/** Shared chip surface (fill + 1.5dp outline, pill shape) in the selected / unselected style, animated. */
@Composable
fun Modifier.chipSurface(selected: Boolean, seed: Int = 0): Modifier {
    val s = selectionColors(selected)
    val stroke by animateColorAsState(s.stroke, motionTween(Motion.SHORT), label = "chipStroke")
    val fill by animateColorAsState(s.fill, motionTween(Motion.SHORT), label = "chipFill")
    return this.edge(stroke, 50.dp, 1.5.dp, seed = seed).clip(RoundedCornerShape(50)).background(fill)
}

/**
 * The one chip: 48dp touch target, pill surface from [chipSurface]. [role] is RadioButton for
 * single choice and Checkbox for toggles.
 */
@Composable
fun SelectChip(text: String, selected: Boolean, modifier: Modifier = Modifier, role: Role = Role.RadioButton, seed: Int = text.length, onClick: () -> Unit) {
    val ink by animateColorAsState(selectionColors(selected).text, motionTween(Motion.SHORT), label = "chipInk")
    Box(
        modifier.heightIn(min = 48.dp).pressScale().selectable(selected = selected, role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = U.t.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, color = ink,
            modifier = Modifier.chipSurface(selected, seed).padding(horizontal = 13.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) =
    SelectChip(text, selected, modifier, onClick = onClick)

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) =
    SlidingSegmented(options, selected, modifier, onSelect)

/**
 * The one segmented control. The selected thumb slides between options and uses the same
 * selected style as [SelectChip] (accent fill, dark-ochre outline and text).
 */
@Composable
fun SlidingSegmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = U.c
    val n = options.size.coerceAtLeast(1)
    val gap = 3.dp
    val pos by animateFloatAsState(selected.coerceIn(0, n - 1).toFloat(), motionTween(Motion.MEDIUM), label = "segThumb")
    val on = selectionColors(true)
    BoxWithConstraints(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.sf2).padding(3.dp)) {
        val slot = (maxWidth - gap * (n - 1)) / n
        Box(
            Modifier
                .offset { IntOffset(((slot + gap).toPx() * pos).toInt(), 0) }
                .size(slot, 48.dp)
                .edge(on.stroke, 9.dp, 1.5.dp)
                .clip(RoundedCornerShape(9.dp)).background(on.fill),
        )
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val tc by animateColorAsState(if (sel) on.text else c.ink2, motionTween(Motion.SHORT), label = "segText")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .selectable(selected = sel, role = Role.Tab) { onSelect(i) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = U.t.label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = tc, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
