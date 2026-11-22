package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.NewItem
import app.unavu.data.fmtQty
import app.unavu.data.nutrFor
import app.unavu.data.unitLabel
import app.unavu.ui.EmojiText
import app.unavu.ui.LocalData
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.data.FoodDb
import kotlin.math.roundToInt

@Composable
internal fun ParsedRow(item: NewItem, onClick: () -> Unit) {
    val food = FoodDb.get(item.foodId) ?: return
    val data = LocalData.current
    val t = U.t
    val remembered = data.portions[item.foodId]?.let { it.qty == item.qty && it.unit == item.unit } == true
    val summary = food.choiceSummary(item.choices)
    val kcal = food.nutrFor(item.qty, item.unit, item.choices).kcal.roundToInt()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${food.emoji} ${food.name} × ${fmtQty(item.qty)} ${unitLabel(item.unit, item.qty)}",
                style = t.bodyS,
            )
            if (summary.isNotBlank()) Text(summary, style = t.caption, color = U.c.leafInk, fontWeight = FontWeight.SemiBold)
            if (remembered || food.liquid) {
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (remembered) StatusBadge(stringResource(R.string.f_your_usual), Status.OK)
                    if (food.liquid) StatusBadge(stringResource(R.string.f_to_liquids), Status.INFO)
                }
            }
        }
        Text(stringResource(R.string.f_kcal, kcal), style = t.bodyS, fontWeight = FontWeight.Bold)
    }
}

/** Big accent button. [enabled] = false dims it; the label itself says why (see QuickAdd). */
@Composable
fun AddButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = U.c
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(if (enabled) c.acc else c.acc.copy(alpha = 0.35f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = U.t.body, fontWeight = FontWeight.ExtraBold, color = if (enabled) c.onAcc else c.ink2) }
}

@Composable
fun LiquidTile(emoji: String, name: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    val t = U.t
    Column(
        modifier.edge(if (on) c.blue else c.line, 14.dp, 1.5.dp, seed = name.length).clip(RoundedCornerShape(14.dp)).background(if (on) c.blueS else c.sf)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(onClick = onClick).heightIn(min = 48.dp).padding(top = 12.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmojiText(emoji, 24)
        Spacer(Modifier.height(4.dp))
        Text(name, style = t.label, fontWeight = FontWeight.Bold)
        Text(sub, style = t.caption, color = c.ink2)
    }
}
