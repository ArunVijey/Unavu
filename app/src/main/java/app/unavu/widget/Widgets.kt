package app.unavu.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.unavu.MainActivity
import app.unavu.data.AppData
import app.unavu.data.Store
import app.unavu.data.addWater
import app.unavu.data.entriesOn
import app.unavu.data.liquidsMl
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BG = ColorProvider(Color(0xFF1F1D1A))
private val CHIP = ColorProvider(Color(0xFF2B2824))
private val INK = ColorProvider(Color(0xFFF2EEE6))
private val INK2 = ColorProvider(Color(0xFFA39E93))
private val LEAF = ColorProvider(Color(0xFF4FAE76))

private data class Snapshot(val left: Int, val target: Int, val eaten: Int, val protein: Int, val proteinT: Int, val liquidsL: Double, val combos: List<Pair<String, String>>)

private fun snapshot(d: AppData): Snapshot {
    val t = d.targets()
    val n = d.entriesOn(today()).total()
    val (w, dr) = d.liquidsMl(today())
    val combos = d.combos.sortedByDescending { it.uses }.take(2).map { it.id to "${it.emoji} ${it.name}" }
    return Snapshot(((t?.kcal ?: 0) - n.kcal).roundToInt(), t?.kcal ?: 0, n.kcal.roundToInt(), n.protein.roundToInt(), t?.protein ?: 0, (w + dr) / 1000.0, combos)
}

private val ACTION_KEY = ActionParameters.Key<String>(MainActivity.EXTRA_ACTION)

/** Glance passes action parameters to the activity as intent extras. */
@Suppress("UNUSED_PARAMETER")
private fun actionParam(context: Context, action: String) = actionParametersOf(ACTION_KEY to action)

private fun openIntent(context: Context, action: String) =
    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTION, action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.get(context).addWater(today(), 250)
        Widgets.refresh(context)
    }
}

@Composable
private fun Pill(text: String, modifier: GlanceModifier) {
    Box(modifier.background(CHIP).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(text, style = TextStyle(color = INK, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

class BigWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = snapshot(Store.get(context).data.value)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(BG).cornerRadius(24.dp).padding(14.dp).clickable(actionStartActivity<MainActivity>(actionParam(context, "today")))) {
                Text(if (s.left >= 0) "CAN STILL EAT" else "OVER BY", style = TextStyle(color = INK2, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${kotlin.math.abs(s.left)}", style = TextStyle(color = INK, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(8.dp))
                    Text("P ${s.protein}/${s.proteinT} · 💧 ${"%.1f".format(s.liquidsL)} L", style = TextStyle(color = INK2, fontSize = 11.sp))
                }
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = if (s.target > 0) (s.eaten / s.target.toFloat()).coerceIn(0f, 1f) else 0f,
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp), color = LEAF, backgroundColor = CHIP,
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Pill("＋ Log food", GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>(actionParam(context, "add"))))
                    Spacer(GlanceModifier.width(6.dp))
                    Pill("💧 +1 glass", GlanceModifier.defaultWeight().clickable(actionRunCallback<AddWaterAction>()))
                }
                if (s.combos.isNotEmpty()) {
                    Spacer(GlanceModifier.height(6.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        s.combos.forEachIndexed { i, (cid, label) ->
                            if (i > 0) Spacer(GlanceModifier.width(6.dp))
                            Pill(label, GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>(actionParam(context, "combo:$cid"))))
                        }
                    }
                }
            }
        }
    }
}

class SmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = snapshot(Store.get(context).data.value)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(BG).cornerRadius(22.dp).padding(12.dp).clickable(actionStartActivity<MainActivity>(actionParam(context, "add")))) {
                Text(if (s.left >= 0) "LEFT" else "OVER", style = TextStyle(color = INK2, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text("${kotlin.math.abs(s.left)}", style = TextStyle(color = INK, fontSize = 26.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = if (s.target > 0) (s.eaten / s.target.toFloat()).coerceIn(0f, 1f) else 0f,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp), color = LEAF, backgroundColor = CHIP,
                )
            }
        }
    }
}

class BigWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BigWidget() }
class SmallWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = SmallWidget() }

object Widgets {
    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val m = GlanceAppWidgetManager(context)
                if (m.getGlanceIds(BigWidget::class.java).isNotEmpty()) BigWidget().updateAll(context)
                if (m.getGlanceIds(SmallWidget::class.java).isNotEmpty()) SmallWidget().updateAll(context)
            }
        }
    }
}

// ---------------- Quick Settings tiles ----------------

class WaterTile : TileService() {
    override fun onStartListening() {
        val (w, _) = Store.get(this).data.value.liquidsMl(today())
        qsTile?.apply { state = Tile.STATE_ACTIVE; label = "+1 glass"; if (Build.VERSION.SDK_INT >= 29) subtitle = "${"%.1f".format(w / 1000.0)} L today"; updateTile() }
    }

    override fun onClick() {
        Store.get(this).addWater(today(), 250)
        Widgets.refresh(this)
        onStartListening()
    }
}

class QuickAddTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_ACTIVE; label = "Log food"; updateTile() }
    }

    override fun onClick() {
        val intent = openIntent(this, "add")
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}

