package com.hits.recipebook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test


/**
 * UI-тесты формы создания продукта с использованием Kaspresso.
 *
 * Тест-дизайн:
 * 1) Эквивалентное разбиение:
 * - валидные и невалидные классы для обязательных полей (пустая строка / число / не число)
 * - валидные и невалидные классы для названия (длина >= 2 / < 2)
 *
 * 2) Анализ граничных значений:
 * - сумма БЖУ: 100 (валидно), 100.1 (невалидно)
 * - границы по белкам: 0 (валидно), 100 (валидно), 100.1 (невалидно)
 */
class ProductCreationKaspressoTest : TestCase() {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createProduct_emptyNameShowsValidationError() = run {
        step("Открыть форму создания продукта") {
            composeRule.onNodeWithText("Создать продукт").performClick()
        }

        step("Заполнить корректные числовые поля, оставить имя пустым") {
            fillProductForm(
                name = "",
                calories = "100",
                proteins = "10",
                fats = "10",
                carbs = "10"
            )
            composeRule.onNodeWithText("Создать").performClick()
        }

        step("Проверить ошибку обязательного поля") {
            composeRule.onNodeWithText("Поле \"Название\" не может быть пустым")
                .assertIsDisplayed()
        }
    }

    @Test
    fun createProduct_nameLengthBoundary_oneCharShowsValidationError() = run {
        composeRule.onNodeWithText("Создать продукт").performClick()

        fillProductForm(
            name = "Я",
            calories = "100",
            proteins = "10",
            fats = "10",
            carbs = "10"
        )
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.onNodeWithText("Название должно содержать минимум 2 символа")
            .assertIsDisplayed()
    }

    @Test
    fun createProduct_bzhuSumAbove100ShowsValidationError() = run {
        composeRule.onNodeWithText("Создать продукт").performClick()

        fillProductForm(
            name = "Тестовый продукт",
            calories = "100",
            proteins = "40",
            fats = "30",
            carbs = "30.1"
        )
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100")
            .assertIsDisplayed()
    }

    @Test
    fun createProduct_proteinUpperBoundaryAbove100ShowsValidationError() = run {
        composeRule.onNodeWithText("Создать продукт").performClick()

        fillProductForm(
            name = "Границы белка",
            calories = "100",
            proteins = "100.1",
            fats = "0",
            carbs = "0"
        )
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.onNodeWithText("Белки должны быть в диапазоне 0..100")
            .assertIsDisplayed()
    }

    private fun fillProductForm(
        name: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String,
    ) {
        inputByLabel("Название*", name)
        inputByLabel("Калорийность", calories)
        inputByLabel("Белки", proteins)
        inputByLabel("Жиры", fats)
        inputByLabel("Углеводы", carbs)
    }

    private fun inputByLabel(label: String, value: String) {
        composeRule.onNodeWithText(label).performClick()
        composeRule.onNodeWithText(label).performTextClearance()
        composeRule.onNodeWithText(label).performTextInput(value)
    }
}
