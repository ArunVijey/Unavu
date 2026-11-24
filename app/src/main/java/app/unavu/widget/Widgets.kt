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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import app.unavu.R
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as dayNight
import app.unavu.MainActivity
import app.unavu.data.AppData
import app.unavu.data.Settings
import app.unavu.data.Store
import app.unavu.data.ThemeMode
import app.unavu.data.addWater
import app.unavu.data.entriesOn
import app.unavu.data.fmtInt
import app.unavu.data.liquidsMl
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.ui.theme.AmoledColors
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.DEFAULT_ACCENT
import app.unavu.ui.theme.DarkColors
import app.unavu.ui.theme.LightColors
import app.unavu.ui.theme.withAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Widget colours come from the app's own palette, theme and accent, so the widget looks like the app:
 * Light / Dark (or Pure black) follow Settings > Appearance, and "System" switches with the phone's day/night mode.
 * Every pair passes WCAG AA: ink and ink2 are at least 4.5:1 on the card and chip colours, the progress fill is the
 * accent ink (at least 4.5:1 on its track, see withAccent) and the primary pill is the accent with its on-accent text.
 */
private class Palette(
    val card: ColorProvider, val chip: ColorProvider, val ink: ColorProvider, val ink2: ColorProvider,
    val bar: ColorProvider, val over: ColorProvider, val acc: ColorProvider, val onAcc: ColorProvider,
)

private fun palette(s: Settings): Palette {
    val accent = Color(s.accent ?: DEFAULT_ACCENT)
    val day = LightColors.withAccent(accent)
    val night = (if (s.amoled) AmoledColors else DarkColors).withAccent(accent)
    fun pick(token: (AppColors) -> Color): ColorProvider = when (s.theme) {
        ThemeMode.LIGHT -> ColorProvider(token(day))
        ThemeMode.DARK -> ColorProvider(token(night))
        ThemeMode.SYSTEM -> dayNight(day = token(day), night = token(night))
    }
    return Palette(
        card = pick { it.sf }, chip = pick { it.sf2 }, ink = pick { it.ink }, ink2 = pick { it.ink2 },
        bar = pick { it.accInk }, over = pick { it.chiInk }, acc = pick { it.acc }, onAcc = pick { it.onAcc },
    )
}

private data class Snapshot(
    val hasProfile: Boolean, val left: Int, val target: Int, val eaten: Int, val protein: Int, val proteinT: Int,
    val liquidsL: Double, val combos: List<Pair<String, String>>, val settings: Settings,
) {
    val over: Boolean get() = left < 0
    val progress: Float get() = if (target > 0) (eaten / target.toFloat()).coerceIn(0f, 1f) else 0f
}

private fun snapshot(d: AppData): Snapshot {
    val t = d.targets()
    val n = d.entriesOn(today()).total()
    val (w, dr) = d.liquidsMl(today())
    val combos = d.combos.sortedByDescending { it.uses }.take(2).map { it.id to "${it.emoji} ${it.name}" }
    return Snapshot(
        t != null, ((t?.kcal ?: 0) - n.kcal).roundToInt(), t?.kcal ?: 0, n.kcal.roundToInt(), n.protein.roundToInt(), t?.protein ?: 0,
        (w + dr) / 1000.0, combos, d.settings,
    )
}

/** Never throws: an unreadable store shows the "no profile yet" state instead of a broken widget. */
private fun safeSnapshot(context: Context): Snapshot =
    runCatching { snapshot(Store.get(context).data.value) }.getOrElse { snapshot(AppData()) }

private val ACTION_KEY = ActionParameters.Key<String>(MainActivity.EXTRA_ACTION)

/** Glance passes action parameters to the activity as intent extras. */
@Suppress("UNUSED_PARAMETER")
private fun actionParam(context: Context, action: String) = actionParametersOf(ACTION_KEY to action)

private fun openIntent(context: Context, action: String) =
    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTION, action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { Store.get(context).addWater(today(), 250) }
        Widgets.refresh(context)
    }
}

/** At least 48dp tall so it is an easy tap target; described for TalkBack. [primary] fills it with the accent. */
@Composable
private fun Pill(p: Palette, text: String, description: String, modifier: GlanceModifier, primary: Boolean = false) {
    Box(
        modifier.height(48.dp).background(if (primary) p.acc else p.chip).cornerRadius(12.dp).padding(horizontal = 10.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(color = if (primary) p.onAcc else p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

/** Eaten against the target: accent fill, red once the day is over the target. */
@Composable
private fun Progress(p: Palette, s: Snapshot, heightDp: Int) {
    LinearProgressIndicator(
        progress = s.progress,
        modifier = GlanceModifier.fillMaxWidth().height(heightDp.dp), color = if (s.over) p.over else p.bar, backgroundColor = p.chip,
    )
}

private fun litres(s: Snapshot): String = "%.1f".format(s.liquidsL)

private fun summary(context: Context, s: Snapshot): String =
    if (!s.hasProfile) context.getString(R.string.a_widget_no_profile_desc)
    else context.getString(
        if (s.over) R.string.a_widget_summary_over else R.string.a_widget_summary,
        abs(s.left), s.protein, s.proteinT, litres(s),
    )

class BigWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = safeSnapshot(context)
        val p = palette(s.settings)
        provideContent {
            val root = GlanceModifier.fillMaxSize().background(p.card).cornerRadius(24.dp).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>(actionParam(context, "today")))
            if (!s.hasProfile) {
                Column(root.semantics { contentDescription = summary(context, s) }) {
                    Text(context.getString(R.string.a_widget_no_profile), style = TextStyle(color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            } else {
                Column(root.semantics { contentDescription = summary(context, s) }) {
                    Text(
                        context.getString(if (s.over) R.string.a_widget_over_by else R.string.a_widget_still_eat),
                        style = TextStyle(color = p.ink2, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(fmtInt(abs(s.left)), style = TextStyle(color = if (s.over) p.over else p.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            context.getString(R.string.r7e_widget_detail, fmtInt(s.protein), fmtInt(s.proteinT), litres(s)),
                            style = TextStyle(color = p.ink2, fontSize = 12.sp), maxLines = 1,
                        )
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Progress(p, s, 8)
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Pill(
                            p, context.getString(R.string.a_widget_log_food), context.getString(R.string.a_widget_log_food_desc),
                            GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>(actionParam(context, "add"))), primary = true,
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Pill(
                            p, context.getString(R.string.a_widget_water), context.getString(R.string.a_widget_water_desc),
                            GlanceModifier.defaultWeight().clickable(actionRunCallback<AddWaterAction>()),
                        )
                    }
                    if (s.combos.isNotEmpty()) {
                        Spacer(GlanceModifier.height(6.dp))
                        Row(GlanceModifier.fillMaxWidth()) {
                            s.combos.forEachIndexed { i, (cid, label) ->
                                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                                Pill(
                                    p, label, context.getString(R.string.a_widget_combo_desc, label),
                                    GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>(actionParam(context, "combo:$cid"))),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class SmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = safeSnapshot(context)
        val p = palette(s.settings)
        provideContent {
            val root = GlanceModifier.fillMaxSize().background(p.card).cornerRadius(22.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>(actionParam(context, if (s.hasProfile) "add" else "today")))
                .semantics { contentDescription = summary(context, s) }
            if (!s.hasProfile) {
                Column(root) { Text(context.getString(R.string.a_widget_no_profile), style = TextStyle(color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)) }
            } else {
                Column(root) {
                    Text(
                        context.getString(if (s.over) R.string.a_widget_over else R.string.a_widget_left),
                        style = TextStyle(color = p.ink2, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(fmtInt(abs(s.left)), style = TextStyle(color = if (s.over) p.over else p.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(6.dp))
                    Progress(p, s, 6)
                }
            }
        }
    }
}

class BigWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BigWidget() }
class SmallWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = SmallWidget() }

object Widgets {
    fun refresh(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val m = GlanceAppWidgetManager(app)
                if (m.getGlanceIds(BigWidget::class.java).isNotEmpty()) BigWidget().updateAll(app)
                if (m.getGlanceIds(SmallWidget::class.java).isNotEmpty()) SmallWidget().updateAll(app)
            }
        }
    }
}

// ---------------- Quick Settings tiles ----------------

class WaterTile : TileService() {
    override fun onStartListening() {
        val w = runCatching { Store.get(this).data.value.liquidsMl(today()).first }.getOrDefault(0)
        val litres = "%.1f".format(w / 1000.0)
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.a_tile_water)
            if (Build.VERSION.SDK_INT >= 29) subtitle = getString(R.string.a_tile_water_sub, litres)
            if (Build.VERSION.SDK_INT >= 30) stateDescription = getString(R.string.a_tile_water_state, litres)
            updateTile()
        }
    }

    override fun onClick() {
        // from the lock screen this waits until the user unlocks
        unlockAndRun {
            runCatching { Store.get(this).addWater(today(), 250) }
            Widgets.refresh(this)
            onStartListening()
        }
    }
}

class QuickAddTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.a_tile_add)
            if (Build.VERSION.SDK_INT >= 30) stateDescription = getString(R.string.a_tile_add_state)
            updateTile()
        }
    }

    override fun onClick() {
        unlockAndRun {
            val intent = openIntent(this, "add")
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
        }
    }
}
