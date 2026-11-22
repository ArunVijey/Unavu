package app.unavu.ui.theme

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.isSpecified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Named type roles. Use `U.t.body` etc. instead of literal font sizes.
 * Sizes and line heights are tuned per role; Excalifont adjustments live in AppTheme.
 */
@Immutable
data class AppType(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleS: TextStyle,
    val body: TextStyle,
    val bodyS: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

/** Builds the roles on top of the base style (app font, tnum, colour). Excalifont runs small, so each role gets +1sp. */
fun appType(base: TextStyle, excalifont: Boolean = false): AppType {
    val d = if (excalifont) 1 else 0
    fun role(size: Int, line: Int, w: FontWeight? = null) =
        base.copy(fontSize = (size + d).sp, lineHeight = (line + d * 2).sp, fontWeight = w ?: base.fontWeight)
    return AppType(
        display = role(34, 40, FontWeight.ExtraBold),
        headline = role(26, 32, FontWeight.Bold),
        title = role(18, 24, FontWeight.Bold),
        titleS = role(16, 22, FontWeight.Bold),
        body = role(15, 21),
        bodyS = role(14, 20),
        label = role(13, 18, FontWeight.SemiBold),
        caption = role(12, 16),
    )
}

/** Provided by AppTheme. */
val LocalAppType = staticCompositionLocalOf { appType(TextStyle.Default) }

internal val currentType: AppType @Composable @ReadOnlyComposable get() = LocalAppType.current

/** Tamil glyphs are taller than Latin ones: give lines of Tamil text 25% more leading so they aren't clipped. */
fun TextStyle.forTamil(): TextStyle =
    if (lineHeight.isSpecified) copy(lineHeight = lineHeight * 1.25f) else copy(lineHeight = (fontSize.value * 1.6f).sp)
