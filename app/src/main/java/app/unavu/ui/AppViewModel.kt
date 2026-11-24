package app.unavu.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.unavu.data.NewItem
import app.unavu.data.Profile
import app.unavu.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * What was in the quick-add sheet when it was closed without adding (✕, scrim, back, or a link to another screen).
 * Reopening the sheet in the same mode within [AppViewModel.DRAFT_TTL_MS] brings it back.
 */
@Serializable
data class QuickAddDraft(
    /** Mode name (FOOD / LIQUID / WEIGHT). */
    val mode: String,
    val text: String = "",
    val meal: String? = null,
    val cheat: Boolean = false,
    /** Water added with the tiles (typed water lives in [text]). */
    val waterMl: Int = 0,
    val waist: String = "",
    /** Customised typed items, keyed like the sheet keys them ("position:food id"). */
    val overrides: Map<String, NewItem> = emptyMap(),
    /** Items from a tapped combo. */
    val comboItems: List<NewItem> = emptyList(),
    /** System time when the sheet was closed. */
    val savedAt: Long = 0,
) {
    /** Nothing worth keeping. */
    val isEmpty: Boolean get() = text.isBlank() && waterMl <= 0 && waist.isBlank() && comboItems.isEmpty()
}

/**
 * Holds the app shell's state so it survives rotation (ViewModel) and process death (SavedStateHandle, as JSON):
 * the navigation state, the onboarding step + draft profile, and the quick-add draft.
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

    /** Keep what the quick-add sheet held when it closed without adding (an empty draft clears it). */
    fun saveQuickAddDraft(d: QuickAddDraft, now: Long = System.currentTimeMillis()) {
        if (d.isEmpty) {
            clearQuickAddDraft()
        } else {
            handle[KEY_QA_DRAFT] = Json.encodeToString(d.copy(savedAt = now))
        }
    }

    /**
     * The quick-add draft for [mode] if it was closed less than 10 minutes ago, else null. An expired draft is
     * dropped; a fresh draft for another mode is kept for when that mode is opened.
     */
    fun quickAddDraft(mode: String, now: Long = System.currentTimeMillis()): QuickAddDraft? {
        val d = handle.get<String>(KEY_QA_DRAFT)?.let { runCatching { Json.decodeFromString<QuickAddDraft>(it) }.getOrNull() } ?: return null
        val age = now - d.savedAt
        if (age < 0 || age > DRAFT_TTL_MS) { clearQuickAddDraft(); return null }
        return d.takeIf { it.mode == mode }
    }

    fun clearQuickAddDraft() { handle.remove<String>(KEY_QA_DRAFT) }

    companion object {
        /** How long a closed quick-add sheet keeps what was typed. */
        const val DRAFT_TTL_MS = 10 * 60 * 1000L
        private const val KEY_NAV = "nav"; private const val KEY_STEP = "ob_step"; private const val KEY_DRAFT = "ob_profile"
        private const val KEY_QA_DRAFT = "qa_draft"
    }
}
