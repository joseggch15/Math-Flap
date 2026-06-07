package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class GameState(
    val status: GameStatus = GameStatus.Menu,
    val score: Int = 0,
    val birdY: Float = 0.5f,
    val birdVelocity: Float = 0f,
    val obstacles: List<Obstacle> = emptyList(),
    val targetAspect: Float = 0.5f // Ratio of width to height
) {
    val currentProblem: MathProblem?
        get() = obstacles.firstOrNull { !it.passed }?.problem
}

enum class GameStatus {
    Menu, Playing, GameOver
}

data class MathProblem(val text: String, val correctAnswer: Int, val wrongAnswer: Int)

data class Obstacle(
    val x: Float,
    val gap1Center: Float,
    val gap2Center: Float,
    val gapHeight: Float,
    val width: Float,
    val correctGapIndex: Int, // 1 or 2
    val problem: MathProblem,
    val passed: Boolean = false
)

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state = _state.asStateFlow()

    // Physics constants (normalized to screen height = 1.0)
    private val gravity = 1.2f
    private val jumpStrength = -0.45f
    private val horizontalSpeed = 0.22f // screen widths per second

    private var accumulatedTime = 0f

    fun startGame() {
        _state.update {
            GameState(
                status = GameStatus.Playing,
                score = 0,
                birdY = 0.5f, // start in middle
                birdVelocity = jumpStrength, // Initial jump on start so you don't just fall instantly
                obstacles = listOf(generateObstacle(1.2f, 0))
            )
        }
    }

    fun jump() {
        if (_state.value.status == GameStatus.Playing) {
            _state.update { it.copy(birdVelocity = jumpStrength) }
        } else if (_state.value.status == GameStatus.GameOver || _state.value.status == GameStatus.Menu) {
            startGame()
        }
    }

    fun updateGame(deltaMs: Long, aspect: Float) {
        // Cap dt to 40ms max to prevent physics explosions on pause/resume or lag spikes
        val dt = (deltaMs / 1000f).coerceAtMost(0.04f)
        val currentState = _state.value

        if (currentState.status != GameStatus.Playing) return

        var newVelocity = currentState.birdVelocity + gravity * dt
        var newBirdY = currentState.birdY + newVelocity * dt

        // Boundaries (Floor and Ceiling)
        if (newBirdY > 0.95f || newBirdY < 0.05f) {
            gameOver()
            return
        }

        // Update obstacles
        var newObstacles = currentState.obstacles.map { it.copy(x = it.x - horizontalSpeed * dt) }.toMutableList()
        var newScore = currentState.score
        var newProblem = currentState.currentProblem

        val birdX = 0.2f
        val birdRadius = 0.05f

        for (i in newObstacles.indices) {
            val obs = newObstacles[i]

            // Collision Detection
            // Left edge of obstacle is obs.x
            // Right edge of obstacle is obs.x + obs.width
            val inCollisionZone = birdX + birdRadius > obs.x && birdX - birdRadius < obs.x + obs.width

            if (inCollisionZone) {
                // Check if bird is safely INSIDE one of the gaps
                val inGap1 = newBirdY - birdRadius > (obs.gap1Center - obs.gapHeight / 2) &&
                             newBirdY + birdRadius < (obs.gap1Center + obs.gapHeight / 2)
                
                val inGap2 = newBirdY - birdRadius > (obs.gap2Center - obs.gapHeight / 2) &&
                             newBirdY + birdRadius < (obs.gap2Center + obs.gapHeight / 2)

                if (!inGap1 && !inGap2) {
                    gameOver()
                    return
                }
            }

            // Exactly crossing the center of the obstacle -> Check if correct gap!
            if (!obs.passed && birdX > obs.x + obs.width / 2) {
                newObstacles[i] = obs.copy(passed = true)
                // Determine which gap the bird went through
                val distToGap1 = Math.abs(newBirdY - obs.gap1Center)
                val distToGap2 = Math.abs(newBirdY - obs.gap2Center)
                val passedGapIndex = if (distToGap1 < distToGap2) 1 else 2

                if (passedGapIndex == obs.correctGapIndex) {
                    newScore++
                } else {
                    gameOver()
                    return
                }
            }
        }

        // Spawn new obstacles
        if (newObstacles.last().x < 0.2f) {
            newObstacles.add(generateObstacle(1.2f, newScore))
        }

        // Remove off-screen obstacles
        if (newObstacles.first().x < -0.3f) {
            newObstacles.removeAt(0)
        }

        _state.update {
            it.copy(
                birdY = newBirdY,
                birdVelocity = newVelocity,
                obstacles = newObstacles,
                score = newScore
            )
        }
    }

    private fun gameOver() {
        _state.update { it.copy(status = GameStatus.GameOver) }
    }

    private fun generateObstacle(startX: Float, levelScore: Int): Obstacle {
        val gapHeight = 0.25f // 25% of screen height
        // Randomize gap centers. Must be separated and fit in screen
        val gap1Center = Random.nextFloat() * 0.15f + 0.2f // 0.2 to 0.35
        val gap2Center = Random.nextFloat() * 0.15f + 0.65f // 0.65 to 0.8
        
        val correctIndex = if (Random.nextBoolean()) 1 else 2
        val problem = generateProblem(levelScore)
        
        return Obstacle(
            x = startX,
            gap1Center = gap1Center,
            gap2Center = gap2Center,
            gapHeight = gapHeight,
            width = 0.12f,
            correctGapIndex = correctIndex,
            problem = problem
        )
    }

    private fun generateProblem(levelScore: Int): MathProblem {
        val isSub = levelScore > 8 && Random.nextBoolean()
        val isSeq = levelScore < 5
        
        if (isSeq) {
            // Counting Sequence: a, b, ?, d
            val start = Random.nextInt(1, 10)
            val correct = start + 2
            val offset = Random.nextInt(-1, 2).takeIf { it != 0 } ?: 1
            var wrong = correct + offset
            return MathProblem("$start, ${start + 1}, _, ${start + 3}", correct, wrong)
        } else if (isSub) {
            // Subtraction: a - b = c, where a is 1..10, b is 1..a
            val a = Random.nextInt(2, 11)
            val b = Random.nextInt(1, a)
            val correct = a - b
            var wrong = correct + Random.nextInt(-2, 3)
            if (wrong == correct) wrong = correct + 1
            if (wrong < 0) wrong = 0
            return MathProblem("$a - $b = ?", correct, wrong)
        } else {
            // Addition
            val maxAddend = if (levelScore < 10) 5 else 10
            val a = Random.nextInt(1, maxAddend + 1)
            val b = Random.nextInt(1, maxAddend + 1)
            val correct = a + b
            var wrong = correct + Random.nextInt(-2, 3)
            if (wrong == correct) wrong = correct + 1
            return MathProblem("$a + $b = ?", correct, wrong)
        }
    }
}
