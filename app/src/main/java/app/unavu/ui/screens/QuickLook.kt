package app.unavu.ui.screens

import app.unavu.ui.EmojiText
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.unavu.ui.Divider
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
                w.setDimAmount(0.35f)
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
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(shown, enter = scaleIn(spring(dampingRatio = 0.62f, stiffness = 420f), initialScale = 0.86f) + fadeIn()) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 40.dp).verticalScroll(rememberScrollState()),
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
                                    Modifier.fillMaxWidth().clickable { a.onClick(); onDismiss() }.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(a.label, fontSize = 14.sp, color = if (a.destructive) c.chi else c.ink)
                                    EmojiText(a.icon, 15)
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
fun SourceRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = U.c.ink, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = valueColor)
        if (onRemove != null) Text("✕", fontSize = 12.sp, color = U.c.ink2, modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onRemove).padding(4.dp))
    }
}
