package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.ui.GameScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenCrashTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simulateStartGameAndLoop() {
        val viewModel = GameViewModel()
        composeTestRule.setContent {
            GameScreen(viewModel = viewModel)
        }
        
        println("Clicking root to start the game")
        composeTestRule.onRoot().performClick()
        
        println("Advancing time to see if crash happens")
        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.mainClock.advanceTimeBy(100L)
        println("No crash!")
    }
}
