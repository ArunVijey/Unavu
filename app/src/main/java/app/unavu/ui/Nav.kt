package app.unavu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import app.unavu.data.AppData
import app.unavu.data.Meal
import app.unavu.data.Store

/** Pages pushed on top of the 4 tabs. */
sealed interface Page {
    data object Profile : Page
    data object Goals : Page
    data object Appearance : Page
    data object Portions : Page
    data object SmartTarget : Page
    data object Backup : Page
    data object Reminders : Page
    data object About : Page
    data object History : Page
    data object Liquids : Page
    data object Weight : Page
    /** Build a plate for a meal (or a combo when [comboId] is set / [asCombo] is true). */
    data class Plate(val meal: Meal?, val date: String, val comboId: String? = null, val asCombo: Boolean = false) : Page
    data class FoodDetail(val foodId: String, val date: String, val meal: Meal? = null) : Page
    data class Recipe(val id: String?) : Page
    data object MyFoods : Page
    /** Add (id = null, optional prefilled [name]) or edit one of "My foods". */
    data class MyFood(val id: String?, val name: String = "") : Page
}

enum class Tab(val glyph: NavGlyph, val label: String) { TODAY(NavGlyph.TODAY, "Today"), INSIGHTS(NavGlyph.INSIGHTS, "Insights"), MEALS(NavGlyph.MEALS, "Meals"), SETTINGS(NavGlyph.SETTINGS, "Settings") }

/** App-wide navigation + shared state. */
@Stable
class Nav(val store: Store) {
    val stack = mutableStateListOf<Page>()
    val tab = mutableStateOf(Tab.TODAY)
    /** The day Today is showing (also where new logs go). */
    val date = mutableStateOf(app.unavu.data.today())
    /** Open the quick-add sheet (optionally preset to a meal). */
    val quickAdd = mutableStateOf<QuickAddRequest?>(null)

    fun push(p: Page) { stack.add(p) }
    fun pop(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.lastIndex); true } else false
    fun popTo(predicate: (Page) -> Boolean) { while (stack.isNotEmpty() && !predicate(stack.last())) stack.removeAt(stack.lastIndex) }
    fun home() { stack.clear(); tab.value = Tab.TODAY }
}

data class QuickAddRequest(val meal: Meal? = null, val mode: String = "FOOD")

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }
val LocalData = compositionLocalOf { AppData() }

val nav: Nav @Composable get() = LocalNav.current
