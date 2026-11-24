package app.unavu.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.FoodDb
import app.unavu.data.fmtInt
import app.unavu.ui.AppCard
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U

/** The installed version name (e.g. "1.2.1"), read from the package so it can never drift from the build. */
private fun appVersion(context: Context): String? = runCatching {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    else @Suppress("DEPRECATION") pm.getPackageInfo(context.packageName, 0)
    info.versionName
}.getOrNull()

@Composable
fun AboutScreen() {
    val c = U.c
    val nav = nav
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_about_title), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            AppCard {
                Text(stringResource(R.string.i_about_app), style = U.t.titleS.iBold(), color = c.ink)
                if (version != null) Text(stringResource(R.string.r7d_version, version), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(bottom = 4.dp))
                Text(stringResource(R.string.i_about_app_text), style = U.t.bodyS, color = c.ink2)
            }
            AppCard {
                Text(stringResource(R.string.i_numbers_title), style = U.t.body.iBold(), color = c.ink)
                Text(
                    stringResource(R.string.i_numbers_text, fmtInt(FoodDb.allIngredients().size), fmtInt(FoodDb.dishes.size)),
                    style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            StatusBadge(stringResource(R.string.i_ifct_credit), Status.INFO)
            Spacer(Modifier.height(24.dp))
        }
    }
}
