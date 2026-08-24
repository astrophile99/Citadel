package com.example.citadel.ui.scene

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ArrivalState(val overallProgress: Float) {
    // Cloud separation fraction during Stages 1-3 (0.0 to 0.5)
    val cloudSeparation: Float
        get() = when {
            overallProgress <= 0.16f -> 0.0f
            overallProgress >= 0.50f -> 1.0f
            else -> (overallProgress - 0.16f) / 0.34f
        }

    // Kingdom visibility fraction during Stage 3 (0.32 to 0.50)
    val kingdomVisibility: Float
        get() = when {
            overallProgress <= 0.32f -> 0.0f
            overallProgress >= 0.50f -> 1.0f
            else -> (overallProgress - 0.32f) / 0.18f
        }

    // Camera settling progress during Stage 4 (0.50 to 0.68)
    val cameraSettling: Float
        get() = when {
            overallProgress <= 0.50f -> 0.0f
            overallProgress >= 0.68f -> 1.0f
            else -> (overallProgress - 0.50f) / 0.18f
        }

    // Camera scale factor: starts at 1.10f and settles smoothly to 1.0f
    val cameraScale: Float
        get() = 1.10f - (0.10f * cameraSettling)

    // Camera vertical translation offset (in dp): slides down from -30.dp to 0.dp
    val cameraOffsetYDp: Float
        get() = -30.0f * (1.0f - cameraSettling)

    // Initial fog/mist opacity multiplier: heavy fog during stages 1-3, clearing by stage 5
    val mistDensityMultiplier: Float
        get() = when {
            overallProgress <= 0.32f -> 3.5f
            overallProgress >= 0.80f -> 1.0f
            else -> 3.5f - (2.5f * ((overallProgress - 0.32f) / 0.48f))
        }

    // Greeting fade-in fraction during Stage 6 (0.80 to 0.90)
    val greetingAlpha: Float
        get() = when {
            overallProgress <= 0.80f -> 0.0f
            overallProgress >= 0.90f -> 1.0f
            else -> (overallProgress - 0.80f) / 0.10f
        }

    // Mission journal cards rise and fade fraction during Stage 7 (0.90 to 1.00)
    val journalAlpha: Float
        get() = when {
            overallProgress <= 0.90f -> 0.0f
            overallProgress >= 1.00f -> 1.0f
            else -> (overallProgress - 0.90f) / 0.10f
        }

    val journalOffsetDp: Float
        get() = 60.0f * (1.0f - journalAlpha)
}

@Composable
fun rememberArrivalState(durationMs: Int = 5000): State<ArrivalState> {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing
        ),
        label = "CinematicArrivalProgress"
    )

    return remember(animProgress) {
        mutableStateOf(ArrivalState(animProgress))
    }
}
