package app.unavu.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.unavu.data.AppData
import app.unavu.ui.Feedback
import app.unavu.ui.LocalFeedback

/** Shows a snackbar with Undo (puts back the earlier snapshot) plus a haptic tick. */
internal class Undoer(
    private val feedback: Feedback?,
    private val haptic: HapticFeedback,
    private val undoLabel: String,
    private val restore: (AppData) -> Unit,
) {
    fun announce(message: String, before: AppData, reject: Boolean = false) {
        haptic.performHapticFeedback(if (reject) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
        feedback?.show(message, undoLabel) { restore(before) }
    }

    fun say(message: String) { feedback?.show(message) }
}

@Composable
internal fun rememberUndoer(undoLabel: String, restore: (AppData) -> Unit): Undoer {
    val fb = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    return Undoer(fb, haptic, undoLabel, restore)
}
