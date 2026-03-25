package com.hits.recipebook.server

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecipeServiceTest {

    private fun serviceWithProducts(): RecipeService {
        val repo = RecipeRepository(kotlin.io.path.createTempDirectory().toFile())
        val service = RecipeService(repo)
        service.createProduct(
            Product(
                id = "p1",
                name = "Tofu",
                nutritionPer100g = Nutrition(80.0, 8.0, 4.0, 2.0),
                category = ProductCategory.MEAT,
                cookingRequirement = CookingRequirement.SEMI_FINISHED,
                flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE)
            )
        )
        service.createProduct(
            Product(
                id = "p2",
                name = "Tomato",
                nutritionPer100g = Nutrition(20.0, 1.0, 0.2, 3.0),
                category = ProductCategory.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE)
            )
        )
        return service
    }

    @Test
    fun `nutrition is calculated from ingredient weights`() {
        val service = serviceWithProducts()

        val nutrition = service.calculateNutrition(listOf(DishIngredient("p1", 50.0), DishIngredient("p2", 150.0)))

        assertEquals(70.0, nutrition.calories)
        assertEquals(5.5, nutrition.proteins)
        assertEquals(2.3, nutrition.fats)
        assertEquals(5.5, nutrition.carbs)
    }

    @Test
    fun `macro in title is extracted and category is inferred`() {
        val service = serviceWithProducts()

        val (name, category) = service.resolveMacroCategory("!суп Томатный крем")

        assertEquals("Томатный крем", name)
        assertEquals(DishCategory.SOUP, category)
    }

    @Test
    fun `deleting product used in dish throws domain exception`() {
        val service = serviceWithProducts()
        service.createDish(
            Dish(
                id = "d1",
                name = "Салат",
                nutritionPerPortion = Nutrition(0.0, 0.0, 0.0, 0.0),
                ingredients = listOf(DishIngredient("p1", 100.0)),
                portionSizeGrams = 250.0,
                category = DishCategory.SALAD,
                flags = setOf(ExtraFlag.VEGAN)
            )
        )

        val exception = assertFailsWith<ProductDeletionBlockedException> { service.deleteProduct("p1") }

        assertTrue(exception.dishIds.contains("d1"))
    }
}
