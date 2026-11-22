package app.unavu.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.unavu.data.Profile
import app.unavu.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds the app shell's state so it survives rotation (ViewModel) and process death (SavedStateHandle, as JSON):
 * the navigation state and the onboarding step + draft profile.
 */
class AppViewModel(private val handle: SavedStateHandle) : ViewModel() {
    private var navInstance: Nav? = null

    /** The Nav for [store] (created once, restored from the saved JSON, then kept in step with every change). */
    fun nav(store: Store): Nav = navInstance ?: Nav(store, handle.get<String>(KEY_NAV)).also { n ->
        navInstance = n
        viewModelScope.launch { n.stateFlow().collect { handle[KEY_NAV] = it } }
    }

    val onboardStep: StateFlow<Int> = handle.getStateFlow(KEY_STEP, 0)
    fun setOnboardStep(step: Int) { handle[KEY_STEP] = step }

    private val _draft = MutableStateFlow(
        handle.get<String>(KEY_DRAFT)?.let { runCatching { Json.decodeFromString<Profile>(it) }.getOrNull() } ?: Profile(),
    )
    val draftProfile: StateFlow<Profile> = _draft
    fun setDraftProfile(p: Profile) { _draft.value = p; handle[KEY_DRAFT] = Json.encodeToString(p) }

    private companion object {
        const val KEY_NAV = "nav"; const val KEY_STEP = "ob_step"; const val KEY_DRAFT = "ob_profile"
    }
}
