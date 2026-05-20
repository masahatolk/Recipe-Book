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
class ProductNumericValidationParameterizedUiTest(
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
    fun invalidNumericValue_showsValidation() {
        composeRule.onNodeWithText("Создать продукт").performClick()
        fillProductForm(name, calories, proteins, fats, carbs)

        composeRule.onNodeWithText("Создать").performScrollTo().performClick()

        if (expectedError.isBlank()) {
            composeRule.onNodeWithText("Сумма БЖУ на 100 г не может превышать 100").assertDoesNotExist()
        } else {
            composeRule.onNodeWithText(expectedError).assertIsDisplayed()
        }
    }

    private fun fillProductForm(name: String, calories: String, proteins: String, fats: String, carbs: String) {
        composeRule.inputField("Название*", name)
        composeRule.inputField("Ккал", calories)
        composeRule.inputField("Белки", proteins)
        composeRule.inputField("Жиры", fats)
        composeRule.inputField("Углев.", carbs)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {5}")
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("Т", "0", "0", "0", "0", ""),
            arrayOf("Продукт", "0", "0", "0", "0", ""),
            arrayOf("Продукт", "0.1", "0", "0", "0", ""),
            arrayOf("Продукт", "-0.1", "0", "0", "0", "Калорийность должна быть >= 0"),
            arrayOf("Продукт", "9999.9", "0", "0", "0", ""),
            arrayOf("Продукт", "10", "0.1", "0", "0", ""),
            arrayOf("Продукт", "10", "99.9", "0", "0", ""),
            arrayOf("Продукт", "10", "100.1", "0", "0", "Белки должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "-0.1", "0", "0", "Белки должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "0", "0.1", "0", ""),
            arrayOf("Продукт", "10", "0", "99.9", "0", ""),
            arrayOf("Продукт", "10", "0", "100.1", "0", "Жиры должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "0", "-0.1", "0", "Жиры должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "0", "0", "0.1", ""),
            arrayOf("Продукт", "10", "0", "0", "99.9", ""),
            arrayOf("Продукт", "10", "0", "0", "100.1", "Углеводы должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "0", "0", "-0.1", "Углеводы должны быть в диапазоне 0..100"),
            arrayOf("Продукт", "10", "100", "0", "0", ""),
            arrayOf("Продукт", "10", "0", "100", "0", ""),
            arrayOf("Продукт", "10", "0", "0", "100", ""),
            arrayOf("Продукт", "10", "40", "30", "30.1", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "10", "40", "30.1", "30", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "10", "40.1", "30", "30", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "10", "99.9", "0.1", "0", ""),
            arrayOf("Продукт", "10", "99.9", "0.2", "0", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "10", "0.1", "99.9", "0", ""),
            arrayOf("Продукт", "10", "0.2", "99.9", "0", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "10", "0.1", "0", "99.9", ""),
            arrayOf("Продукт", "10", "0.2", "0", "99.9", "Сумма БЖУ на 100 г не может превышать 100"),
            arrayOf("Продукт", "999999", "100", "0", "0", ""),
            arrayOf("Продукт", "999999", "33.3", "33.3", "33.4", ""),
            arrayOf("Продукт", "1", "34", "33", "33.1", "Сумма БЖУ на 100 г не может превышать 100")
        )
    }
}
