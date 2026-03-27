package com.hits.recipebook

import java.time.Instant
import java.util.UUID

enum class ExtraFlag(val label: String) {
    VEGAN("Веган"),
    GLUTEN_FREE("Без глютена"),
    SUGAR_FREE("Без сахара")
}

enum class ProductCategory(val label: String) {
    FROZEN("Замороженный"),
    MEAT("Мясной"),
    VEGETABLES("Овощи"),
    HERBS("Зелень"),
    SPICES("Специи"),
    GRAINS("Крупы"),
    CANNED("Консервы"),
    LIQUID("Жидкость"),
    SWEETS("Сладости")
}

enum class CookingRequirement(val label: String) {
    READY_TO_EAT("Готовый к употреблению"),
    SEMI_FINISHED("Полуфабрикат"),
    REQUIRES_COOKING("Требует приготовления")
}

enum class DishCategory(val label: String) {
    DESSERT("Десерт"),
    FIRST_COURSE("Первое"),
    SECOND_COURSE("Второе"),
    DRINK("Напиток"),
    SALAD("Салат"),
    SOUP("Суп"),
    SNACK("Перекус")
}

data class Nutrition(
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
)

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val photos: List<String> = emptyList(),
    val nutritionPer100g: Nutrition,
    val composition: String? = null,
    val category: ProductCategory,
    val cookingRequirement: CookingRequirement,
    val flags: Set<ExtraFlag> = emptySet(),
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String? = null,
)

data class DishIngredient(
    val productId: String,
    val grams: Double,
)

data class Dish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val photos: List<String> = emptyList(),
    val nutritionPerPortion: Nutrition,
    val ingredients: List<DishIngredient>,
    val portionSizeGrams: Double,
    val category: DishCategory,
    val flags: Set<ExtraFlag> = emptySet(),
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String? = null,
)

enum class ProductSort(val label: String) {
    NAME("По названию"),
    CALORIES("По калорийности"),
    PROTEINS("По белкам"),
    FATS("По жирам"),
    CARBS("По углеводам")
}

private val macroMap = mapOf(
    "!десерт" to DishCategory.DESSERT,
    "!первое" to DishCategory.FIRST_COURSE,
    "!второе" to DishCategory.SECOND_COURSE,
    "!напиток" to DishCategory.DRINK,
    "!салат" to DishCategory.SALAD,
    "!суп" to DishCategory.SOUP,
    "!перекус" to DishCategory.SNACK,
)

fun resolveDishNameAndMacroCategory(name: String): Pair<String, DishCategory?> {
    val normalizedName = name.trim()
    val lower = normalizedName.lowercase()
    val firstMacroMatch = macroMap.entries
        .mapNotNull { entry ->
            val index = lower.indexOf(entry.key)
            if (index >= 0) entry to index else null
        }
        .minByOrNull { it.second }
        ?: return normalizedName to null

    val macro = firstMacroMatch.first.key
    val macroCategory = firstMacroMatch.first.value
    val macroIndex = firstMacroMatch.second
    val cleaned = buildString {
        append(normalizedName.substring(0, macroIndex))
        append(normalizedName.substring(macroIndex + macro.length))
    }.replace(Regex("\\s+"), " ").trim()
    return cleaned to macroCategory
}

fun calculateNutrition(ingredients: List<DishIngredient>, productsById: Map<String, Product>): Nutrition {
    fun calc(selector: (Nutrition) -> Double): Double = ingredients.sumOf {
        val product = productsById[it.productId] ?: return@sumOf 0.0
        selector(product.nutritionPer100g) * it.grams / 100.0
    }
    return Nutrition(
        calories = calc { it.calories },
        proteins = calc { it.proteins },
        fats = calc { it.fats },
        carbs = calc { it.carbs },
    )
}

fun allowedDishFlags(ingredients: List<DishIngredient>, productsById: Map<String, Product>): Set<ExtraFlag> {
    if (ingredients.isEmpty()) return emptySet()
    return ExtraFlag.entries.filter { flag ->
        ingredients.all { ingredient ->
            productsById[ingredient.productId]?.flags?.contains(flag) == true
        }
    }.toSet()
}
