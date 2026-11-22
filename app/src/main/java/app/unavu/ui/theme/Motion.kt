package app.unavu.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens (durations + easings). SHORT: taps/press; MEDIUM: state changes; LONG: page and sheet transitions.
 * LocalReducedMotion mirrors the system animator duration scale (set in AppTheme).
 */
object Motion {
    /** Fade-out half of a fade-through, tiny confirmations. */
    const val XSHORT = 90
    /** Fade-in half of a fade-through (starts after XSHORT). */
    const val FADE_THROUGH = 210
    const val SHORT = 150
    const val MEDIUM = 250
    const val LONG = 300
    val Standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** True when the user turned animations off (Settings › Accessibility › Remove animations). */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/** A tween that becomes an instant snap when reduced motion is on. */
@Composable
fun <T> motionTween(durationMs: Int = Motion.MEDIUM, easing: Easing = Motion.EmphasizedDecelerate, delayMs: Int = 0): FiniteAnimationSpec<T> =
    motionTween(LocalReducedMotion.current, durationMs, easing, delayMs)

/** Non-composable variant for transitionSpec lambdas and coroutines: pass the reduced-motion flag read in composition. */
fun <T> motionTween(reduced: Boolean, durationMs: Int, easing: Easing = Motion.EmphasizedDecelerate, delayMs: Int = 0): FiniteAnimationSpec<T> =
    if (reduced) snap() else tween(durationMs, delayMillis = delayMs, easing = easing)

/** A spring that becomes an instant snap when reduced motion is on. */
@Composable
fun <T> motionSpring(dampingRatio: Float = 0.8f, stiffness: Float = Spring.StiffnessMediumLow): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else spring(dampingRatio, stiffness)

/** Reads Settings.Global.ANIMATOR_DURATION_SCALE and follows changes (scale 0 = reduced motion). */
@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    var reduced by remember { mutableStateOf(readReduced(ctx)) }
    DisposableEffect(ctx) {
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = readReduced(ctx) }
        }
        ctx.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, obs)
        onDispose { ctx.contentResolver.unregisterContentObserver(obs) }
    }
    return reduced
}

private fun readReduced(ctx: Context): Boolean =
    try { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { false }
