package com.hits.recipebook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.hits.recipebook.ui.theme.RecipeBookTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProductValidationParameterizedUiTest(
    private val name: String,
    private val calories: String,
    private val proteins: String,
    private val fats: String,
    private val carbs: String,
    private val expectedError: String,
) : BaseComposeUiTest() {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent {
            RecipeBookTheme {
                RecipeBookApp(api = TestAppFactory.fakeApi())
            }
        }
        composeRule.waitUntilVisible(hasText("Авокадо UI"))
    }

    @Test
    fun invalidProductData_showsValidation() {
        composeRule.onNodeWithText("Создать продукт").performClick()
        composeRule.inputField("Название*", name)
        composeRule.inputField("Ккал", calories)
        composeRule.inputField("Белки", proteins)
        composeRule.inputField("Жиры", fats)
        composeRule.inputField("Углев.", carbs)
        composeRule.onNodeWithText("Создать").performScrollTo().performClick()

        composeRule.onNodeWithText(expectedError).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {5}")
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("Я", "10", "1", "1", "1", "Название продукта: минимум 2 символа"),
            arrayOf("Творог", "", "1", "1", "1", "Поле \"Калорийность\" не может быть пустым"),
            arrayOf("Творог", "abc", "1", "1", "1", "Поле \"Калорийность\" должно быть числом"),
        )
    }
}
