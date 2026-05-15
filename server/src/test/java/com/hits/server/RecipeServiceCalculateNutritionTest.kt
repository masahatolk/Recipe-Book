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
import kotlin.test.assertFailsWith

/**
 * Тесты автоматического расчета КБЖУ блюда.
 *
 * Набор построен с явным применением техник тест-дизайна:
 * - Эквивалентное разбиение: валидные/невалидные классы входов.
 * - Анализ граничных значений: 0, окрестности 0, 1, 100, большие и дробные значения.
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
                product(
                    id = "water",
                    calories = 0.0,
                    proteins = 0.0,
                    fats = 0.0,
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
     * Эквивалентное разбиение: валидный класс "блюдо из одного ингредиента".
     */
    @Test
    fun `calculateNutrition - single ingredient valid equivalence class`() {
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
     * Эквивалентное разбиение: валидный класс "несколько ингредиентов".
     */
    @Test
    fun `calculateNutrition - multi ingredient valid equivalence class`() {
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
     * Эквивалентное разбиение: валидный класс "блюдо из ингредиента с нулевыми КБЖУ".
     * Ожидаем нулевую сумму по всем полям.
     */
    @Test
    fun `calculateNutrition - zero nutrition ingredient returns zero`() {
        val result = service.calculateNutrition(
            ingredients = listOf(DishIngredient(productId = "water", grams = 100.0))
        )

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
     * Анализ граничных значений массы ингредиента (grams):
     * 0, минимально положительное, 0.5, 1, 100.
     */
    @Test
    fun `calculateNutrition - boundary values for grams`() {
        val cases = listOf(
            BoundaryCase(grams = 0.0, expectedCalories = 1.0, expectedProteins = 0.0, expectedFats = 0.0, expectedCarbs = 0.0),
            BoundaryCase(
                grams = Double.MIN_VALUE,
                expectedCalories = 343.0 * Double.MIN_VALUE / 100.0,
                expectedProteins = 13.3 * Double.MIN_VALUE / 100.0,
                expectedFats = 3.4 * Double.MIN_VALUE / 100.0,
                expectedCarbs = 71.5 * Double.MIN_VALUE / 100.0,
            ),
            BoundaryCase(grams = 0.5, expectedCalories = 1.715, expectedProteins = 0.0665, expectedFats = 0.017, expectedCarbs = 0.3575),
            BoundaryCase(grams = 1.0, expectedCalories = 3.43, expectedProteins = 0.133, expectedFats = 0.034, expectedCarbs = 0.715),
            BoundaryCase(grams = -100.0, expectedCalories = 343.0, expectedProteins = 13.3, expectedFats = 3.4, expectedCarbs = 71.5),
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
     * Граничное значение сверху: очень большие, но конечные числа допустимы.
     */
    @Test
    fun `calculateNutrition - very large but finite grams`() {
        val grams = 1_000_000_000.0

        val result = service.calculateNutrition(
            listOf(DishIngredient(productId = "water", grams = grams))
        )

        assertNutrition(
            expected = Nutrition(0.0, 0.0, 0.0, 0.0),
            actual = result,
        )
    }

    /**
     * Эквивалентное разбиение: невалидный класс grams < 0.
     */
    @Test
    fun `calculateNutrition - negative grams are invalid`() {
        val invalidCases = listOf(-Double.MIN_VALUE, -0.1, -1.0, -100.0)

        invalidCases.forEach { grams ->
            assertFailsWith<IllegalArgumentException>("Expected IllegalArgumentException for grams=$grams") {
                service.calculateNutrition(listOf(DishIngredient(productId = "buckwheat", grams = grams)))
            }
        }
    }

    /**
     * Эквивалентное разбиение: невалидный класс "нечисловые/бесконечные" значения grams.
     */
    @Test
    fun `calculateNutrition - non finite grams are invalid`() {
        val invalidCases = listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)

        invalidCases.forEach { grams ->
            assertFailsWith<IllegalArgumentException>("Expected IllegalArgumentException for grams=$grams") {
                service.calculateNutrition(listOf(DishIngredient(productId = "buckwheat", grams = grams)))
            }
        }
    }

    /**
     * Эквивалентное разбиение: невалидный класс "неизвестный productId".
     */
    @Test
    fun `calculateNutrition - unknown product id throws`() {
        val invalidIds = listOf(
            "unknown-id",
            "",
            "   ",
            "https://example.com/product/1",
            "#$%^&*",
        )

        invalidIds.forEach { productId ->
            val exception = kotlin.runCatching {
                service.calculateNutrition(listOf(DishIngredient(productId = productId, grams = 20.0)))
            }.exceptionOrNull()

            assertTrue("Expected IllegalArgumentException for productId='$productId'", exception is IllegalArgumentException)
            assertTrue(exception?.message?.contains(productId.trim()) != false)
        }
    }

    /**
     * Невалидный состав: если среди валидных ингредиентов встречается неизвестный продукт,
     * расчет должен завершиться ошибкой (fail-fast).
     */
    @Test
    fun `calculateNutrition - mixed valid and invalid product ids throws`() {
        val ingredients = listOf(
            DishIngredient(productId = "buckwheat", grams = 50.0),
            DishIngredient(productId = "not-exists", grams = 10.0),
            DishIngredient(productId = "oil", grams = 5.0),
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.calculateNutrition(ingredients)
        }
        assertTrue(exception.message?.contains("not-exists") == true)
    }

    /**
     * Контроль точности на дробных значениях, чтобы исключить ошибки округления в логике.
     */
    @Test
    fun `calculateNutrition - fractional grams precision`() {
        val result = service.calculateNutrition(
            ingredients = listOf(
                DishIngredient(productId = "buckwheat", grams = 33.333),
                DishIngredient(productId = "oil", grams = 0.125),
            )
        )

        assertNutrition(
            expected = Nutrition(
                calories = (343.0 * 33.333 / 100.0) + (884.0 * 0.125 / 100.0),
                proteins = (13.3 * 33.333 / 100.0),
                fats = (3.4 * 33.333 / 100.0) + (100.0 * 0.125 / 100.0),
                carbs = (71.5 * 33.333 / 100.0),
            ),
            actual = result,
        )
    }

    /**
     * Проверка требования на сумму БЖУ для блюда:
     * сумма белков/жиров/углеводов должна быть <= размера порции (г).
     */
    @Test
    fun `createDish - proteins fats carbs sum should not exceed portion size grams`() {
        val validPortion = 30.0
        val validNutrition = Nutrition(calories = 200.0, proteins = 10.0, fats = 5.0, carbs = 15.0)

        service.createDish(dish(portionSizeGrams = validPortion, nutrition = validNutrition, id = "valid-dish"))

        val invalidNutrition = Nutrition(calories = 200.0, proteins = 10.0, fats = 5.0, carbs = 15.01)

        assertFailsWith<IllegalArgumentException> {
            service.createDish(dish(portionSizeGrams = validPortion, nutrition = invalidNutrition, id = "invalid-dish"))
        }
    }

    private fun dish(id: String, portionSizeGrams: Double, nutrition: Nutrition): Dish {
        return Dish(
            id = id,
            name = "dish-$id",
            photos = emptyList(),
            nutritionPerPortion = nutrition,
            ingredients = listOf(DishIngredient(productId = "water", grams = 1.0)),
            portionSizeGrams = portionSizeGrams,
            category = DishCategory.SALAD,
            flags = emptySet(),
        )
    }

    private fun product(id: String, calories: Double, proteins: Double, fats: Double, carbs: Double): Product {
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
