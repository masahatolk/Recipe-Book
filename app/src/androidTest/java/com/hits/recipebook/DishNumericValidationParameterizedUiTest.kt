package com.hits.recipebook

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.hits.recipebook.ui.theme.RecipeBookTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DishNumericValidationParameterizedUiTest(
    private val calories: String,
    private val proteins: String,
    private val fats: String,
    private val carbs: String,
    private val expectedError: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent {
            RecipeBookTheme {
                RecipeBookApp(api = FakeRecipeBookApi())
            }
        }
        waitForProduct("Авокадо UI")
    }

    @Test
    fun invalidNumericValue_showsValidation() {
        openDishEditor()
        selectFirstDishIngredient()
        fillDishBaseForm(
            name = "!суп Борщ",
            portion = "100",
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        )

        composeRule.onNodeWithText("Создать").performClick()

        composeRule.onNodeWithText(expectedError).assertIsDisplayed()
    }

    private fun openDishEditor() {
        composeRule.onNodeWithText("Блюда").performClick()
        waitForDish("Салат UI")
        composeRule.onNodeWithText("Создать блюдо").performClick()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun selectFirstDishIngredient() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("dish-ingredient-checkbox"), timeoutMillis = 5_000)
        composeRule.onAllNodesWithTag("dish-ingredient-checkbox").onFirst().performClick()
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {4}")
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("10", "-0.0001", "1", "1", "Белки должны быть >= 0"),
            arrayOf("10", "-1", "1", "1", "Белки должны быть >= 0"),
            arrayOf("10", "0", "1", "1", ""),
            arrayOf("10", "0.0001", "1", "1", ""),

            arrayOf("10", "1", "-0.0001", "1", "Жиры должны быть >= 0"),
            arrayOf("10", "1", "-1", "1", "Жиры должны быть >= 0"),
            arrayOf("10", "1", "0", "1", ""),
            arrayOf("10", "1", "0.0001", "1", ""),

            arrayOf("10", "1", "1", "-0.0001", "Углеводы должны быть >= 0"),
            arrayOf("10", "1", "1", "-1", "Углеводы должны быть >= 0"),
            arrayOf("10", "1", "1", "0", ""),
            arrayOf("10", "1", "1", "0.0001", ""),

            arrayOf("200", "40", "30", "30", ""),
            arrayOf("200", "100", "0", "0", ""),
            arrayOf("200", "0", "100", "0", ""),
            arrayOf("200", "0", "0", "100", ""),

            arrayOf("200", "40", "30", "30.0001", "Сумма БЖУ на порцию не может превышать 100"),
            arrayOf("200", "40", "30.0001", "30", "Сумма БЖУ на порцию не может превышать 100"),
            arrayOf("200", "40.0001", "30", "30", "Сумма БЖУ на порцию не может превышать 100"),
            arrayOf("200", "100", "0", "0.0001", "Сумма БЖУ на порцию не может превышать 100"),

            arrayOf("200", "50", "50", "50", "Сумма БЖУ на порцию не может превышать 100"),
            arrayOf("200", "101", "0", "0", "Сумма БЖУ на порцию не может превышать 100"),

            arrayOf("200", "0", "0", "0", ""),

            arrayOf("200", "99.9999", "0", "0", ""),
            arrayOf("200", "0", "99.9999", "0", ""),
            arrayOf("200", "0", "0", "99.9999", ""),

            arrayOf("200", "0", "0", "0.1", ""),
            arrayOf("200", "0.1", "0", "0", ""),
            arrayOf("200", "0", "0.1", "0", ""),

            arrayOf("200", "60", "20", "20", ""),
            arrayOf("200", "33.3", "33.3", "33.4", ""),

            arrayOf("200", "33.3", "33.3", "33.41", "Сумма БЖУ на порцию не может превышать 100"),

            arrayOf("200", "-1", "60", "50", "Белки должны быть >= 0"),
            arrayOf("200", "60", "-1", "50", "Жиры должны быть >= 0"),
            arrayOf("200", "60", "50", "-1", "Углеводы должны быть >= 0")
        )
    }
}
