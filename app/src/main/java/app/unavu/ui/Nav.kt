package app.unavu.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.Meal
import app.unavu.data.Store
import app.unavu.data.today

/** Pages pushed on top of the 4 tabs. */
@Serializable
sealed interface Page {
    @Serializable data object Profile : Page
    @Serializable data object Goals : Page
    @Serializable data object Appearance : Page
    @Serializable data object Portions : Page
    @Serializable data object SmartTarget : Page
    @Serializable data object Backup : Page
    @Serializable data object Reminders : Page
    @Serializable data object About : Page
    @Serializable data object History : Page
    @Serializable data object Liquids : Page
    @Serializable data object Weight : Page
    /** Build a plate for a meal (or a combo when [comboId] is set / [asCombo] is true). */
    @Serializable data class Plate(val meal: Meal?, val date: String, val comboId: String? = null, val asCombo: Boolean = false) : Page
    @Serializable data class FoodDetail(val foodId: String, val date: String, val meal: Meal? = null) : Page
    @Serializable data class Recipe(val id: String?) : Page
    @Serializable data object MyFoods : Page
    /** Add (id = null, optional prefilled [name]) or edit one of "My foods". */
    @Serializable data class MyFood(val id: String?, val name: String = "") : Page
}

/** The 4 root tabs. Labels are string resources (serialized by name, so renaming a label never breaks saved state). */
@Serializable
enum class Tab(val glyph: NavGlyph, @StringRes val labelRes: Int) {
    TODAY(NavGlyph.TODAY, R.string.b_tab_today),
    INSIGHTS(NavGlyph.INSIGHTS, R.string.b_tab_insights),
    MEALS(NavGlyph.MEALS, R.string.b_tab_meals),
    SETTINGS(NavGlyph.SETTINGS, R.string.b_tab_settings),
}

@Serializable
private class NavState(val stack: List<Page> = emptyList(), val tab: Tab = Tab.TODAY, val date: String? = null, val quickAdd: QuickAddRequest? = null)

/**
 * App-wide navigation + shared state. Saveable: pass the JSON from [saveState] back as [restored]
 * (AppViewModel keeps it in a SavedStateHandle, so it survives rotation and process death).
 */
@Stable
class Nav(val store: Store, restored: String? = null) {
    val stack = mutableStateListOf<Page>()
    val tab = mutableStateOf(Tab.TODAY)
    /** The day Today is showing (also where new logs go). */
    val date = mutableStateOf(today())
    /** Open the quick-add sheet (optionally preset to a meal). */
    val quickAdd = mutableStateOf<QuickAddRequest?>(null)

    init {
        if (restored != null) runCatching { Json.decodeFromString<NavState>(restored) }.getOrNull()?.let {
            stack.addAll(it.stack); tab.value = it.tab; it.date?.let { d -> date.value = d }; quickAdd.value = it.quickAdd
        }
    }

    fun push(p: Page) { stack.add(p) }
    fun pop(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.lastIndex); true } else false
    fun popTo(predicate: (Page) -> Boolean) { while (stack.isNotEmpty() && !predicate(stack.last())) stack.removeAt(stack.lastIndex) }
    fun home() { stack.clear(); tab.value = Tab.TODAY }

    /** JSON of the current navigation state (for SavedStateHandle). */
    fun saveState(): String = Json.encodeToString(NavState(stack.toList(), tab.value, date.value, quickAdd.value))

    /** Emits the saved JSON whenever any navigation state changes. */
    fun stateFlow() = snapshotFlow { saveState() }
}

@Serializable
data class QuickAddRequest(val meal: Meal? = null, val mode: String = "FOOD")

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }
/**
 * The whole app data, fed from the Store's StateFlow in MainActivity.
 * Dynamic (not static) on purpose: a write (e.g. one glass of water) recomposes only the composables that read
 * `LocalData.current`, instead of invalidating the entire tree under the provider.
 */
val LocalData = compositionLocalOf { AppData() }

val nav: Nav @Composable get() = LocalNav.current
