package com.example.citadel.ui.motion

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class SceneScrollController(val scrollState: ScrollState) {
    // Background parallax translation offset in pixels (0.30x speed)
    val parallaxOffsetPx: Float
        get() = -scrollState.value.toFloat() * 0.30f

    // Background darkness scrim alpha (0.0 to 0.65 as scrolling progresses)
    val backgroundScrimAlpha: Float
        get() = (scrollState.value.toFloat() / 900f).coerceIn(0f, 0.65f)

    // Backdrop scene alpha (never drops below 0.25f so the kingdom remains persistent)
    val sceneAlpha: Float
        get() = (1.0f - (scrollState.value.toFloat() / 1400f)).coerceIn(0.25f, 1.0f)
}

@Composable
fun rememberSceneScrollController(): SceneScrollController {
    val scrollState = rememberScrollState()
    return remember(scrollState) {
        SceneScrollController(scrollState)
    }
}
