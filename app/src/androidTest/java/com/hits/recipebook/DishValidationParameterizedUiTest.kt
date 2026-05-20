package com.hits.recipebook

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hits.recipebook.ui.theme.RecipeBookTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DishValidationParameterizedUiTest(
    private val name: String,
    private val portion: String,
    private val calories: String,
    private val proteins: String,
    private val fats: String,
    private val carbs: String,
    private val shouldSelectIngredient: Boolean,
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
    fun invalidDishData_showsValidation() {
        composeRule.onNodeWithText("Блюда").performClick()
        composeRule.waitUntilVisible(hasText("Салат UI"))
        composeRule.onNodeWithText("Создать блюдо").performClick()

        composeRule.inputField("Название* (макросы: !десерт, !первое...)", name)
        composeRule.inputField("Размер порции, г*", portion)
        composeRule.inputField("Ккал", calories)
        composeRule.inputField("Белки", proteins)
        composeRule.inputField("Жиры", fats)
        composeRule.inputField("Углев.", carbs)

        if (shouldSelectIngredient) {
            selectFirstDishIngredient()
        }

        composeRule.onNodeWithText("Создать").performClick()
        composeRule.onNodeWithText(expectedError).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun selectFirstDishIngredient() {
        composeRule.waitUntilAtLeastOneExists(hasTestTag("dish-ingredient-checkbox"), timeoutMillis = 5_000)
        composeRule.onAllNodesWithTag("dish-ingredient-checkbox").onFirst().performClick()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {7}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("Я", "100", "10", "1", "1", "1", true, "Название блюда: минимум 2 символа"),
            arrayOf("Салат", "100", "10", "1", "1", "1", false, "Нужно добавить минимум 1 продукт"),
            arrayOf("Салат", "", "10", "1", "1", "1", true, "Поле \\\"Размер порции\\\" не может быть пустым"),
            arrayOf("Овощное блюдо", "100", "10", "1", "1", "1", true, "Укажите категорию или добавьте макрос в названии"),
            arrayOf("!суп Борщ", "100", "abc", "1", "1", "1", true, "Поле \\\"Калорийность\\\" должно быть числом"),
        )
    }
}
