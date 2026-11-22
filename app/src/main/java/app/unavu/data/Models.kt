package app.unavu.data

import kotlinx.serialization.Serializable

@Serializable
enum class Meal(val label: String, val tamil: String, val emoji: String) {
    BREAKFAST("Breakfast", "காலை", "☀️"),
    LUNCH("Lunch", "மதியம்", "🍛"),
    SNACKS("Snacks", "சிற்றுண்டி", "☕"),
    DINNER("Dinner", "இரவு", "🌙");

    companion object {
        /** Only used for suggestions (e.g. "next meal"), never to file a log automatically. */
        fun forHour(hour: Int): Meal = when {
            hour < 11 -> BREAKFAST
            hour < 16 -> LUNCH
            hour < 19 -> SNACKS
            else -> DINNER
        }
    }
}

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class AppFont(val label: String) { GOOGLE_SANS_FLEX("Google Sans Flex"), INTER("Inter"), EXCALIFONT("Excalifont") }

@Serializable
data class Profile(
    val male: Boolean = true,
    val age: Int = 28,
    val heightCm: Double = 172.0,
    val weightKg: Double = 72.0,
    /** Activity multiplier for maintenance calories. */
    val activity: Double = 1.55,
    /** Daily calorie adjustment for the goal: -500 lose, 0 maintain, +300 gain. */
    val goalKcal: Int = -500,
    val goalWeightKg: Double? = null,
)

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val font: AppFont = AppFont.GOOGLE_SANS_FLEX,
    /** Share of the daily calories for Breakfast, Lunch, Snacks, Dinner. */
    val mealSplit: List<Double> = listOf(0.25, 0.35, 0.15, 0.25),
    val waterGoalMl: Int = 2500,
    /** Count buttermilk / tender coconut / juices towards the water goal. */
    val countDrinksAsWater: Boolean = false,
    /** Nutrient chips shown on Today (ids from the nutrient list). */
    val homeChips: List<String> = listOf("carbs", "protein", "fat", "sodium", "fibre", "iron"),
    /** Accent colour (ARGB). Null = the default turmeric yellow. */
    val accent: Int? = null,
    /** Hand-drawn "Sketch" style (Excalifont + wobbly outlines + dotted paper). */
    val sketch: Boolean = false,
)

@Serializable
data class LogEntry(
    val id: Long,
    /** yyyy-MM-dd */
    val date: String,
    val meal: Meal,
    val foodId: String,
    val qty: Double,
    val unit: String,
    /** HH:mm when it was logged */
    val time: String,
    val cheat: Boolean = false,
    /** Picked options per choice group (vegetables, dal, sugar, milk, oil…). */
    val choices: Map<String, List<String>> = emptyMap(),
)

/** The last portion + choices used for a food; pre-filled the next time it is logged. */
@Serializable
data class Portion(val qty: Double, val unit: String, val choices: Map<String, List<String>> = emptyMap())

@Serializable
data class WeightEntry(val date: String, val kg: Double, val waistCm: Double? = null)

/** A dish the user built from ingredients ("Amma's puli kuzhambu"). */
@Serializable
data class CustomFood(
    val id: String,
    val name: String,
    val emoji: String = "🍲",
    /** ingredient id -> raw grams for one batch */
    val ing: Map<String, Double>,
    /** cooked weight (g) or volume (ml) of the batch */
    val yieldG: Double,
    val unitName: String = "serving",
    val unitG: Double,
    val liquid: Boolean = false,
    /** Set for "My foods" entered from a label or a guess: nutrients for ONE serving ([unitG]). No ingredients then. */
    val facts: Facts? = null,
)

/** Nutrition for one serving, typed in by the user (from a pack label, a menu, or a guess). */
@Serializable
data class Facts(
    val kcal: Double,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fibre: Double = 0.0,
    val sugar: Double = 0.0,
    val satFat: Double = 0.0,
    val sodium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val potassium: Double = 0.0,
)

@Serializable
data class ComboItem(val foodId: String, val qty: Double, val unit: String, val choices: Map<String, List<String>> = emptyMap())

/** A saved plate ("My usual lunch"). */
@Serializable
data class Combo(
    val id: String,
    val name: String,
    val emoji: String = "⭐",
    val meal: Meal? = null,
    val items: List<ComboItem>,
    val uses: Int = 0,
    val order: Int = 0,
)

@Serializable
data class BackupSettings(
    /** content:// tree URI the user picked (persisted permission). */
    val folderUri: String? = null,
    val folderName: String? = null,
    val daily: Boolean = true,
    val hour: Int = 23,
    val minute: Int = 30,
    val lastAt: Long? = null,
    val lastOk: Boolean = true,
    val lastMessage: String? = null,
)

@Serializable
enum class SmartMode { AUTO, ASK, MANUAL }

@Serializable
data class SmartTarget(
    val mode: SmartMode = SmartMode.ASK,
    /** Used when mode = MANUAL, or when the user overrides. */
    val manualKcal: Int? = null,
    /** Maintenance (burn) estimate currently applied, null = use the formula. */
    val appliedBurn: Int? = null,
    /** Pending estimate waiting for the user's OK (mode ASK). */
    val pendingBurn: Int? = null,
    val lastCheckDate: String? = null,
    /** History of weekly estimates: date -> estimated burn. */
    val history: Map<String, Int> = emptyMap(),
)

@Serializable
data class Reminder(val id: String, val label: String, val hour: Int, val minute: Int, val on: Boolean)

@Serializable
data class AppData(
    val version: Int = 2,
    val profile: Profile? = null,
    val settings: Settings = Settings(),
    val entries: List<LogEntry> = emptyList(),
    val portions: Map<String, Portion> = emptyMap(),
    val weights: List<WeightEntry> = emptyList(),
    /** Plain water in ml per date (drinks with calories are normal log entries). */
    val water: Map<String, Int> = emptyMap(),
    val cheatDays: Set<String> = emptySet(),
    val customFoods: List<CustomFood> = emptyList(),
    val combos: List<Combo> = emptyList(),
    val backup: BackupSettings = BackupSettings(),
    val smart: SmartTarget = SmartTarget(),
    val reminders: List<Reminder> = emptyList(),
    /** Days with notes ("Cousin's wedding"). */
    val dayNotes: Map<String, String> = emptyMap(),
)
