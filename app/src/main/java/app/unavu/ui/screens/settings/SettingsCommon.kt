package app.unavu.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.ui.EmojiText
import app.unavu.ui.pressScale
import app.unavu.ui.theme.U

internal fun TextStyle.iBold() = copy(fontWeight = FontWeight.Bold)

/** Leading emoji tile shared by the setting rows. Decorative, so screen readers skip it. */
@Composable
private fun RowLead(emoji: String) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(U.c.sf2).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) { EmojiText(emoji, 17) }
}

/** A whole row toggles the switch; announced as one switch with its title and subtitle. */
@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    emoji: String? = null,
    onChange: (Boolean) -> Unit,
) {
    val c = U.c
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.pressScale().fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch) { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onChange(it) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (emoji != null) RowLead(emoji)
        Column(Modifier.weight(1f)) {
            Text(title, style = U.t.titleS.iBold(), color = c.ink)
            if (subtitle != null) Text(subtitle, style = U.t.bodyS, color = c.ink2)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.acc, checkedThumbColor = c.onAcc,
                uncheckedTrackColor = c.sf2, uncheckedThumbColor = c.ink2, uncheckedBorderColor = c.line,
            ),
        )
    }
}

/** One option of a radio group (wrap the rows in `Modifier.selectableGroup()`). */
@Composable
fun SettingRadioRow(title: String, subtitle: String?, selected: Boolean, onSelect: () -> Unit) {
    val c = U.c
    Row(
        Modifier.pressScale().fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = U.t.titleS.iBold(), color = c.ink)
            if (subtitle != null) Text(subtitle, style = U.t.bodyS, color = c.ink2)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) c.acc else c.ink2, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(c.acc)) }
    }
}

/** Finds the hosting Activity from a Compose context (for permission rationale checks). */
internal fun Context.findActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal fun notificationsAllowed(context: Context): Boolean =
    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
