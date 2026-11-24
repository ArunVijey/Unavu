package app.unavu.ui.screens

import app.unavu.ui.theme.motionTween
import app.unavu.ui.theme.motionSpring
import app.unavu.ui.EmojiText
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import app.unavu.ui.theme.Motion
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.unavu.R
import app.unavu.ui.pressScale
import app.unavu.ui.theme.LocalReducedMotion
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.unavu.ui.Divider
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U

data class MenuAction(val label: String, val icon: String, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * The quick-look popup: the screen behind blurs (Android 12+), the card springs in with a small
 * vibration, and an action menu sits underneath it. Tap outside to close.
 */
@Composable
fun QuickLook(onDismiss: () -> Unit, actions: List<MenuAction> = emptyList(), content: @Composable ColumnScope.() -> Unit) {
    val haptics = LocalHapticFeedback.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let { w ->
                w.setDimAmount(0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    w.attributes = w.attributes.apply { blurBehindRadius = 48 }
                }
            }
        }
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); shown = true }
        val c = U.c
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = stringResource(R.string.d_close), onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            val reduced = LocalReducedMotion.current
            val scrim by animateFloatAsState(if (shown) 0.35f else 0f, motionTween(Motion.MEDIUM, Motion.Standard), label = "scrim")
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
            AnimatedVisibility(shown, enter = if (reduced) fadeIn(snap()) else scaleIn(motionSpring(0.8f, Spring.StiffnessMedium), initialScale = 0.86f, transformOrigin = TransformOrigin(0.5f, 0.3f)) + fadeIn(tween(Motion.SHORT))) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 40.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .shadow(18.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.sf)
                            .border(1.dp, c.line, RoundedCornerShape(24.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        content = content,
                    )
                    if (actions.isNotEmpty()) {
                        Column(
                            Modifier
                                .width(240.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.sf)
                                .border(1.dp, c.line, RoundedCornerShape(16.dp)),
                        ) {
                            actions.forEachIndexed { i, a ->
                                if (i > 0) Divider()
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { a.onClick(); onDismiss() }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(a.label, style = U.t.bodyS, color = if (a.destructive) c.chiInk else c.ink, modifier = Modifier.weight(1f))
                                    Box(Modifier.clearAndSetSemantics {}) { EmojiText(a.icon, 15) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceRow(label: String, value: String, valueColor: Color = U.c.ink, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = U.t.bodyS, modifier = Modifier.weight(1f))
        Text(value, style = U.t.bodyS, fontWeight = FontWeight.Bold, color = valueColor)
        if (onRemove != null) RemoveButton(onRemove)
    }
}

@Composable
fun RemoveButton(onClick: () -> Unit) {
    val desc = stringResource(R.string.d_remove)
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { Text("✕", style = U.t.label, color = U.c.ink2) }
}
