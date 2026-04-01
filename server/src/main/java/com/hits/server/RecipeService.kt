package com.hits.recipebook.server

import com.hits.server.Dish
import com.hits.server.DishCategory
import com.hits.server.DishFilter
import com.hits.server.DishIngredient
import com.hits.server.ExtraFlag
import com.hits.server.Nutrition
import com.hits.server.Product
import com.hits.server.ProductFilter
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RecipeService(private val repository: RecipeRepository) {
    private val tomskZoneId = ZoneId.of("Asia/Tomsk")

    private fun nowTomsk(): String =
        OffsetDateTime.now(tomskZoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    fun listProducts(filter: ProductFilter): List<Product> {
        return repository.products()
            .filter { filter.category == null || it.category == filter.category }
            .filter { filter.cookingRequirement == null || it.cookingRequirement == filter.cookingRequirement }
            .filter { it.flags.containsAll(filter.flags) }
            .filter {
                filter.query.isNullOrBlank() || it.name.contains(
                    filter.query,
                    ignoreCase = true
                )
            }
            .sortedWith(productComparator(filter.sortBy))
    }

    fun createProduct(product: Product): Product {
        validateProduct(product)
        val items = repository.products()
        val created = product.copy(createdAt = nowTomsk(), updatedAt = null)
        items += created
        repository.saveProducts(items)
        return created
    }

    fun updateProduct(id: String, product: Product): Product {
        validateProduct(product)
        val items = repository.products()
        val index = items.indexOfFirst { it.id == id }
        require(index >= 0) { "Product not found" }
        val updated =
            product.copy(id = id, createdAt = items[index].createdAt, updatedAt = nowTomsk())
        items[index] = updated
        repository.saveProducts(items)
        return updated
    }

    fun deleteProduct(id: String) {
        val dishesWithProduct =
            repository.dishes().filter { d -> d.ingredients.any { it.productId == id } }
        require(dishesWithProduct.isEmpty()) {
            throw ProductDeletionBlockedException(dishesWithProduct.map { it.name })
        }
        val products = repository.products().filterNot { it.id == id }
        repository.saveProducts(products)
    }

    fun product(id: String): Product = repository.products().first { it.id == id }

    fun listDishes(filter: DishFilter): List<Dish> {
        return repository.dishes()
            .filter { filter.category == null || it.category == filter.category }
            .filter { it.flags.containsAll(filter.flags) }
            .filter {
                filter.query.isNullOrBlank() || it.name.contains(
                    filter.query,
                    ignoreCase = true
                )
            }
    }

    fun createDish(dish: Dish): Dish {
        val normalizedDish = applyDishAutofill(dish)
        validateDish(normalizedDish)
        val createdDish = normalizedDish.copy(createdAt = nowTomsk(), updatedAt = null)
        val dishes = repository.dishes()
        dishes += createdDish
        repository.saveDishes(dishes)
        return createdDish
    }

    fun updateDish(id: String, dish: Dish): Dish {
        val normalizedDish = applyDishAutofill(dish)
        validateDish(normalizedDish)
        val items = repository.dishes()
        val index = items.indexOfFirst { it.id == id }
        require(index >= 0) { "Dish not found" }
        val normalized = normalizedDish.copy(
            id = id,
            createdAt = items[index].createdAt,
            updatedAt = nowTomsk()
        )
        items[index] = normalized
        repository.saveDishes(items)
        return normalized
    }

    fun deleteDish(id: String) {
        val dishes = repository.dishes().filterNot { it.id == id }
        repository.saveDishes(dishes)
    }

    fun dish(id: String): Dish = repository.dishes().first { it.id == id }

    fun calculateNutrition(ingredients: List<DishIngredient>): Nutrition {
        ingredients.forEach { ingredient ->
            require(ingredient.grams.isFinite()) { "Ingredient grams must be finite numbers" }
            require(ingredient.grams >= 0.0) { "Ingredient grams must be >= 0" }
        }
        val productsById = repository.products().associateBy { it.id }
        fun calc(selector: (Nutrition) -> Double): Double = ingredients.sumOf { ingredient ->
            val product =
                requireNotNull(productsById[ingredient.productId]) { "Product ${ingredient.productId} not found" }
            selector(product.nutritionPer100g) * ingredient.grams / 100.0
        }
        return Nutrition(
            calories = calc { it.calories },
            proteins = calc { it.proteins },
            fats = calc { it.fats },
            carbs = calc { it.carbs }
        )
    }

    fun availableFlags(ingredients: List<DishIngredient>): Set<ExtraFlag> {
        val productsById = repository.products().associateBy { it.id }
        if (ingredients.isEmpty()) return emptySet()
        return ExtraFlag.entries.filter { flag ->
            ingredients.all { ingredient ->
                productsById[ingredient.productId]?.flags?.contains(flag) == true
            }
        }.toSet()
    }

    fun resolveMacroCategory(name: String): Pair<String, DishCategory?> {
        val macros = mapOf(
            "!десерт" to DishCategory.DESSERT,
            "!первое" to DishCategory.FIRST_COURSE,
            "!второе" to DishCategory.SECOND_COURSE,
            "!напиток" to DishCategory.DRINK,
            "!салат" to DishCategory.SALAD,
            "!суп" to DishCategory.SOUP,
            "!перекус" to DishCategory.SNACK,
        )
        val normalizedName = name.trim()
        val lower = normalizedName.lowercase()
        val firstMacroMatch = macros.entries
            .mapNotNull { entry ->
                val index = lower.indexOf(entry.key)
                if (index >= 0) entry to index else null
            }
            .minByOrNull { it.second }
            ?: return normalizedName to null
        val macro = firstMacroMatch.first.key
        val category = firstMacroMatch.first.value
        val macroIndex = firstMacroMatch.second
        val cleaned = buildString {
            append(normalizedName.substring(0, macroIndex))
            append(normalizedName.substring(macroIndex + macro.length))
        }.replace(Regex("\\s+"), " ").trim()
        return cleaned to category
    }

    private fun applyDishAutofill(dish: Dish): Dish {
        val (titleWithoutMacro, macroCategory) = resolveMacroCategory(dish.name)
        val allowedFlags = availableFlags(dish.ingredients)
        return dish.copy(
            name = titleWithoutMacro.trim(),
            flags = dish.flags.intersect(allowedFlags),
            category = macroCategory ?: dish.category,
        )
    }

    private fun validateProduct(product: Product) {
        require(product.name.length >= 2) { "Product name min length is 2" }
        require(product.photos.size <= 5) { "Max 5 photos" }
        require(product.nutritionPer100g.calories >= 0) { "Calories must be >= 0" }
        require(product.nutritionPer100g.proteins in 0.0..100.0) { "Proteins must be in [0, 100]" }
        require(product.nutritionPer100g.fats in 0.0..100.0) { "Fats must be in [0, 100]" }
        require(product.nutritionPer100g.carbs in 0.0..100.0) { "Carbs must be in [0, 100]" }
        require(
            product.nutritionPer100g.proteins + product.nutritionPer100g.fats + product.nutritionPer100g.carbs <= 100.0
        ) { "Proteins + fats + carbs must be <= 100" }
    }

    private fun validateDish(dish: Dish) {
        require(dish.name.trim().length >= 2) { "Dish name min length is 2" }
        require(dish.photos.size <= 5) { "Max 5 photos" }
        require(dish.portionSizeGrams > 0) { "Portion size must be positive" }
        require(dish.ingredients.isNotEmpty()) { "Dish should contain at least one ingredient" }
        require(dish.ingredients.all { it.grams > 0 }) { "Ingredient grams must be positive" }
        require(dish.nutritionPerPortion.calories >= 0) { "Calories must be >= 0" }
        require(dish.nutritionPerPortion.proteins >= 0) { "Proteins must be >= 0" }
        require(dish.nutritionPerPortion.fats >= 0) { "Fats must be >= 0" }
        require(dish.nutritionPerPortion.carbs >= 0) { "Carbs must be >= 0" }
        require(
            dish.nutritionPerPortion.proteins + dish.nutritionPerPortion.fats + dish.nutritionPerPortion.carbs <= dish.portionSizeGrams
        ) { "Proteins + fats + carbs per portion must be <= portion size grams" }
    }

    private fun productComparator(sortBy: String): Comparator<Product> = when (sortBy.lowercase()) {
        "calories" -> compareBy { it.nutritionPer100g.calories }
        "proteins" -> compareBy { it.nutritionPer100g.proteins }
        "fats" -> compareBy { it.nutritionPer100g.fats }
        "carbs" -> compareBy { it.nutritionPer100g.carbs }
        else -> compareBy { it.name.lowercase() }
    }
}

class ProductDeletionBlockedException(val dishNames: List<String>) :
    RuntimeException("Product is used in dishes")
