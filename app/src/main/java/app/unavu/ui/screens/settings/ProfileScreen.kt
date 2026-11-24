package app.unavu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json
import androidx.compose.ui.res.stringResource
import app.unavu.R
import app.unavu.data.Profile
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.nav

@Composable
fun ProfileScreen() {
    val nav = nav
    val data = LocalData.current
    val feedback = LocalFeedback.current
    val saved = stringResource(R.string.i_details_saved)
    var step by rememberSaveable { mutableStateOf(0) }
    // AboutYouScreen keeps its own text fields saveable; the confirmed draft is saveable too, so rotating on the
    // targets step never falls back to the old profile.
    var draft by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(data.profile ?: Profile()) }
    if (step == 0) {
        AboutYouScreen(draft, stringResource(R.string.i_your_details), onBack = { nav.pop() }, buttonText = stringResource(R.string.i_see_targets)) { draft = it; step = 1 }
    } else {
        BackHandler { step = 0 }
        TargetsScreen(draft, onBack = { step = 0 }, buttonText = stringResource(R.string.i_save), showSteps = false) {
            nav.store.update { it.copy(profile = draft.copy(goalWeightKg = it.profile?.goalWeightKg)) }
            feedback?.show(saved)
            nav.pop()
        }
    }
}

private val ProfileSaver = Saver<Profile, String>(
    save = { Json.encodeToString(Profile.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(Profile.serializer(), it) }.getOrNull() },
)
