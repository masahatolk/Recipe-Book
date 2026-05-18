package com.hits.recipebook

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.hits.recipebook.network.DishCalculationResponse
import com.hits.recipebook.network.DishUpsertRequest
import com.hits.recipebook.network.PhotoUploadResponse
import com.hits.recipebook.network.ProductUpsertRequest
import com.hits.recipebook.network.RecipeBookApi
import com.hits.recipebook.ui.theme.RecipeBookTheme
import okhttp3.MultipartBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * В тестах применяются:
 * - Эквивалентное разбиение: валидные/невалидные классы данных для строк и чисел.
 * - Анализ граничных значений: 0, >0, 100, >100, минимальная длина названия.
 */
class UiTests {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var fakeApi: FakeRecipeBookApi

    @Before
    fun setUp() {
        fakeApi = FakeRecipeBookApi()
        composeRule.setContent {
            RecipeBookTheme {
                RecipeBookApp(api = fakeApi)
            }
        }
        waitForProduct("Авокадо UI")
    }

    // ---------- Product tests ----------

    @Test
    fun product_nameTooShort_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Я", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Название продукта: минимум 2 символа").assertIsDisplayed()
    }

    @Test
    fun product_caloriesRequired_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Поле \"Калорийность\" не может быть пустым").assertIsDisplayed()
    }

    @Test
    fun product_caloriesMustBeNumber_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "abc", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Поле \"Калорийность\" должно быть числом").assertIsDisplayed()
    }

    @Test
    fun product_caloriesBelowZero_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "-0.1", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Калорийность должна быть >= 0").assertIsDisplayed()
    }

    @Test
    fun product_proteinsBelowZero_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "10", proteins = "-0.1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_proteinsAboveHundred_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "10", proteins = "100.1", fats = "0", carbs = "0")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_fatsAboveHundred_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Авокадо", calories = "10", proteins = "0", fats = "100.1", carbs = "0")
        save()
        composeRule.onNodeWithText("Жиры должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_carbsAboveHundred_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Фрукт", calories = "10", proteins = "0", fats = "0", carbs = "100.1")
        save()
        composeRule.onNodeWithText("Углеводы должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_bzhuSumAboveHundred_showsValidation() {
        openProductEditor()
        fillProductForm(name = "Сыр", calories = "200", proteins = "40", fats = "30", carbs = "30.1")
        save()
        composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100").assertIsDisplayed()
    }

    @Test
    fun product_bzhuSumEqualHundred_passesLocalValidationAndFailsOnBackend() {
        openProductEditor()
        fillProductForm(name = "Сыр", calories = "200", proteins = "40", fats = "30", carbs = "30")
        save()
        composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100").assertDoesNotExist()
    }

    @Test
    fun product_zeroBoundaryValues_areAcceptedLocally() {
        openProductEditor()
        fillProductForm(name = "Вода", calories = "0", proteins = "0", fats = "0", carbs = "0")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertDoesNotExist()
    }

    @Test
    fun product_viewEditDelete_flow() {
        searchProduct("Авокадо UI")

        composeRule.onAllNodesWithText("Просмотр").onFirst().performClick()
        composeRule.onNodeWithText("Просмотр продукта").assertIsDisplayed()
        composeRule.onNodeWithText("Название: Авокадо UI").assertIsDisplayed()
        composeRule.onNodeWithText("Категория: Овощи").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onAllNodesWithText("Редактировать").onFirst().performClick()
        composeRule.onNodeWithText("Редактирование продукта").assertIsDisplayed()
        inputField("Название*", "Авокадо UI обновлен")
        saveEdited()
        waitUntilTextDoesNotExist("Сохранить")
        searchProduct("Авокадо UI обновлен")
        waitForProduct("Авокадо UI обновлен")

        composeRule.onAllNodesWithText("Удалить").onFirst().performClick()
        waitUntilTextDoesNotExist("Авокадо UI обновлен")
        composeRule.onNodeWithText("Продукты не найдены").assertIsDisplayed()
    }

    @Test
    fun product_searchFilterAndSort_work() {
        searchProduct("Курица")
        composeRule.onNodeWithText("Курица UI").assertIsDisplayed()
        composeRule.onNodeWithText("Авокадо UI").assertDoesNotExist()

        searchProduct("")
        composeRule.onNodeWithText("Открыть фильтры и сортировку").performClick()
        composeRule.onNodeWithText("Мясной").performScrollTo().performClick()
        composeRule.onNodeWithText("Курица UI").assertIsDisplayed()
        composeRule.onNodeWithText("Авокадо UI").assertDoesNotExist()

        composeRule.onNodeWithText("Мясной").performScrollTo().performClick()
        composeRule.onNodeWithText("По калорийности").performScrollTo().performClick()
        composeRule.onNodeWithText("↑ По возрастанию").performScrollTo().performClick()
        composeRule.onNodeWithText("Скрыть фильтры и сортировку").performClick()
        assertTextIsAbove("Курица UI", "Авокадо UI")
    }

    // ---------- Dish tests ----------

    @Test
    fun dish_nameTooShort_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "Я", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Название блюда: минимум 2 символа").assertIsDisplayed()
    }

    @Test
    fun dish_withoutIngredients_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "Салат", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_portionRequired_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "Салат", portion = "", calories = "10", proteins = "1", fats = "1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Поле \\\"Размер порции\\\" не может быть пустым").assertIsDisplayed()
    }

    @Test
    fun dish_categoryRequiredWithoutMacro_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "Овощное блюдо", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Укажите категорию или добавьте макрос в названии").assertIsDisplayed()
    }

    @Test
    fun dish_caloriesMustBeNumber_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "abc", proteins = "1", fats = "1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Поле \\\"Калорийность\\\" должно быть числом").assertIsDisplayed()
    }

    @Test
    fun dish_proteinsBelowZero_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "-0.1", fats = "1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Белки должны быть >= 0").assertIsDisplayed()
    }

    @Test
    fun dish_fatsBelowZero_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "1", fats = "-0.1", carbs = "1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Жиры должны быть >= 0").assertIsDisplayed()
    }

    @Test
    fun dish_carbsBelowZero_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "-0.1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Углеводы должны быть >= 0").assertIsDisplayed()
    }

    @Test
    fun dish_bzhuSumAboveHundred_showsValidation() {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "200", proteins = "40", fats = "30", carbs = "30.1")
        selectFirstDishIngredient()
        save()
        composeRule.onNodeWithText("Сумма БЖУ на порцию не может превышать 100").assertIsDisplayed()
    }

    @Test
    fun dish_flagsSectionVisible_andHasThreeFlags() {
        openDishEditor()
        composeRule.onNodeWithText("Флаги блюда").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Веган").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Без глютена").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Без сахара").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dish_viewEditDelete_flow() {
        openDishesTab()
        searchDish("Салат UI")

        composeRule.onAllNodesWithText("Просмотр").onFirst().performClick()
        composeRule.onNodeWithText("Просмотр блюда").assertIsDisplayed()
        composeRule.onNodeWithText("Название: Салат UI").assertIsDisplayed()
        composeRule.onNodeWithText("Категория: Салат").assertIsDisplayed()
        composeRule.onNodeWithText("- Авокадо UI: 50.0 г").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onAllNodesWithText("Редактировать").onFirst().performClick()
        composeRule.onNodeWithText("Редактирование блюда").assertIsDisplayed()
        inputField("Название* (макросы: !десерт, !первое...)", "Салат UI обновлен")
        saveEdited()
        waitUntilTextDoesNotExist("Сохранить")
        searchDish("Салат UI обновлен")
        waitForDish("Салат UI обновлен")

        composeRule.onAllNodesWithText("Удалить").onFirst().performClick()
        waitUntilTextDoesNotExist("Салат UI обновлен")
        composeRule.onNodeWithText("Блюда не найдены").assertIsDisplayed()
    }

    @Test
    fun dish_searchAndFilter_work() {
        openDishesTab()
        searchDish("Суп")
        composeRule.onNodeWithText("Суп UI").assertIsDisplayed()
        composeRule.onNodeWithText("Салат UI").assertDoesNotExist()

        searchDish("")
        composeRule.onNodeWithText("Открыть фильтры").performClick()
        composeRule.onNodeWithText("Салат").performScrollTo().performClick()
        composeRule.onNodeWithText("Салат UI").assertIsDisplayed()
        composeRule.onNodeWithText("Суп UI").assertDoesNotExist()
    }


    @Test
    fun filters_canBeExpandedForProductsAndDishes() {
        composeRule.onNodeWithText("Открыть фильтры и сортировку").performClick()
        composeRule.onNodeWithText("Сортировка").performScrollTo().assertIsDisplayed()

        openDishesTab()
        composeRule.onNodeWithText("Открыть фильтры").performClick()
        composeRule.onNodeWithText("Категория").performScrollTo().assertIsDisplayed()
    }

    private fun openProductEditor() {
        composeRule.onNodeWithText("Создать продукт").performClick()
    }

    private fun openDishesTab() {
        composeRule.onNodeWithText("Блюда").performClick()
        waitForDish("Салат UI")
    }

    private fun openDishEditor() {
        openDishesTab()
        composeRule.onNodeWithText("Создать блюдо").performClick()
    }

    private fun save() {
        composeRule.onNodeWithText("Создать").performClick()
    }

    private fun saveEdited() {
        composeRule.onNodeWithText("Сохранить").performClick()
    }

    private fun searchProduct(value: String) {
        inputField("Поиск продукта", value)
    }

    private fun searchDish(value: String) {
        inputField("Поиск блюда", value)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun selectFirstDishIngredient() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("dish-ingredient-checkbox"), timeoutMillis = 5_000)
        composeRule.onAllNodesWithTag("dish-ingredient-checkbox").onFirst().performClick()
    }

    private fun fillProductForm(name: String, calories: String, proteins: String, fats: String, carbs: String) {
        inputField("Название*", name)
        inputField("Ккал", calories)
        inputField("Белки", proteins)
        inputField("Жиры", fats)
        inputField("Углев.", carbs)
    }

    private fun fillDishBaseForm(name: String, portion: String, calories: String, proteins: String, fats: String, carbs: String) {
        inputField("Название* (макросы: !десерт, !первое...)", name)
        inputField("Размер порции, г*", portion)
        inputField("Ккал", calories)
        inputField("Белки", proteins)
        inputField("Жиры", fats)
        inputField("Углев.", carbs)
    }

    private fun inputField(label: String, value: String) {
        composeRule
            .onNode(
                hasSetTextAction() and hasAnyDescendant(hasText(label)),
                useUnmergedTree = true
            )
            .performTextClearance()

        if (value.isNotEmpty()) {
            composeRule
                .onNode(
                    hasSetTextAction() and hasAnyDescendant(hasText(label)),
                    useUnmergedTree = true
                )
                .performTextInput(value)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForProduct(name: String) {
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForDish(name: String) {
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitUntilTextDoesNotExist(text: String) {
        composeRule.waitUntilDoesNotExist(hasText(text), timeoutMillis = 5_000)
    }

    private fun assertTextIsAbove(upperText: String, lowerText: String) {
        val upperPosition = composeRule.onNodeWithText(upperText).fetchSemanticsNode().positionInRoot
        val lowerPosition = composeRule.onNodeWithText(lowerText).fetchSemanticsNode().positionInRoot
        assertTrue("$upperText должен отображаться выше $lowerText", upperPosition.y < lowerPosition.y)
    }
}

private class FakeRecipeBookApi : RecipeBookApi {
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
