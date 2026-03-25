package com.hits.recipebook.server

import java.time.Instant

class RecipeService(private val repository: RecipeRepository) {

    fun listProducts(filter: ProductFilter): List<Product> {
        return repository.products()
            .filter { filter.category == null || it.category == filter.category }
            .filter { filter.cookingRequirement == null || it.cookingRequirement == filter.cookingRequirement }
            .filter { it.flags.containsAll(filter.flags) }
            .filter { filter.query.isNullOrBlank() || it.name.contains(filter.query, ignoreCase = true) }
            .sortedWith(productComparator(filter.sortBy))
    }

    fun createProduct(product: Product): Product {
        validateProduct(product)
        val items = repository.products()
        items += product
        repository.saveProducts(items)
        return product
    }

    fun updateProduct(id: String, product: Product): Product {
        validateProduct(product)
        val items = repository.products()
        val index = items.indexOfFirst { it.id == id }
        require(index >= 0) { "Product not found" }
        val updated = product.copy(id = id, createdAt = items[index].createdAt, updatedAt = Instant.now().toString())
        items[index] = updated
        repository.saveProducts(items)
        return updated
    }

    fun deleteProduct(id: String) {
        val dishesWithProduct = repository.dishes().filter { d -> d.ingredients.any { it.productId == id } }
        require(dishesWithProduct.isEmpty()) {
            throw ProductDeletionBlockedException(dishesWithProduct.map { it.id })
        }
        val products = repository.products().filterNot { it.id == id }
        repository.saveProducts(products)
    }

    fun product(id: String): Product = repository.products().first { it.id == id }

    fun listDishes(filter: DishFilter): List<Dish> {
        return repository.dishes()
            .filter { filter.category == null || it.category == filter.category }
            .filter { it.flags.containsAll(filter.flags) }
            .filter { filter.query.isNullOrBlank() || it.name.contains(filter.query, ignoreCase = true) }
    }

    fun createDish(dish: Dish): Dish {
        validateDish(dish)
        val normalizedDish = applyDishAutofill(dish)
        val dishes = repository.dishes()
        dishes += normalizedDish
        repository.saveDishes(dishes)
        return normalizedDish
    }

    fun updateDish(id: String, dish: Dish): Dish {
        validateDish(dish)
        val items = repository.dishes()
        val index = items.indexOfFirst { it.id == id }
        require(index >= 0) { "Dish not found" }
        val normalized = applyDishAutofill(dish).copy(id = id, createdAt = items[index].createdAt, updatedAt = Instant.now().toString())
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
        val productsById = repository.products().associateBy { it.id }
        fun calc(selector: (Nutrition) -> Double): Double = ingredients.sumOf { ingredient ->
            val product = requireNotNull(productsById[ingredient.productId]) { "Product ${ingredient.productId} not found" }
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
        val words = name.split(" ").toMutableList()
        val firstMacroIndex = words.indexOfFirst { macros.containsKey(it.lowercase()) }
        if (firstMacroIndex == -1) return name to null
        val category = macros[words[firstMacroIndex].lowercase()]
        words.removeAt(firstMacroIndex)
        return words.joinToString(" ").trim() to category
    }

    private fun applyDishAutofill(dish: Dish): Dish {
        val (titleWithoutMacro, macroCategory) = resolveMacroCategory(dish.name)
        val nutritionDraft = calculateNutrition(dish.ingredients)
        val allowedFlags = availableFlags(dish.ingredients)
        return dish.copy(
            name = titleWithoutMacro,
            nutritionPerPortion = nutritionDraft,
            flags = dish.flags.intersect(allowedFlags),
            category = dish.category.takeIf { it in DishCategory.entries } ?: macroCategory ?: DishCategory.SNACK,
        )
    }

    private fun validateProduct(product: Product) {
        require(product.name.length >= 2) { "Product name min length is 2" }
        require(product.photos.size <= 5) { "Max 5 photos" }
        require(product.nutritionPer100g.calories >= 0 && product.nutritionPer100g.proteins >= 0 && product.nutritionPer100g.fats >= 0 && product.nutritionPer100g.carbs >= 0)
    }

    private fun validateDish(dish: Dish) {
        require(dish.name.length >= 2) { "Dish name min length is 2" }
        require(dish.photos.size <= 5) { "Max 5 photos" }
        require(dish.portionSizeGrams > 0) { "Portion size must be positive" }
        require(dish.ingredients.isNotEmpty()) { "Dish should contain at least one ingredient" }
        require(dish.ingredients.all { it.grams > 0 }) { "Ingredient grams must be positive" }
    }

    private fun productComparator(sortBy: String): Comparator<Product> = when (sortBy.lowercase()) {
        "calories" -> compareBy { it.nutritionPer100g.calories }
        "proteins" -> compareBy { it.nutritionPer100g.proteins }
        "fats" -> compareBy { it.nutritionPer100g.fats }
        "carbs" -> compareBy { it.nutritionPer100g.carbs }
        else -> compareBy { it.name.lowercase() }
    }
}

class ProductDeletionBlockedException(val dishIds: List<String>) : RuntimeException("Product is used in dishes")
