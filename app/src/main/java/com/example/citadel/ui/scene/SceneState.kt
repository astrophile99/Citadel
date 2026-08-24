package com.example.citadel.ui.scene

import androidx.compose.runtime.Immutable
import com.example.citadel.ui.theme.TimeOfDay

@Immutable
data class SceneState(
    val timeOfDay: TimeOfDay,
    val campfireIntensity: Float = 0f,
    val completedCount: Int = 0,
    val cameraScale: Float = 1.0f,
    val cameraOffsetY: Float = 0.0f,
    val sceneAlpha: Float = 1.0f
)
