package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.GameStatus
import com.example.GameViewModel
import kotlinx.coroutines.isActive

@OptIn(ExperimentalTextApi::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Game loop
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (isActive) {
            val currentFrameTime = withFrameNanos { it }
            val deltaMs = (currentFrameTime - lastFrameTime) / 1_000_000L
            lastFrameTime = currentFrameTime
            viewModel.updateGame(deltaMs, 0.5f) // Dummy aspect
        }
    }

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = { viewModel.jump() })
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val density = LocalDensity.current.density

        // Background Gradient
        val skyGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF87CEEB), Color(0xFFB3E5FC), Color(0xFFE0F6FF))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw sky background
            drawRect(brush = skyGradient, size = size)

            // Draw ground (grass)
            drawRect(
                color = Color(0xFF43A047),
                topLeft = Offset(0f, height * 0.95f),
                size = Size(width, height * 0.05f)
            )

            // Draw obstacles
            for (obs in state.obstacles) {
                val obsLeft = obs.x * width
                val obsWidth = obs.width * width

                // Block colors
                val blockColor = Color(0xFF81C784)

                // Top block
                val topBlockBottom = (obs.gap1Center - obs.gapHeight / 2) * height
                val topBlockHeight = topBlockBottom.coerceAtLeast(0f)
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(obsLeft, 0f),
                    size = Size(obsWidth, topBlockHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Middle block
                val midBlockTop = (obs.gap1Center + obs.gapHeight / 2) * height
                val midBlockBottom = (obs.gap2Center - obs.gapHeight / 2) * height
                val midBlockHeight = (midBlockBottom - midBlockTop).coerceAtLeast(0f)
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(obsLeft, midBlockTop),
                    size = Size(obsWidth, midBlockHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Bottom block
                val bottomBlockTop = (obs.gap2Center + obs.gapHeight / 2) * height
                val bottomBlockHeight = (height - bottomBlockTop).coerceAtLeast(0f)
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(obsLeft, bottomBlockTop),
                    size = Size(obsWidth, bottomBlockHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Draw answers inside the gaps
                val correctAnswerText = obs.problem.correctAnswer.toString()
                val wrongAnswerText = obs.problem.wrongAnswer.toString()

                val answerStyle = TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                val (ans1, ans2) = if (obs.correctGapIndex == 1) {
                    correctAnswerText to wrongAnswerText
                } else {
                    wrongAnswerText to correctAnswerText
                }

                val drawLabel = { text: String, gapCenter: Float ->
                    val measured = textMeasurer.measure(text, answerStyle)
                    val textWidth = measured.size.width.toFloat()
                    val textHeight = measured.size.height.toFloat()
                    val labelBgWidth = textWidth + 40f
                    val labelBgHeight = textHeight + 20f
                    val labelTopLeft = Offset(
                        obsLeft + (obsWidth - labelBgWidth) / 2f,
                        (gapCenter * height) - (labelBgHeight / 2f)
                    )
                    
                    // Shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(labelTopLeft.x + 4f, labelTopLeft.y + 6f),
                        size = Size(labelBgWidth, labelBgHeight),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    
                    // Bubble
                    drawRoundRect(
                        color = Color(0xFFFFA726), // Orange bubble
                        topLeft = labelTopLeft,
                        size = Size(labelBgWidth, labelBgHeight),
                        cornerRadius = CornerRadius(16f, 16f)
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = answerStyle,
                        topLeft = Offset(
                            obsLeft + (obsWidth - textWidth) / 2f,
                            (gapCenter * height) - (textHeight / 2f)
                        ),
                        size = Size(textWidth, textHeight)
                    )
                }

                drawLabel(ans1, obs.gap1Center)
                drawLabel(ans2, obs.gap2Center)
            }

            // Draw player (cute circle/bird representation)
            val birdX = 0.2f * width
            val birdY = state.birdY * height
            val birdRadius = 0.05f * height

            drawCircle(
                color = Color(0xFFFFD54F),
                radius = birdRadius,
                center = Offset(birdX, birdY)
            )

            // Draw bird eye / details
            drawCircle(
                color = Color.Black,
                radius = birdRadius * 0.2f,
                center = Offset(birdX + birdRadius * 0.4f, birdY - birdRadius * 0.2f)
            )
        }

        // Draw Math Problem at the top center
        if (state.status == GameStatus.Playing && state.currentProblem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = state.currentProblem?.text ?: "",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // Score display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "Score: ${state.score}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }

        // Overlays for Menu / Game Over
        if (state.status == GameStatus.Menu) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tap to Start",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
        }

        if (state.status == GameStatus.GameOver) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Game Over! Tap to Retry",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
    }
}
