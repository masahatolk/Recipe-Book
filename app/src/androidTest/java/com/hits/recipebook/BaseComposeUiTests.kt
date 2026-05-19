package com.hits.recipebook

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

open class BaseComposeUiTest {

    protected fun ComposeContentTestRule.inputField(label: String, value: String) {
        val matcher = hasSetTextAction() and hasAnyDescendant(hasText(label))
        onNode(matcher, useUnmergedTree = true).performTextClearance()
        if (value.isNotEmpty()) {
            onNode(matcher, useUnmergedTree = true).performTextInput(value)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    protected fun ComposeContentTestRule.waitUntilVisible(matcher: SemanticsMatcher) {
        waitUntilAtLeastOneExists(matcher, timeoutMillis = 5_000)
    }

    @OptIn(ExperimentalTestApi::class)
    protected fun ComposeContentTestRule.waitUntilNotVisible(matcher: SemanticsMatcher) {
        waitUntilDoesNotExist(matcher, timeoutMillis = 5_000)
    }

    protected fun ComposeContentTestRule.assertOptionalError(errorMessage: String) {
        if (errorMessage.isBlank()) {
            onNodeWithText(errorMessage).assertDoesNotExist()
        } else {
            onNodeWithText(errorMessage).assertExists()
        }
    }
}
