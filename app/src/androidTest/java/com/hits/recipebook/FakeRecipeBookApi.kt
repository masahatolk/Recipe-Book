package com.hits.recipebook

import com.hits.recipebook.network.DishCalculationResponse
import com.hits.recipebook.network.DishUpsertRequest
import com.hits.recipebook.network.PhotoUploadResponse
import com.hits.recipebook.network.ProductUpsertRequest
import com.hits.recipebook.network.RecipeBookApi
import okhttp3.MultipartBody
import java.time.Instant

class FakeRecipeBookApi : RecipeBookApi {
    private val products = mutableListOf(
        Product(
            id = "product-avocado",
            name = "Авокадо UI",
            nutritionPer100g = Nutrition(calories = 160.0, proteins = 2.0, fats = 15.0, carbs = 9.0),
            composition = "Авокадо",
            category = ProductCategory.VEGETABLES,
            cookingRequirement = CookingRequirement.READY_TO_EAT,
            flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
            createdAt = CREATED_AT,
        ),
        Product(
            id = "product-banana",
            name = "Банан UI",
            nutritionPer100g = Nutrition(calories = 90.0, proteins = 1.0, fats = 0.3, carbs = 23.0),
            composition = "Банан",
            category = ProductCategory.SWEETS,
            cookingRequirement = CookingRequirement.READY_TO_EAT,
            flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
            createdAt = CREATED_AT,
        ),
        Product(
            id = "product-chicken",
            name = "Курица UI",
            nutritionPer100g = Nutrition(calories = 200.0, proteins = 23.0, fats = 10.0, carbs = 0.0),
            composition = "Курица",
            category = ProductCategory.MEAT,
            cookingRequirement = CookingRequirement.REQUIRES_COOKING,
            flags = setOf(ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
            createdAt = CREATED_AT,
        ),
    )

    private val dishes = mutableListOf(
        Dish(
            id = "dish-salad",
            name = "Салат UI",
            nutritionPerPortion = Nutrition(calories = 80.0, proteins = 1.0, fats = 7.5, carbs = 4.5),
            ingredients = listOf(DishIngredient("product-avocado", 50.0)),
            portionSizeGrams = 100.0,
            category = DishCategory.SALAD,
            flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
            createdAt = CREATED_AT,
        ),
        Dish(
            id = "dish-soup",
            name = "Суп UI",
            nutritionPerPortion = Nutrition(calories = 120.0, proteins = 12.0, fats = 5.0, carbs = 7.0),
            ingredients = listOf(DishIngredient("product-chicken", 60.0)),
            portionSizeGrams = 250.0,
            category = DishCategory.SOUP,
            flags = setOf(ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
            createdAt = CREATED_AT,
        ),
    )

    override suspend fun getProducts(
        category: ProductCategory?,
        cookingRequirement: CookingRequirement?,
        flags: MutableList<ExtraFlag>?,
        query: String?,
        sortBy: String?,
        direction: String?,
    ): List<Product> = products.toList()

    override suspend fun createProduct(request: ProductUpsertRequest): Product {
        return request.toProduct(id = "product-${products.size + 1}").also { products += it }
    }

    override suspend fun updateProduct(id: String, request: ProductUpsertRequest): Product {
        val updated = request.toProduct(id = id, updatedAt = Instant.now().toString())
        products[products.indexOfFirst { it.id == id }] = updated
        return updated
    }

    override suspend fun deleteProduct(id: String) {
        products.removeAll { it.id == id }
    }

    override suspend fun getDishes(
        category: DishCategory?,
        flags: List<ExtraFlag>,
        query: String?,
    ): List<Dish> = dishes.toList()

    override suspend fun createDish(request: DishUpsertRequest): Dish {
        return request.toDish(id = "dish-${dishes.size + 1}").also { dishes += it }
    }

    override suspend fun updateDish(id: String, request: DishUpsertRequest): Dish {
        val updated = request.toDish(id = id, updatedAt = Instant.now().toString())
        dishes[dishes.indexOfFirst { it.id == id }] = updated
        return updated
    }

    override suspend fun deleteDish(id: String) {
        dishes.removeAll { it.id == id }
    }

    override suspend fun calculateDish(ingredients: List<DishIngredient>): DishCalculationResponse {
        val productsById = products.associateBy { it.id }
        return DishCalculationResponse(
            nutrition = calculateNutrition(ingredients, productsById),
            availableFlags = allowedDishFlags(ingredients, productsById),
        )
    }

    override suspend fun uploadPhoto(photo: MultipartBody.Part): PhotoUploadResponse =
        PhotoUploadResponse(url = "https://example.test/photo.jpg")

    private fun ProductUpsertRequest.toProduct(id: String, updatedAt: String? = null): Product = Product(
        id = id,
        name = name,
        photos = photos,
        nutritionPer100g = nutritionPer100g,
        composition = composition,
        category = category,
        cookingRequirement = cookingRequirement,
        flags = flags,
        createdAt = CREATED_AT,
        updatedAt = updatedAt,
    )

    private fun DishUpsertRequest.toDish(id: String, updatedAt: String? = null): Dish = Dish(
        id = id,
        name = name,
        photos = photos,
        nutritionPerPortion = nutritionPerPortion,
        ingredients = ingredients,
        portionSizeGrams = portionSizeGrams,
        category = requireNotNull(category),
        flags = flags,
        createdAt = CREATED_AT,
        updatedAt = updatedAt,
    )

    private companion object {
        const val CREATED_AT = "2024-01-01T00:00:00Z"
    }
}