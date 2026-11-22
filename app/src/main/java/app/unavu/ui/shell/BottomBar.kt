package app.unavu.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.ui.Divider
import app.unavu.ui.NavIcon
import app.unavu.ui.Tab
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween

@Composable
private fun tabLabel(t: Tab): String = stringResource(
    when (t) {
        Tab.TODAY -> R.string.b_tab_today
        Tab.INSIGHTS -> R.string.b_tab_insights
        Tab.MEALS -> R.string.b_tab_meals
        Tab.SETTINGS -> R.string.b_tab_settings
    },
)

/** Bottom tab bar + the turmeric add button. Tabs are one selectable group (Role.Tab, >= 48dp, ripple). */
@Composable
fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, onAdd: () -> Unit, addOpen: Boolean = false) {
    val c = U.c
    val bigFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    val haptic = LocalHapticFeedback.current
    Divider()
    Row(
        Modifier.fillMaxWidth().background(if (U.sketch) c.bg else c.sf).navigationBarsPadding().padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.entries.forEach { t ->
                val on = t == tab
                val label = tabLabel(t)
                val col by animateColorAsState(if (on) c.ink else c.ink2, motionTween(), label = "tabColor")
                val ind by animateFloatAsState(if (on) 1f else 0f, motionTween(), label = "tabIndicator")
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = label }.selectable(selected = on, role = Role.Tab) {
                        if (!on) { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onTab(t) }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // M3-style pill behind the icon, growing from the centre
                    Box(Modifier.width(56.dp).height(32.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxSize().graphicsLayer { val s = 0.6f + 0.4f * ind; scaleX = s; scaleY = s; alpha = ind }
                                .clip(RoundedCornerShape(16.dp)).background(c.accS),
                        )
                        NavIcon(t.glyph, col, on, c.accS)
                    }
                    Spacer(Modifier.height(4.dp))
                    if (!bigFont) Text(
                        label, style = U.t.caption, fontWeight = FontWeight.SemiBold, color = col,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        BarFab(Modifier.padding(start = 6.dp), open = addOpen, contentDescription = stringResource(R.string.b_fab_add_food)) {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onAdd()
        }
    }
}

/** Turmeric add button: softer shadow, 0.92 press scale, the + turns into an x (45deg) while the sheet is open. */
@Composable
private fun BarFab(modifier: Modifier, open: Boolean, contentDescription: String, onClick: () -> Unit) {
    val c = U.c
    val reduced = LocalReducedMotion.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed && !reduced) 0.92f else 1f, motionTween(Motion.SHORT), label = "fabPress")
    val rot by animateFloatAsState(if (open) 45f else 0f, motionTween(Motion.MEDIUM, Motion.Emphasized), label = "fabRot")
    Box(
        modifier
            .size(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, CircleShape, ambientColor = c.acc, spotColor = c.acc)
            .clip(CircleShape)
            .background(c.acc)
            .then(if (focused) Modifier.border(2.dp, c.ink, CircleShape) else Modifier)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(bounded = true, color = c.onAcc), role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp).graphicsLayer { rotationZ = rot }) {
            val w = size.width; val sw = w * 0.13f
            drawLine(c.onAcc, Offset(w / 2, 0f), Offset(w / 2, w), sw, StrokeCap.Round)
            drawLine(c.onAcc, Offset(0f, w / 2), Offset(w, w / 2), sw, StrokeCap.Round)
        }
    }
}
