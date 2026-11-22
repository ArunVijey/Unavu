package app.unavu.data

import kotlinx.serialization.Serializable

/** Nutrients. Per 100 g (or 100 ml) inside foods, absolute amounts once a portion is applied. */
data class Nutr(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fibre: Double = 0.0,
    val sodium: Double = 0.0,    // mg
    val iron: Double = 0.0,      // mg
    val calcium: Double = 0.0,   // mg
    val potassium: Double = 0.0, // mg
    val vitC: Double = 0.0,      // mg
    val vitA: Double = 0.0,      // µg
    val satFat: Double = 0.0,    // g
    val sugar: Double = 0.0,     // g, ADDED sugar only (sugar, jaggery, syrups, soft drinks…)
) {
    operator fun plus(o: Nutr) = Nutr(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat, fibre + o.fibre, sodium + o.sodium,
        iron + o.iron, calcium + o.calcium, potassium + o.potassium, vitC + o.vitC, vitA + o.vitA, satFat + o.satFat, sugar + o.sugar,
    )

    operator fun times(k: Double) = Nutr(
        kcal * k, protein * k, carbs * k, fat * k, fibre * k, sodium * k, iron * k, calcium * k,
        potassium * k, vitC * k, vitA * k, satFat * k, sugar * k,
    )
}

data class FoodUnit(val name: String, val grams: Double)

data class Ingredient(
    val id: String,
    val name: String,
    val tamil: String,
    val group: String,
    val per100: Nutr,
    val isFat: Boolean,
    val source: String,
)

@Serializable
data class ChoiceOption(val id: String, val label: String, val ing: Map<String, Double> = emptyMap(), val default: Boolean = false)

@Serializable
data class ChoiceGroup(
    val id: String,
    val label: String,
    val options: List<ChoiceOption>,
    val multi: Boolean = false,
    /** For multi groups: grams shared equally by the picked options. */
    val total: Double? = null,
    /** How much of the added raw weight ends up in the cooked yield. */
    val yieldf: Double = 1.0,
)

/** Picked options per choice group id. The universal oil level uses the key [OIL]. */
typealias Choices = Map<String, List<String>>

const val OIL = "oil"
val OIL_LEVELS = listOf("less" to "Less oil", "usual" to "As usual", "extra" to "Extra oil")
private val OIL_FACTOR = mapOf("less" to 0.6, "usual" to 1.0, "extra" to 1.6)

enum class FoodSource(val label: String) {
    RECIPE("Recipe · IFCT 2017"),
    IFCT("IFCT 2017"),
    CUSTOM("My recipe"),
    ESTIMATED("Estimated"),
}

data class Food(
    val id: String,
    val name: String,
    val tamil: String,
    val emoji: String,
    /** Lower-case search names: English, Tanglish spellings and Tamil script. */
    val aliases: List<String>,
    /** Raw ingredient grams for one batch (already flattened to plain ingredients). */
    val ing: Map<String, Double>,
    val yieldG: Double,
    val units: List<FoodUnit>,
    val defaultQty: Double = 1.0,
    val liquid: Boolean = false,
    val category: String,
    val choices: List<ChoiceGroup> = emptyList(),
    /** Has enough cooking fat for the Less / As usual / Extra oil choice to matter. */
    val hasOil: Boolean = false,
    val source: FoodSource = FoodSource.RECIPE,
) {
    fun unit(name: String): FoodUnit = units.firstOrNull { it.name == name } ?: units.first()

    fun defaultChoices(): Choices = choices.associate { g -> g.id to g.options.filter { it.default }.map { it.id } }

    /** Choices filled in with defaults for any group not picked. */
    fun resolve(c: Choices): Choices = defaultChoices() + c.filterKeys { k -> k == OIL || choices.any { it.id == k } }

    fun choiceSummary(c: Choices): String {
        val r = resolve(c)
        val parts = choices.mapNotNull { g ->
            val picked = r[g.id].orEmpty()
            if (picked.isEmpty()) null else g.options.filter { it.id in picked }.joinToString(" + ") { it.label }
        }.toMutableList()
        r[OIL]?.firstOrNull()?.takeIf { it != "usual" && hasOil }?.let { lvl -> parts += OIL_LEVELS.first { it.first == lvl }.second }
        return parts.joinToString(" · ")
    }
}

/** Grams of each ingredient in one batch plus the cooked yield, for the picked choices. */
fun Food.batch(c: Choices): Pair<Map<String, Double>, Double> {
    val r = resolve(c)
    val ing = ing.toMutableMap()
    var yieldG = yieldG
    for (g in choices) {
        val picked = g.options.filter { it.id in r[g.id].orEmpty() }
        if (g.multi) {
            val total = g.total ?: 0.0
            if (picked.isNotEmpty()) {
                val share = total / picked.size
                picked.forEach { o -> o.ing.forEach { (k, w) -> ing[k] = (ing[k] ?: 0.0) + share * w } }
                yieldG += total * g.yieldf
            }
        } else {
            picked.forEach { o ->
                o.ing.forEach { (k, grams) -> ing[k] = (ing[k] ?: 0.0) + grams; yieldG += grams * g.yieldf }
            }
        }
    }
    val oil = OIL_FACTOR[r[OIL]?.firstOrNull() ?: "usual"] ?: 1.0
    if (oil != 1.0) {
        for ((k, grams) in ing.toMap()) {
            if (FoodDb.ingredient(k)?.isFat == true) {
                val delta = grams * (oil - 1.0)
                ing[k] = grams + delta
                yieldG += delta
            }
        }
    }
    return ing to yieldG
}

/** Nutrients per 100 g for the picked choices. */
fun Food.per100(c: Choices = emptyMap()): Nutr = FoodDb.per100(this, c)

fun Food.nutrFor(qty: Double, unitName: String, c: Choices = emptyMap()): Nutr = per100(c) * (qty * unit(unitName).grams / 100.0)
