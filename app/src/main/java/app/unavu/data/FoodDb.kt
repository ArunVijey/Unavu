package app.unavu.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * The food database: ~700 ingredients (IFCT 2017 + a few hand-added) and ~160 Tamil Nadu dishes
 * calculated from them (built by tools/build_foods.py into assets/), plus the user's own recipes.
 */
object FoodDb {

    @Serializable
    private data class IngDto(
        val name: String, val group: String = "", val tamil: String = "",
        val kcal: Double = 0.0, val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0, val fibre: Double = 0.0,
        val sodium: Double = 0.0, val iron: Double = 0.0, val calcium: Double = 0.0, val potassium: Double = 0.0,
        val vitc: Double = 0.0, val vita: Double = 0.0, val satfat: Double = 0.0, val sugar: Double = 0.0,
        val source: String = "", val code: String = "",
        @SerialName("fat_role") val fatRole: Boolean = false,
        @SerialName("added_sugar") val addedSugar: Boolean = false,
    )

    @Serializable
    private data class UnitDto(val name: String, val g: Double)

    @Serializable
    private data class FoodDto(
        val id: String, val name: String, val tamil: String = "", val emoji: String = "🍽", val cat: String = "",
        val aliases: List<String> = emptyList(), val ing: Map<String, Double>,
        @SerialName("yield_") val yieldG: Double, val units: List<UnitDto>, val defq: Double = 1.0,
        val liquid: Boolean = false, val choices: List<ChoiceGroup> = emptyList(), val oil: Boolean = false,
        val source: String = "recipe",
    )

    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var ingredients: Map<String, Ingredient> = emptyMap()
    /** One made-up ingredient per "My food" (its typed-in values), so it works like every other food. */
    @Volatile private var factIngredients: Map<String, Ingredient> = emptyMap()
    @Volatile private var builtIn: List<Food> = emptyList()
    @Volatile private var custom: List<Food> = emptyList()
    @Volatile private var byId: Map<String, Food> = emptyMap()
    /** Plain IFCT foods ("Carrot, orange", "Ground nut"…) so anything can be logged by weight. */
    @Volatile private var rawFoods: List<Food> = emptyList()
    private val cache = ConcurrentHashMap<String, Nutr>()

    val loaded: Boolean get() = builtIn.isNotEmpty()
    val dishes: List<Food> get() = builtIn + custom
    val all: List<Food> get() = builtIn + custom + rawFoods

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val a = context.assets
            load(a.open("ingredients.json").bufferedReader().readText(), a.open("foods.json").bufferedReader().readText())
        }
    }

    fun load(ingredientsJson: String, foodsJson: String) {
        val ingDtos: Map<String, IngDto> = json.decodeFromString(ingredientsJson)
        ingredients = ingDtos.mapValues { (id, d) ->
            Ingredient(
                id = id, name = d.name, tamil = d.tamil, group = d.group, isFat = d.fatRole, source = d.source,
                per100 = Nutr(d.kcal, d.protein, d.carbs, d.fat, d.fibre, d.sodium, d.iron, d.calcium, d.potassium, d.vitc, d.vita, d.satfat,
                    if (d.addedSugar) d.sugar else 0.0),
            )
        }
        val foods: List<FoodDto> = json.decodeFromString(foodsJson)
        builtIn = foods.map { d ->
            Food(
                id = d.id, name = d.name, tamil = d.tamil, emoji = d.emoji,
                aliases = (d.aliases + d.name.lowercase() + d.tamil).filter { it.isNotBlank() }.distinct(),
                ing = d.ing, yieldG = d.yieldG,
                units = d.units.map { FoodUnit(it.name, it.g) }.let { us -> if (us.any { it.name == "g" || it.name == "ml" }) us else us + FoodUnit(if (d.liquid) "ml" else "g", 1.0) },
                defaultQty = d.defq, liquid = d.liquid, category = d.cat, choices = d.choices, hasOil = d.oil,
                source = FoodSource.RECIPE,
            )
        }
        // IFCT foods by their code (the friendly duplicate keys like "carrot" are skipped)
        rawFoods = ingredients.values.filter { it.source == "IFCT 2017" && it.id.length == 4 && it.id[0].isUpperCase() }.map { i ->
            Food(
                id = "ing:${i.id}", name = i.name, tamil = i.tamil, emoji = "🥕", aliases = listOf(i.name.lowercase(), i.tamil.lowercase()).filter { it.isNotBlank() },
                ing = mapOf(i.id to 100.0), yieldG = 100.0, units = listOf(FoodUnit("100 g", 100.0), FoodUnit("g", 1.0)),
                category = "Ingredient · ${i.group}", source = FoodSource.IFCT,
            )
        }
        reindex()
    }

    fun setCustom(list: List<CustomFood>) {
        val foods = list.map { it.toFood() }
        factIngredients = list.filter { it.facts != null }.associate { f -> f.factKey() to f.factIngredient() }
        cache.keys.removeAll { it.startsWith("u_") }
        if (foods == custom) return
        custom = foods
        cache.keys.removeAll { it.startsWith("u_") }
        reindex()
    }

    private fun reindex() {
        byId = (builtIn + custom + rawFoods).associateBy { it.id }
    }

    fun get(id: String): Food? = byId[id]
    fun ingredient(id: String): Ingredient? = ingredients[id] ?: factIngredients[id]
    fun allIngredients(): Collection<Ingredient> = ingredients.values

    fun per100(food: Food, c: Choices): Nutr {
        val key = food.id + "|" + c.toSortedMap().entries.joinToString(";") { (k, v) -> k + "=" + v.sorted().joinToString(",") }
        return cache.getOrPut(key) {
            val (batch, yieldG) = food.batch(c)
            val total = batch.entries.fold(Nutr()) { acc, (k, g) -> acc + ((ingredients[k] ?: factIngredients[k])?.per100 ?: Nutr()) * (g / 100.0) }
            if (yieldG <= 0) Nutr() else total * (100.0 / yieldG)
        }
    }

    private fun score(aliases: List<String>, q: String): Int = aliases.minOfOrNull { a ->
        when {
            a == q -> 0
            a.startsWith(q) -> 1
            a.split(" ").any { it.startsWith(q) } -> 2
            a.contains(q) -> 3
            q.length >= 4 && editDistance(a, q) <= 1 -> 4
            q.length >= 6 && editDistance(a, q) <= 2 -> 5
            else -> 9
        }
    } ?: 9

    /** Dishes first, then plain IFCT ingredients. */
    fun search(query: String, limit: Int = 40, includeRaw: Boolean = true): List<Food> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val dishHits = dishes.map { it to score(it.aliases, q) }.filter { it.second < 9 }.sortedBy { it.second }.map { it.first }
        val rawHits = if (!includeRaw) emptyList() else rawFoods.map { it to score(it.aliases, q) }.filter { it.second < 4 }.sortedBy { it.second }.map { it.first }
        return (dishHits + rawHits).take(limit)
    }

    fun searchIngredients(query: String, limit: Int = 30): List<Ingredient> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return ingredients.values.filter { it.id.length == 4 || it.source.startsWith("custom") }
            .map { it to score(listOf(it.name.lowercase(), it.tamil.lowercase()).filter(String::isNotBlank), q) }
            .filter { it.second < 4 }.sortedBy { it.second }.map { it.first }.take(limit)
    }
}

/** Levenshtein distance, small strings only (typo tolerance in search). */
fun editDistance(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 2) return 3
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]; dp[0] = i
        for (j in 1..b.length) {
            val tmp = dp[j]
            dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = tmp
        }
    }
    return dp[b.length]
}

fun CustomFood.toFood() = Food(
    id = id, name = name, tamil = "", emoji = emoji, aliases = listOf(name.lowercase()),
    ing = if (facts != null) mapOf(factKey() to unitG) else ing, yieldG = if (facts != null) unitG else yieldG,
    units = listOf(FoodUnit(unitName, unitG), FoodUnit(if (liquid) "ml" else "g", 1.0)), liquid = liquid,
    category = if (facts != null) "My foods" else "My recipes", hasOil = false, source = FoodSource.CUSTOM,
)

fun CustomFood.factKey() = "f_$id"

private fun CustomFood.factIngredient(): Ingredient {
    val f = facts!!
    val k = 100.0 / unitG.coerceAtLeast(1.0)
    return Ingredient(
        id = factKey(), name = "$name (your values)", tamil = "", group = "My foods", isFat = false, source = "custom: you",
        per100 = Nutr(
            kcal = f.kcal, protein = f.protein, carbs = f.carbs, fat = f.fat, fibre = f.fibre, sodium = f.sodium,
            iron = f.iron, calcium = f.calcium, potassium = f.potassium, satFat = f.satFat, sugar = f.sugar,
        ) * k,
    )
}
