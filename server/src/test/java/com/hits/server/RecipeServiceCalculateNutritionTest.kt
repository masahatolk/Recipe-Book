package com.hits.server

import com.hits.recipebook.server.RecipeRepository
import com.hits.recipebook.server.RecipeService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Тесты автоматического расчета КБЖУ блюда.
 *
 * Покрытие данных построено двумя техниками тест-дизайна:
 * 1) Эквивалентное разбиение (валидные и невалидные классы входов).
 * 2) Анализ граничных значений (0 г, 1 г, 100 г, дробные граммы).
 */
class RecipeServiceCalculateNutritionTest {
    private lateinit var dataDir: File
    private lateinit var repository: RecipeRepository
    private lateinit var service: RecipeService

    @Before
    fun setUp() {
        dataDir = Files.createTempDirectory("recipe-service-test").toFile()
        repository = RecipeRepository(dataDir)
        service = RecipeService(repository)

        repository.saveProducts(
            listOf(
                product(
                    id = "buckwheat",
                    calories = 343.0,
                    proteins = 13.3,
                    fats = 3.4,
                    carbs = 71.5,
                ),
                product(
                    id = "chicken",
                    calories = 165.0,
                    proteins = 31.0,
                    fats = 3.6,
                    carbs = 0.0,
                ),
                product(
                    id = "oil",
                    calories = 884.0,
                    proteins = 0.0,
                    fats = 100.0,
                    carbs = 0.0,
                ),
            )
        )
    }

    @After
    fun tearDown() {
        dataDir.deleteRecursively()
    }

    /**
     * Эквивалентное разбиение: валидный класс "однотипный состав из одного продукта".
     * Проверяем, что для одного ингредиента применяется формула valuePer100g * grams / 100.
     */
    @Test
    fun `calculateNutrition - equivalence class single ingredient`() {
        val result = service.calculateNutrition(
            ingredients = listOf(DishIngredient(productId = "buckwheat", grams = 150.0))
        )

        assertNutrition(
            expected = Nutrition(
                calories = 514.5,
                proteins = 19.95,
                fats = 5.1,
                carbs = 107.25,
            ),
            actual = result,
        )
    }

    /**
     * Эквивалентное разбиение: валидный класс "смешанный состав из нескольких продуктов".
     * Проверяем суммирование вкладов каждого ингредиента.
     */
    @Test
    fun `calculateNutrition - equivalence class multiple ingredients`() {
        val result = service.calculateNutrition(
            ingredients = listOf(
                DishIngredient(productId = "buckwheat", grams = 80.0),
                DishIngredient(productId = "chicken", grams = 120.0),
                DishIngredient(productId = "oil", grams = 10.0),
            )
        )

        assertNutrition(
            expected = Nutrition(
                calories = 560.8,
                proteins = 47.84,
                fats = 17.04,
                carbs = 57.2,
            ),
            actual = result,
        )
    }

    /**
     * Эквивалентное разбиение: невалидный класс "ингредиент с несуществующим productId".
     * Ожидаем информативное исключение.
     */
    @Test
    fun `calculateNutrition - equivalence class unknown product id throws`() {
        val exception = kotlin.runCatching {
            service.calculateNutrition(
                ingredients = listOf(DishIngredient(productId = "unknown-id", grams = 20.0))
            )
        }.exceptionOrNull()

        assertTrue("Expected IllegalArgumentException", exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("unknown-id") == true)
    }

    /**
     * Эквивалентное разбиение: валидный класс "пустой состав".
     * Для пустого списка ингредиентов сумма по формуле должна быть нулевой.
     */
    @Test
    fun `calculateNutrition - equivalence class empty ingredients returns zero nutrition`() {
        val result = service.calculateNutrition(emptyList())

        assertNutrition(
            expected = Nutrition(
                calories = 0.0,
                proteins = 0.0,
                fats = 0.0,
                carbs = 0.0,
            ),
            actual = result,
        )
    }

    /**
     * Анализ граничных значений для количества ингредиента (grams).
     * Границы и окрестности: 0 г, 1 г, 100 г и дробное значение 0.5 г.
     */
    @Test
    fun `calculateNutrition - boundary values for grams`() {
        val cases = listOf(
            BoundaryCase(
                grams = 0.0,
                expectedCalories = 0.0,
                expectedProteins = 0.0,
                expectedFats = 0.0,
                expectedCarbs = 0.0,
            ),
            BoundaryCase(
                grams = 0.5,
                expectedCalories = 1.715,
                expectedProteins = 0.0665,
                expectedFats = 0.017,
                expectedCarbs = 0.3575,
            ),
            BoundaryCase(
                grams = 1.0,
                expectedCalories = 3.43,
                expectedProteins = 0.133,
                expectedFats = 0.034,
                expectedCarbs = 0.715,
            ),
            BoundaryCase(
                grams = 100.0,
                expectedCalories = 343.0,
                expectedProteins = 13.3,
                expectedFats = 3.4,
                expectedCarbs = 71.5,
            ),
        )

        cases.forEach { case ->
            val result = service.calculateNutrition(
                ingredients = listOf(DishIngredient(productId = "buckwheat", grams = case.grams))
            )
            assertNutrition(
                expected = Nutrition(
                    calories = case.expectedCalories,
                    proteins = case.expectedProteins,
                    fats = case.expectedFats,
                    carbs = case.expectedCarbs,
                ),
                actual = result,
            )
        }
    }

    /**
     * Эквивалентное разбиение: валидный класс "одинаковый состав в разном порядке".
     * Проверяем инвариантность суммы КБЖУ к перестановке ингредиентов.
     */
    @Test
    fun `calculateNutrition - ingredient order does not affect result`() {
        val firstOrder = listOf(
            DishIngredient(productId = "buckwheat", grams = 80.0),
            DishIngredient(productId = "chicken", grams = 120.0),
            DishIngredient(productId = "oil", grams = 10.0),
        )
        val secondOrder = firstOrder.reversed()

        val firstResult = service.calculateNutrition(firstOrder)
        val secondResult = service.calculateNutrition(secondOrder)

        assertNutrition(expected = firstResult, actual = secondResult)
    }

    private fun product(
        id: String,
        calories: Double,
        proteins: Double,
        fats: Double,
        carbs: Double,
    ): Product {
        return Product(
            id = id,
            name = "product-$id",
            nutritionPer100g = Nutrition(calories, proteins, fats, carbs),
            category = ProductCategory.GRAINS,
            cookingRequirement = CookingRequirement.READY_TO_EAT,
        )
    }

    private fun assertNutrition(expected: Nutrition, actual: Nutrition) {
        assertEquals(expected.calories, actual.calories, EPS)
        assertEquals(expected.proteins, actual.proteins, EPS)
        assertEquals(expected.fats, actual.fats, EPS)
        assertEquals(expected.carbs, actual.carbs, EPS)
    }

    private data class BoundaryCase(
        val grams: Double,
        val expectedCalories: Double,
        val expectedProteins: Double,
        val expectedFats: Double,
        val expectedCarbs: Double,
    )

    private companion object {
        private const val EPS = 1e-9
    }
}
