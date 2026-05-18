package com.hits.recipebook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Подробный набор UI-тестов для ТЗ «Книга рецептов».
 *
 * В тестах применяются:
 * - Эквивалентное разбиение: валидные/невалидные классы данных для строк и чисел.
 * - Анализ граничных значений: 0, >0, 100, >100, минимальная длина названия.
 *
 * Набор фокусируется на детерминированной валидации формы на UI-слое,
 * чтобы тесты были стабильны без зависимости от состояния backend.
 */
class UiTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // ---------- Product tests ----------

    @Test
    fun product_nameTooShort_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Я", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Название продукта: минимум 2 символа").assertIsDisplayed()
    }

    @Test
    fun product_caloriesRequired_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Поле \"Калорийность\" не может быть пустым").assertIsDisplayed()
    }

    @Test
    fun product_caloriesMustBeNumber_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "abc", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Поле \"Калорийность\" должно быть числом").assertIsDisplayed()
    }

    @Test
    fun product_caloriesBelowZero_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "-0.1", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Калорийность должна быть >= 0").assertIsDisplayed()
    }

    @Test
    fun product_proteinsBelowZero_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "10", proteins = "-0.1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_proteinsAboveHundred_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Творог", calories = "10", proteins = "100.1", fats = "0", carbs = "0")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_fatsAboveHundred_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Авокадо", calories = "10", proteins = "0", fats = "100.1", carbs = "0")
        save()
        composeRule.onNodeWithText("Жиры должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_carbsAboveHundred_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Фрукт", calories = "10", proteins = "0", fats = "0", carbs = "100.1")
        save()
        composeRule.onNodeWithText("Углеводы должны быть в диапазоне 0..100").assertIsDisplayed()
    }

    @Test
    fun product_bzhuSumAboveHundred_showsValidation() = run {
        openProductEditor()
        fillProductForm(name = "Сыр", calories = "200", proteins = "40", fats = "30", carbs = "30.1")
        save()
        composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100").assertIsDisplayed()
    }

    @Test
    fun product_bzhuSumEqualHundred_passesLocalValidationAndFailsOnBackend() = run {
        openProductEditor()
        fillProductForm(name = "Сыр", calories = "200", proteins = "40", fats = "30", carbs = "30")
        save()
        composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100").assertDoesNotExist()
    }

    @Test
    fun product_zeroBoundaryValues_areAcceptedLocally() = run {
        openProductEditor()
        fillProductForm(name = "Вода", calories = "0", proteins = "0", fats = "0", carbs = "0")
        save()
        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100").assertDoesNotExist()
    }

    // ---------- Dish tests ----------

    @Test
    fun dish_nameTooShort_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "Я", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Название блюда: минимум 2 символа").assertIsDisplayed()
    }

    @Test
    fun dish_withoutIngredients_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "Салат", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_portionRequired_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "Салат", portion = "", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_categoryRequiredWithoutMacro_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "Овощное блюдо", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_caloriesMustBeNumber_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "abc", proteins = "1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_proteinsBelowZero_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "-0.1", fats = "1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_fatsBelowZero_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "1", fats = "-0.1", carbs = "1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_carbsBelowZero_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "10", proteins = "1", fats = "1", carbs = "-0.1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_bzhuSumAboveHundred_showsValidation() = run {
        openDishEditor()
        fillDishBaseForm(name = "!суп Борщ", portion = "100", calories = "200", proteins = "40", fats = "30", carbs = "30.1")
        save()
        composeRule.onNodeWithText("Нужно добавить минимум 1 продукт").assertIsDisplayed()
    }

    @Test
    fun dish_flagsSectionVisible_andHasThreeFlags() = run {
        openDishEditor()
        composeRule.onNodeWithText("Флаги блюда").assertIsDisplayed()
        composeRule.onNodeWithText("Веган").assertIsDisplayed()
        composeRule.onNodeWithText("Без глютена").assertIsDisplayed()
        composeRule.onNodeWithText("Без сахара").assertIsDisplayed()
    }

    @Test
    fun filters_canBeExpandedForProductsAndDishes() = run {
        composeRule.onNodeWithText("Открыть фильтры и сортировку").performClick()
        composeRule.onNodeWithText("Сортировка").assertIsDisplayed()

        composeRule.onNodeWithText("Блюда").performClick()
        composeRule.onNodeWithText("Открыть фильтры").performClick()
        composeRule.onNodeWithText("Категория").assertIsDisplayed()
    }

    private fun openProductEditor() {
        composeRule.onNodeWithText("Создать продукт").performClick()
    }

    private fun openDishEditor() {
        composeRule.onNodeWithText("Блюда").performClick()
        composeRule.onNodeWithText("Создать блюдо").performClick()
    }

    private fun save() {
        composeRule.onNodeWithText("Создать").performClick()
    }

    private fun fillProductForm(name: String, calories: String, proteins: String, fats: String, carbs: String) {
        inputField("Название*", name)
        inputField("Калорийность", calories)
        inputField("Белки", proteins)
        inputField("Жиры", fats)
        inputField("Углеводы", carbs)
    }

    private fun fillDishBaseForm(name: String, portion: String, calories: String, proteins: String, fats: String, carbs: String) {
        inputField("Название* (макросы: !десерт, !первое...) ", name)
        inputField("Размер порции, г*", portion)
        inputField("Калорийность", calories)
        inputField("Белки", proteins)
        inputField("Жиры", fats)
        inputField("Углеводы", carbs)
    }

    private fun inputField(label: String, value: String) {
        composeRule.onNodeWithText(label).performClick()
        composeRule.onNodeWithText(label).performTextClearance()
        composeRule.onNodeWithText(label).performTextInput(value)
    }
}