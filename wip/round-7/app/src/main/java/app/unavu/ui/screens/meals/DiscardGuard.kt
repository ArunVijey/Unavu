package app.unavu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.ui.theme.U

/**
 * Keeps an editor from silently throwing work away. While [dirty], system back (gesture or button) asks
 * "Discard …?" instead of leaving; call the returned function from the TopBar back arrow so both paths behave the
 * same. Leaving a clean editor calls [onLeave] straight away.
 */
@Composable
internal fun rememberDiscardGuard(dirty: Boolean, title: String, onLeave: () -> Unit): () -> Unit {
    var asking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = dirty) { asking = true }
    if (asking) {
        DiscardDialog(title, onKeep = { asking = false }, onDiscard = { asking = false; onLeave() })
    }
    return {
        if (!dirty) onLeave()
        asking = dirty
    }
}

/** "Discard …?" with Keep editing / Discard, in the app's dialog style (see ConfirmDialog). */
@Composable
private fun DiscardDialog(title: String, onKeep: () -> Unit, onDiscard: () -> Unit) {
    val c = U.c
    AlertDialog(
        onDismissRequest = onKeep, containerColor = c.sf,
        title = { Text(title, style = U.t.title) },
        text = { Text(stringResource(R.string.r7d_discard_body), style = U.t.body, color = c.ink2) },
        confirmButton = {
            TextButton(onDiscard, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(stringResource(R.string.r7d_discard), color = c.chiInk, style = U.t.label, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onKeep, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(stringResource(R.string.r7d_keep_editing), color = c.ink2, style = U.t.label)
            }
        },
    )
}
