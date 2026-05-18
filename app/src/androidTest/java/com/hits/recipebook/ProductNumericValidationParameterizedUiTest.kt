package com.hits.recipebook

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
class ProductNumericValidationParameterizedUiTest(
    private val name: String,
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
        composeRule.onNodeWithText("Создать продукт").performClick()
        fillProductForm(name, calories, proteins, fats, carbs)

        composeRule.onNodeWithText("Создать").performClick()

        composeRule.onNodeWithText(expectedError).assertIsDisplayed()
    }

    private fun fillProductForm(name: String, calories: String, proteins: String, fats: String, carbs: String) {
        inputField("Название*", name)
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {5}")
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("Творог", "-0.1", "1", "1", "1", "Калорийность должна быть >= 0"),
            arrayOf("Творог", "10", "-0.1", "1", "1", "Белки должны быть в диапазоне 0..100"),
            arrayOf("Творог", "10", "100.1", "0", "0", "Белки должны быть в диапазоне 0..100"),
            arrayOf("Авокадо", "10", "0", "100.1", "0", "Жиры должны быть в диапазоне 0..100"),
            arrayOf("Фрукт", "10", "0", "0", "100.1", "Углеводы должны быть в диапазоне 0..100"),
            arrayOf("Сыр", "200", "40", "30", "30.1", "Сумма БЖУ на 100 г не может превышать 100"),
        )
    }
}
