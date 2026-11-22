package app.unavu.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Named type roles. Use `U.t.body` etc. instead of literal font sizes.
 * Owner: Agent C (theme). This is the round-1 skeleton; C refines sizes, line heights and the Excalifont adjustments.
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

/** Builds the roles on top of the current base style (app font, tnum, colour). */
fun appType(base: TextStyle): AppType = AppType(
    display = base.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold),
    headline = base.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    title = base.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleS = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    body = base.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodyS = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    label = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    caption = base.copy(fontSize = 12.sp, lineHeight = 16.sp),
)

internal val currentType: AppType @Composable get() = appType(LocalTextStyle.current)
