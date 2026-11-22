package app.unavu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.unavu.R
import app.unavu.data.Backup
import app.unavu.ui.AppCard
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.EmojiText
import app.unavu.ui.ListRow
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.TimeDialog
import app.unavu.ui.TopBar
import app.unavu.ui.fmtTime
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import app.unavu.work.Scheduler
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val context = LocalContext.current
    val res = androidx.compose.ui.platform.LocalResources.current
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val b = data.backup
    var busy by remember { mutableStateOf(false) }
    var timeDialog by rememberSaveable { mutableStateOf(false) }
    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }
    val undo = stringResource(R.string.i_undo)
    val unknown = stringResource(R.string.i_backup_error_unknown)
    val backedUpFmt = stringResource(R.string.i_backed_up, "%s")
    val failedFmt = stringResource(R.string.i_backup_error, "%s")

    fun runBackup() {
        if (busy) return
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { Backup.run(context) }
            busy = false
            r.fold(
                { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); feedback?.show(backedUpFmt.format(it)) },
                { feedback?.show(failedFmt.format(it.message ?: unknown)) },
            )
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val name = DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment
            store.update { d -> d.copy(backup = d.backup.copy(folderUri = uri.toString(), folderName = name)) }
            Scheduler.scheduleBackup(context)
            runBackup()
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreUri = uri.toString() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_backup), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val ok = b.lastOk && b.lastAt != null
            AppCard(color = if (b.lastAt == null) c.sf2 else if (ok) c.leafS else c.chiS, border = null) {
                Text(
                    when {
                        b.folderUri == null -> stringResource(R.string.i_pick_folder_start)
                        b.lastAt == null -> stringResource(R.string.i_not_backed_up)
                        ok -> stringResource(R.string.i_last_backup, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(b.lastAt)))
                        else -> stringResource(R.string.i_last_backup_failed)
                    },
                    style = U.t.body.iBold(), color = if (b.lastAt == null) c.ink else if (ok) c.leafInk else c.chiInk,
                )
                b.lastMessage?.let { Text(it, style = U.t.caption, color = c.ink2) }
            }
            PrimaryButton(stringResource(if (busy) R.string.i_backing_up else R.string.i_backup_now), enabled = b.folderUri != null && !busy) { runBackup() }
            if (b.folderUri == null) Text(stringResource(R.string.i_backup_needs_folder), style = U.t.caption, color = c.ink2)

            SectionLabel(stringResource(R.string.i_folder_section))
            AppCard(onClick = { pickFolder.launch(null) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmojiText("📁", 30)
                    Column(Modifier.weight(1f)) {
                        Text(b.folderName ?: stringResource(R.string.i_no_folder), style = U.t.body.iBold(), color = c.ink)
                        Text(stringResource(R.string.i_overwritten), style = U.t.caption, color = c.ink2)
                    }
                    Text(stringResource(if (b.folderUri == null) R.string.i_choose else R.string.i_change), color = c.accInk, style = U.t.label.iBold())
                }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                listOf(
                    "🗂" to (Backup.JSON_NAME to R.string.i_file_json),
                    "🛟" to (Backup.PREVIOUS_NAME to R.string.i_file_prev),
                    "📄" to (Backup.LOG_CSV to R.string.i_file_log),
                    "📊" to (Backup.SUMMARY_CSV to R.string.i_file_summary),
                ).forEachIndexed { i, (icon, f) ->
                    if (i > 0) Divider()
                    ListRow(f.first, stringResource(f.second), icon, trailing = null)
                }
            }
            SectionLabel(stringResource(R.string.i_daily_backup))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                SettingSwitchRow(stringResource(R.string.i_backup_every_day), stringResource(R.string.i_backup_every_day_sub), b.daily) { v ->
                    store.update { d -> d.copy(backup = d.backup.copy(daily = v)) }
                    Scheduler.scheduleBackup(context)
                }
                Divider()
                ListRow(stringResource(R.string.i_time), null, trailing = fmtTime(b.hour, b.minute)) { timeDialog = true }
            }
            SectionLabel(stringResource(R.string.i_restore))
            SecondaryButton(stringResource(R.string.i_restore_button)) { pickRestore.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
            Text(stringResource(R.string.i_restore_note), style = U.t.caption, color = c.ink2)
            Spacer(Modifier.height(24.dp))
        }
    }
    if (timeDialog) {
        TimeDialog(b.hour, b.minute, onDismiss = { timeDialog = false }) { h, m ->
            store.update { d -> d.copy(backup = d.backup.copy(hour = h, minute = m)) }
            Scheduler.scheduleBackup(context)
        }
    }
    restoreUri?.let { uriStr ->
        ConfirmDialog(
            stringResource(R.string.i_restore_title), stringResource(R.string.i_restore_text), stringResource(R.string.i_restore_confirm), true,
            { restoreUri = null },
        ) {
            val before = store.data.value
            val uri: Uri = uriStr.toUri()
            scope.launch {
                val r = withContext(Dispatchers.IO) { Backup.restore(context, uri) }
                r.fold(
                    { n ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        feedback?.show(res.getString(R.string.i_restored, n), undo) { store.restoreSnapshot(before) }
                    },
                    { feedback?.show(res.getString(R.string.i_restore_failed, it.message ?: unknown)) },
                )
            }
        }
    }
}
