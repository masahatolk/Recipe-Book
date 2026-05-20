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
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.hits.recipebook.ui.theme.RecipeBookTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

    @Test
    fun product_view_showsDetails() {
        searchProduct("Банан UI")

        composeRule.onAllNodesWithText("Просмотр").onFirst().performClick()
        composeRule.onNodeWithText("Просмотр продукта").assertIsDisplayed()
        composeRule.onNodeWithText("Название: Банан UI").assertIsDisplayed()
        composeRule.onNodeWithText("Категория: Сладости").assertIsDisplayed()
    }

    @Test
    fun product_edit_updatesName() {
        searchProduct("Банан UI")
        composeRule.onAllNodesWithText("Редактировать").onFirst().performClick()
        composeRule.onNodeWithText("Редактирование продукта").assertIsDisplayed()
        inputField("Название*", "Банан UI обновлен")
        saveEdited()
        waitUntilTextDoesNotExist("Сохранить")
        searchProduct("Банан UI обновлен")
        waitForProduct("Банан UI обновлен")
    }

    @Test
    fun product_deleteUsedInDish_showsBlockedMessage() {
        searchProduct("Авокадо UI")

        composeRule.onAllNodesWithText("Удалить").onFirst().performClick()

        val blockedDeletionTextPrefix = "Удаление недоступно: продукт используется в блюдах"
        waitForText(blockedDeletionTextPrefix, substring = true)
        composeRule.onNodeWithText(blockedDeletionTextPrefix, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Авокадо UI").assertIsDisplayed()
    }

    @Test
    fun product_delete_removesProductFromList() {
        searchProduct("Банан UI")
        composeRule.onAllNodesWithText("Удалить").onFirst().performClick()
        waitForProductListEmpty()
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

    @Test
    fun dish_flagsSectionVisible_andHasThreeFlags() {
        openDishEditor()
        composeRule.onNodeWithText("Флаги блюда").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Веган").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Без глютена").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Без сахара").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dish_view_showsDetails() {
        openDishesTab()
        searchDish("Салат UI")

        composeRule.onAllNodesWithText("Просмотр").onFirst().performClick()
        composeRule.onNodeWithText("Просмотр блюда").assertIsDisplayed()
        composeRule.onNodeWithText("Название: Салат UI").assertIsDisplayed()
        composeRule.onNodeWithText("Категория: Салат").assertIsDisplayed()
        composeRule.onNodeWithText("- Авокадо UI: 50.0 г").assertIsDisplayed()
    }

    @Test
    fun dish_edit_updatesName() {
        openDishesTab()
        searchDish("Салат UI")
        composeRule.onAllNodesWithText("Редактировать").onFirst().performClick()
        composeRule.onNodeWithText("Редактирование блюда").assertIsDisplayed()
        inputField("Название* (макросы: !десерт, !первое...)", "Салат UI обновлен")
        saveEdited()
        waitUntilTextDoesNotExist("Сохранить")
        searchDish("Салат UI обновлен")
        waitForDish("Салат UI обновлен")
    }

    @Test
    fun dish_delete_removesDishFromList() {
        openDishesTab()
        searchDish("Салат UI")
        composeRule.onAllNodesWithText("Удалить").onFirst().performClick()
        waitForDishListEmpty()
    }

    @Test
    fun dish_createWithMacro_setsCategory() {
        openDishEditor()
        fillDishBaseForm(
            name = "!десерт Макро UI",
            portion = "100",
            calories = "10",
            proteins = "1",
            fats = "1",
            carbs = "1"
        )
        selectFirstDishIngredient()
        save()
        waitUntilTextDoesNotExist("Создать")

        searchDish("Макро UI")
        waitForDish("Макро UI")
        composeRule.onNodeWithText("Категория: Десерт").assertIsDisplayed()
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
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag("dish-ingredient-checkbox"),
            timeoutMillis = 5_000
        )
        composeRule.onAllNodesWithTag("dish-ingredient-checkbox").onFirst().performClick()
    }

    private fun fillProductForm(
        name: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String
    ) {
        inputField("Название*", name)
        inputField("Ккал", calories)
        inputField("Белки", proteins)
        inputField("Жиры", fats)
        inputField("Углев.", carbs)
    }

    private fun fillDishBaseForm(
        name: String,
        portion: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String
    ) {
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
    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntilAtLeastOneExists(
            hasText(text, substring = substring),
            timeoutMillis = 5_000
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForDish(name: String) {
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitUntilTextDoesNotExist(text: String) {
        composeRule.waitUntilDoesNotExist(hasText(text), timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForProductListEmpty() {
        composeRule.waitUntilAtLeastOneExists(hasText("Продукты не найдены"), timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForDishListEmpty() {
        composeRule.waitUntilAtLeastOneExists(hasText("Блюда не найдены"), timeoutMillis = 5_000)
    }

    private fun assertTextIsAbove(upperText: String, lowerText: String) {
        val upperPosition =
            composeRule.onNodeWithText(upperText).fetchSemanticsNode().positionInRoot
        val lowerPosition =
            composeRule.onNodeWithText(lowerText).fetchSemanticsNode().positionInRoot
        assertTrue(
            "$upperText должен отображаться выше $lowerText",
            upperPosition.y < lowerPosition.y
        )
    }
}
