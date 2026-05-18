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
            arrayOf("10", "-0.1", "1", "1", "Белки должны быть >= 0"),
            arrayOf("10", "1", "-0.1", "1", "Жиры должны быть >= 0"),
            arrayOf("10", "1", "1", "-0.1", "Углеводы должны быть >= 0"),
            arrayOf("200", "40", "30", "30.1", "Сумма БЖУ на порцию не может превышать 100"),
        )
    }
}
