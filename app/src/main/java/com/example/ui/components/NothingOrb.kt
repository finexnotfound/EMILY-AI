package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingGreyDark
import com.example.ui.theme.NothingGreyLight
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NothingOrb(
    state: AssistantState,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "fast_rotation"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("nothing_orb_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f

            // Dynamic RMS amplification
            val rmsBoost = (audioRms * 32f)

            when (state) {
                AssistantState.SLEEPING -> {
                    // Dim sleeping wireframe ring
                    drawCircle(
                        color = NothingGreyDark,
                        radius = baseRadius * 0.9f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                        )
                    )
                    // Dim core dot
                    drawCircle(
                        color = Color(0xFF222222),
                        radius = baseRadius * 0.4f,
                        center = center
                    )
                }

                AssistantState.LISTENING_WAKE_WORD -> {
                    // Standby ring with signature Nothing OS dotted circles
                    drawCircle(
                        color = NothingGreyDark,
                        radius = baseRadius * pulse,
                        center = center,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), rotation)
                        )
                    )
                    drawCircle(
                        color = NothingWhite,
                        radius = baseRadius * 0.55f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Subtle red glyph center dot
                    drawCircle(
                        color = NothingRed,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                AssistantState.LISTENING_COMMAND -> {
                    // Active audio-reactive pulsating rings
                    val dynamicRadius = (baseRadius + rmsBoost) * pulse
                    drawCircle(
                        color = NothingGreyLight.copy(alpha = 0.4f),
                        radius = dynamicRadius * 1.35f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), rotation)
                        )
                    )
                    drawCircle(
                        color = NothingWhite,
                        radius = dynamicRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Glowing active core
                    drawCircle(
                        color = NothingWhite,
                        radius = (baseRadius * 0.55f) + (rmsBoost * 0.4f),
                        center = center
                    )
                }

                AssistantState.THINKING -> {
                    // Rotating constellation rings
                    drawCircle(
                        color = NothingGreyLight,
                        radius = baseRadius * 1.15f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 14f), fastRotation)
                        )
                    )
                    drawCircle(
                        color = NothingWhite,
                        radius = baseRadius * 0.75f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), -fastRotation)
                        )
                    )
                    // Orbiting satellite dots
                    val orbitAngle = Math.toRadians(fastRotation.toDouble())
                    val dotX = center.x + (baseRadius * 1.15f * cos(orbitAngle)).toFloat()
                    val dotY = center.y + (baseRadius * 1.15f * sin(orbitAngle)).toFloat()
                    drawCircle(
                        color = NothingRed,
                        radius = 5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                AssistantState.SPEAKING -> {
                    // Acoustic ripples expanding outward
                    val waveRadius = baseRadius * 1.1f * pulse
                    drawCircle(
                        color = NothingWhite.copy(alpha = 0.25f),
                        radius = waveRadius * 1.4f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = NothingWhite,
                        radius = waveRadius,
                        center = center,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 6f), 0f)
                        )
                    )
                    drawCircle(
                        color = NothingWhite,
                        radius = baseRadius * 0.6f,
                        center = center
                    )
                }

                AssistantState.EXECUTING -> {
                    // Strobe wireframe circle
                    drawCircle(
                        color = NothingWhite,
                        radius = baseRadius * 1.1f,
                        center = center,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), fastRotation * 2f)
                        )
                    )
                    drawCircle(
                        color = NothingRed,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }

                AssistantState.ERROR -> {
                    // Error state with red halo
                    drawCircle(
                        color = NothingRed.copy(alpha = 0.5f),
                        radius = baseRadius * 1.2f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = NothingRed,
                        radius = baseRadius * 0.7f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                }
            }
        }
    }
}
