package app.unavu.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * All app data lives in one JSON file in the app's private storage. It is small (even years of
 * logs are a few MB), written atomically (temp file + rename) after every change, and is the same
 * format the backup exports.
 */
class Store private constructor(private val file: File) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(migrate(load()))
    val data: StateFlow<AppData> = _data

    private var nextId = (_data.value.entries.maxOfOrNull { it.id } ?: 0L) + 1

    init {
        FoodDb.setCustom(_data.value.customFoods)
    }

    fun newId(): Long = synchronized(this) { nextId++ }

    fun update(change: (AppData) -> AppData) {
        _data.update(change)
        val snapshot = _data.value
        FoodDb.setCustom(snapshot.customFoods)
        scope.launch { writeLock.withLock { save(snapshot) } }
    }

    /** Replace everything (restore from backup). */
    fun replaceAll(d: AppData) {
        update { migrate(d) }
        nextId = (d.entries.maxOfOrNull { it.id } ?: 0L) + 1
    }

    // ---- round-1 skeleton APIs (owner: Agent A fills in the real behaviour) ----
    private val _loadIssue = MutableStateFlow<LoadIssue?>(null)
    /** Non-null when the data file was unreadable at start (a copy was kept). */
    val loadIssue: StateFlow<LoadIssue?> = _loadIssue
    fun clearLoadIssue() { _loadIssue.value = null }
    /** Undo: put back an earlier snapshot. */
    fun restoreSnapshot(snapshot: AppData) = update { snapshot }
    /** Write pending changes now (called from onStop). */
    fun flush() { val snap = _data.value; scope.launch { writeLock.withLock { save(snap) } } }

    fun exportJson(): String = JSON.encodeToString(AppData.serializer(), _data.value)

    private fun load(): AppData = runCatching {
        if (file.exists()) JSON.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    }.getOrElse { AppData() }

    private fun save(d: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(JSON.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(file)) {
            file.delete(); tmp.renameTo(file)
        }
    }

    companion object {
        val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

        @Volatile private var instance: Store? = null
        /** The store if it's already loaded, without blocking. */
        fun peek(): Store? = instance
        fun get(context: Context): Store = instance ?: synchronized(this) {
            FoodDb.init(context)
            instance ?: Store(File(context.applicationContext.filesDir, "unavu-data.json")).also { instance = it }
        }

        val DEFAULT_REMINDERS = listOf(
            Reminder("breakfast", "Breakfast", 8, 30, true),
            Reminder("lunch", "Lunch", 13, 30, true),
            Reminder("snacks", "Snacks", 17, 0, false),
            Reminder("dinner", "Dinner", 21, 0, true),
            Reminder("water", "Water (every 2 h, 9 AM – 9 PM)", 9, 0, true),
            Reminder("weigh", "Weigh-in", 7, 0, true),
        )

        /** Upgrades older saved data to the current version. */
        fun migrate(d: AppData): AppData {
            var x = d
            if (x.version < 2) {
                x = x.copy(
                    version = 2,
                    settings = x.settings.copy(waterGoalMl = if (x.settings.waterGoalMl == 2000) 2500 else x.settings.waterGoalMl),
                    // old food ids that were renamed in the IFCT database
                    entries = x.entries.map { e -> e.copy(foodId = RENAMED[e.foodId] ?: e.foodId) },
                    portions = x.portions.mapKeys { (k, _) -> RENAMED[k] ?: k },
                )
            }
            if (x.reminders.isEmpty()) x = x.copy(reminders = DEFAULT_REMINDERS)
            return x
        }

        private val RENAMED = mapOf(
            "onion_uthappam" to "uthappam", "dal" to "dal_fry", "keerai_poriyal" to "keerai", "orange_juice" to "fresh_juice",
            "mcaloo_tikki" to "veg_burger", "mcchicken" to "chicken_burger", "gingelly_oil" to "gingelly_oil_side",
            "ghee" to "ghee_side", "bisibelabath" to "sambar_rice", "noodles" to "veg_noodles",
        )
    }
}

data class LoadIssue(val keptCopyPath: String)

// ---------- common actions ----------

data class NewItem(val foodId: String, val qty: Double, val unit: String, val choices: Choices = emptyMap())

fun QuickParser.Item.toNew() = NewItem(food.id, qty, unit, choices)
fun ComboItem.toNew() = NewItem(foodId, qty, unit, choices)
fun NewItem.toCombo() = ComboItem(foodId, qty, unit, choices)

fun Store.logItems(items: List<NewItem>, meal: Meal, date: String, cheat: Boolean) = update { d ->
    val time = nowTime()
    val newEntries = items.map { LogEntry(newId(), date, meal, it.foodId, it.qty, it.unit, time, cheat, it.choices) }
    d.copy(
        entries = d.entries + newEntries,
        portions = d.portions + items.associate { it.foodId to Portion(it.qty, it.unit, it.choices) },
    )
}

fun Store.updateEntry(e: LogEntry) = update { d ->
    d.copy(
        entries = d.entries.map { if (it.id == e.id) e else it },
        portions = d.portions + (e.foodId to Portion(e.qty, e.unit, e.choices)),
    )
}

fun Store.deleteEntry(id: Long) = update { d -> d.copy(entries = d.entries.filterNot { it.id == id }) }

fun Store.setMealCheat(date: String, meal: Meal, cheat: Boolean) = update { d ->
    d.copy(entries = d.entries.map { if (it.date == date && it.meal == meal) it.copy(cheat = cheat) else it })
}

fun Store.setCheatDay(date: String, cheat: Boolean, note: String? = null) = update { d ->
    d.copy(
        cheatDays = if (cheat) d.cheatDays + date else d.cheatDays - date,
        dayNotes = if (note.isNullOrBlank()) d.dayNotes - date else d.dayNotes + (date to note),
    )
}

fun Store.deleteMeal(date: String, meal: Meal) = update { d ->
    d.copy(entries = d.entries.filterNot { it.date == date && it.meal == meal })
}

fun Store.copyMeal(fromDate: String, from: Meal, toDate: String, to: Meal) = update { d ->
    val time = nowTime()
    val copies = d.entries.filter { it.date == fromDate && it.meal == from }.map { it.copy(id = newId(), date = toDate, meal = to, time = time, cheat = false) }
    d.copy(entries = d.entries + copies)
}

fun Store.copyDay(fromDate: String, toDate: String) = update { d ->
    val time = nowTime()
    val copies = d.entries.filter { it.date == fromDate }.map { it.copy(id = newId(), date = toDate, time = time) }
    d.copy(entries = d.entries + copies)
}

fun Store.addWater(date: String, ml: Int) = update { d ->
    d.copy(water = d.water + (date to ((d.water[date] ?: 0) + ml).coerceAtLeast(0)))
}

fun Store.addWeight(date: String, kg: Double, waist: Double? = null) = update { d ->
    val old = d.weights.firstOrNull { it.date == date }
    d.copy(
        weights = (d.weights.filterNot { it.date == date } + WeightEntry(date, kg, waist ?: old?.waistCm)).sortedBy { it.date },
        profile = if (date >= (d.weights.maxOfOrNull { it.date } ?: "")) d.profile?.copy(weightKg = kg) else d.profile,
    )
}

fun Store.deleteWeight(date: String) = update { d -> d.copy(weights = d.weights.filterNot { it.date == date }) }

fun Store.saveCombo(c: Combo) = update { d ->
    val exists = d.combos.any { it.id == c.id }
    d.copy(combos = if (exists) d.combos.map { if (it.id == c.id) c else it } else d.combos + c.copy(order = (d.combos.maxOfOrNull { it.order } ?: 0) + 1))
}

fun Store.deleteCombo(id: String) = update { d -> d.copy(combos = d.combos.filterNot { it.id == id }) }

fun Store.useCombo(c: Combo, meal: Meal, date: String) {
    logItems(c.items.map { it.toNew() }, meal, date, cheat = false)
    update { d -> d.copy(combos = d.combos.map { if (it.id == c.id) it.copy(uses = it.uses + 1) else it }) }
}

fun Store.saveCustomFood(f: CustomFood) = update { d ->
    d.copy(customFoods = d.customFoods.filterNot { it.id == f.id } + f)
}

fun Store.deleteCustomFood(id: String) = update { d -> d.copy(customFoods = d.customFoods.filterNot { it.id == id }) }

fun newKey(prefix: String) = prefix + UUID.randomUUID().toString().take(8)
