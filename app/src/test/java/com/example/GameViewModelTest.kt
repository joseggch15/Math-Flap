package com.example

import org.junit.Test
import org.junit.Assert.*

class GameViewModelTest {
    @Test
    fun testStartGameAndPlay() {
        val viewModel = GameViewModel()
        viewModel.jump()
        println("Status: ${viewModel.state.value.status}")
        for (i in 0..100) {
            viewModel.updateGame(16, 0.5f)
        }
        println("Done without exception!")
    }
}
