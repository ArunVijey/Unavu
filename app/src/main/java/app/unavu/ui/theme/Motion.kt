package app.unavu.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens. Owner: Agent C (theme). Round-1 skeleton: C wires LocalReducedMotion to the system
 * animator duration scale inside AppTheme.
 */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 250
    const val LONG = 300
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** True when the user turned animations off (Settings › Accessibility › Remove animations). */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/** A tween that becomes an instant snap when reduced motion is on. */
@Composable
fun <T> motionTween(durationMs: Int = Motion.MEDIUM, easing: Easing = Motion.EmphasizedDecelerate): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else tween(durationMs, easing = easing)
