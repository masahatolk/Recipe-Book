package com.hits.recipebook.server

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class ExtraFlag { VEGAN, GLUTEN_FREE, SUGAR_FREE }

@Serializable
enum class ProductCategory { FROZEN, MEAT, VEGETABLES, HERBS, SPICES, GRAINS, CANNED, LIQUID, SWEETS }

@Serializable
enum class CookingRequirement { READY_TO_EAT, SEMI_FINISHED, REQUIRES_COOKING }

@Serializable
enum class DishCategory { DESSERT, FIRST_COURSE, SECOND_COURSE, DRINK, SALAD, SOUP, SNACK }

@Serializable
data class Nutrition(
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
)

@Serializable
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

@Serializable
data class DishIngredient(
    val productId: String,
    val grams: Double,
)

@Serializable
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

@Serializable
data class ProductFilter(
    val category: ProductCategory? = null,
    val cookingRequirement: CookingRequirement? = null,
    val flags: Set<ExtraFlag> = emptySet(),
    val query: String? = null,
    val sortBy: String = "name",
)

@Serializable
data class DishFilter(
    val category: DishCategory? = null,
    val flags: Set<ExtraFlag> = emptySet(),
    val query: String? = null,
)

@Serializable
data class DeletionBlockedResponse(
    val message: String,
    val dishIds: List<String>,
)
