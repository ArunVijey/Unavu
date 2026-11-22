package app.unavu.ui.shell

import androidx.compose.animation.core.animateFloatAsState
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
import app.unavu.ui.Fab
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
fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, onAdd: () -> Unit) {
    val c = U.c
    val haptic = LocalHapticFeedback.current
    Divider()
    Row(
        Modifier.fillMaxWidth().background(if (U.sketch) c.bg else c.sf).navigationBarsPadding().padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.entries.forEach { t ->
                val on = t == tab
                val col = if (on) c.accInk else c.ink2
                val ind by animateFloatAsState(if (on) 1f else 0f, motionTween(), label = "tabIndicator")
                Column(
                    Modifier.weight(1f).heightIn(min = 72.dp).selectable(selected = on, role = Role.Tab) {
                        if (!on) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onTab(t) }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // indicator line on top of the active tab
                    Box(
                        Modifier.width(30.dp).height(3.dp).graphicsLayer { scaleX = ind; alpha = ind }
                            .clip(RoundedCornerShape(2.dp)).background(c.acc),
                    )
                    Spacer(Modifier.height(10.dp))
                    NavIcon(t.glyph, col, on, c.sf)
                    Spacer(Modifier.height(4.dp))
                    val label = tabLabel(t)
                    Text(
                        if (U.sketch) label.lowercase() else label.uppercase(),
                        style = U.t.caption, color = col, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        Fab(Modifier.padding(start = 6.dp), size = 58.dp, contentDescription = stringResource(R.string.b_fab_add_food), onClick = onAdd)
    }
}
