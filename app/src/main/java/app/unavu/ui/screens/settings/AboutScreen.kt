package app.unavu.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.FoodDb
import app.unavu.ui.AppCard
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U

@Composable
fun AboutScreen() {
    val c = U.c
    val nav = nav
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_about_title), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Text(stringResource(R.string.i_about_app), style = U.t.titleS.iBold(), color = c.ink)
                Text(stringResource(R.string.i_about_app_text), style = U.t.bodyS, color = c.ink2)
            }
            AppCard {
                Text(stringResource(R.string.i_numbers_title), style = U.t.body.iBold(), color = c.ink)
                Text(
                    stringResource(R.string.i_numbers_text, FoodDb.allIngredients().size, FoodDb.dishes.size),
                    style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            StatusBadge(stringResource(R.string.i_ifct_credit), Status.INFO)
            Spacer(Modifier.height(24.dp))
        }
    }
}
