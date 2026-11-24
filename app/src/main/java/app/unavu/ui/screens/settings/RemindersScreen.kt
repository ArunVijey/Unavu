package app.unavu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import app.unavu.ui.EmojiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.unavu.R
import app.unavu.data.Meal
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.LocalData
import app.unavu.ui.PrimaryButton
import app.unavu.ui.TimeDialog
import app.unavu.ui.TopBar
import app.unavu.ui.fmtTime
import app.unavu.ui.nav
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import app.unavu.work.Scheduler

@Composable
fun RemindersScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    // true once the system dialog has refused us; with no rationale left that means "don't ask again"
    var asked by rememberSaveable { mutableStateOf(false) }
    var blockedForGood by rememberSaveable { mutableStateOf(false) }

    // The user may flip the switch in system settings, so look again whenever we come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = notificationsAllowed(context) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted && notificationsAllowed(context)
        if (!granted) {
            val activity = context.findActivity()
            blockedForGood = activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun canAsk() = Build.VERSION.SDK_INT >= 33 && !blockedForGood &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    fun ensurePermission() {
        if (allowed) return
        if (canAsk()) { asked = true; askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) } else openNotificationSettings(context)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_reminders_title), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            if (!allowed) {
                AppCard(color = c.chiS, border = null) {
                    Text(stringResource(R.string.i_notif_off_title), style = U.t.titleS.iBold(), color = c.chiInk, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.i_notif_off_text), style = U.t.bodyS, color = c.ink, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                    PrimaryButton(stringResource(if (canAsk()) R.string.i_notif_allow else R.string.i_notif_open_settings)) { ensurePermission() }
                }
            }
            AppCard(color = c.sf2, border = null) {
                Text(stringResource(R.string.i_reminders_hint), style = U.t.bodyS, color = c.ink2)
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                data.reminders.forEachIndexed { i, r ->
                    if (i > 0) Divider()
                    val icon = when (r.id) { "water" -> "💧"; "weigh" -> "⚖️"; else -> Meal.entries.firstOrNull { it.name.equals(r.id, true) }?.emoji ?: "🔔" }
                    val time = (if (r.id == "water") stringResource(R.string.i_reminder_from, fmtTime(r.hour, r.minute)) else fmtTime(r.hour, r.minute))
                    ReminderRow(r.label, stringResource(R.string.i_reminder_sub, time), icon, r.on, onToggle = { v ->
                        if (v) ensurePermission()
                        store.update { d -> d.copy(reminders = d.reminders.map { if (it.id == r.id) it.copy(on = v) else it }) }
                        Scheduler.scheduleReminders(context)
                    }, onEditTime = { editing = r.id })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    editing?.let { id ->
        val r = data.reminders.firstOrNull { it.id == id }
        if (r == null) editing = null else TimeDialog(r.hour, r.minute, onDismiss = { editing = null }) { h, m ->
            store.update { d -> d.copy(reminders = d.reminders.map { if (it.id == id) it.copy(hour = h, minute = m) else it }) }
            Scheduler.scheduleReminders(context)
        }
    }
}

/** Left part opens the time picker; the switch on the right has its own label, so each is a clear 48dp+ target. */
@Composable
private fun ReminderRow(title: String, sub: String, emoji: String, on: Boolean, onToggle: (Boolean) -> Unit, onEditTime: () -> Unit) {
    val c = U.c
    val cd = stringResource(R.string.i_reminder_toggle_cd, title)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onEditTime)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.sf2).clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) { EmojiText(emoji, 17) }
            Column(Modifier.weight(1f)) {
                Text(title, style = U.t.titleS.iBold(), color = c.ink)
                Text(sub, style = U.t.bodyS, color = c.ink2)
            }
        }
        Switch(
            checked = on, onCheckedChange = onToggle, modifier = Modifier.semantics { contentDescription = cd },
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.acc, checkedThumbColor = c.onAcc,
                uncheckedTrackColor = c.sf2, uncheckedThumbColor = c.ink2, uncheckedBorderColor = c.line,
            ),
        )
    }
}
