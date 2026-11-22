package app.unavu.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-wide snackbar messages ("Added to Breakfast", "Deleted · UNDO").
 * The SnackbarHost lives in Shell.kt; LocalFeedback is provided by MainActivity.
 */
@Stable
class Feedback(val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(message, actionLabel, withDismissAction = false, duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalFeedback: ProvidableCompositionLocal<Feedback?> = staticCompositionLocalOf { null }
