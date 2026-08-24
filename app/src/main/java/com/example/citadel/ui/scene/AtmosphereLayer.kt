package com.example.citadel.ui.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.theme.DawnGold
import kotlin.math.PI
import kotlin.math.sin

fun DrawScope.drawAtmosphereLayer(
    sceneState: SceneState,
    arrivalState: ArrivalState,
    campfireFlicker: Float,
    sparkProgress1: Float,
    sparkProgress2: Float,
    sparkProgress3: Float,
    fogProgress1: Float,
    fogProgress2: Float,
    fogAlpha: Float
) {
    val width = size.width
    val height = size.height
    val campfireIntensity = sceneState.campfireIntensity

    // 1. Campfire Glow & Sparks
    val fireX = width * 0.26f
    val fireY = height * 0.53f
    val fireGlowRadius = (10.dp.toPx() + (campfireIntensity * 18.dp.toPx())) * campfireFlicker
    val fireBaseRadius = (4.dp.toPx() + (campfireIntensity * 4.dp.toPx())) * campfireFlicker

    // Flame glow circle
    drawCircle(
        color = DawnGold.copy(alpha = (0.12f + (campfireIntensity * 0.16f)).coerceIn(0f, 1f)),
        radius = fireGlowRadius,
        center = Offset(fireX, fireY)
    )
    // Core flame circle
    drawCircle(
        color = if (campfireIntensity > 0) DawnGold.copy(alpha = 0.9f) else Color(0xFFB35E28).copy(alpha = 0.6f),
        radius = fireBaseRadius,
        center = Offset(fireX, fireY)
    )

    // Spark particles
    if (campfireIntensity > 0.1f) {
        val s1Y = fireY - 40.dp.toPx() * sparkProgress1
        val s1X = fireX + sin(sparkProgress1 * 2 * PI).toFloat() * 5.dp.toPx() - 2.dp.toPx()
        drawCircle(DawnGold.copy(alpha = ((1f - sparkProgress1) * campfireIntensity).coerceIn(0f, 1f)), radius = 1.2.dp.toPx(), center = Offset(s1X, s1Y))

        val s2Y = fireY - 50.dp.toPx() * sparkProgress2
        val s2X = fireX + sin(sparkProgress2 * 2.5 * PI).toFloat() * 4.dp.toPx() + 3.dp.toPx()
        drawCircle(DawnGold.copy(alpha = ((1f - sparkProgress2) * campfireIntensity).coerceIn(0f, 1f)), radius = 1.0.dp.toPx(), center = Offset(s2X, s2Y))

        val s3Y = fireY - 35.dp.toPx() * sparkProgress3
        val s3X = fireX + sin(sparkProgress3 * 1.5 * PI).toFloat() * 6.dp.toPx() - 4.dp.toPx()
        drawCircle(DawnGold.copy(alpha = ((1f - sparkProgress3) * campfireIntensity).coerceIn(0f, 1f)), radius = 1.3.dp.toPx(), center = Offset(s3X, s3Y))
    }

    // 2. Valley Mist / Fog Layers
    val mistAlphaFraction = ((1.0f - campfireIntensity) * fogAlpha * arrivalState.mistDensityMultiplier).coerceIn(0f, 1f)
    
    // Layer 1
    val fogX1 = width * fogProgress1
    drawRoundRect(
        color = Color.White.copy(alpha = mistAlphaFraction),
        topLeft = Offset(fogX1 - (width * 0.25f), height * 0.58f),
        size = Size(width * 0.5f, 6.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    // Layer 2
    val fogX2 = width * fogProgress2
    drawRoundRect(
        color = Color.White.copy(alpha = (mistAlphaFraction * 0.8f).coerceIn(0f, 1f)),
        topLeft = Offset(fogX2 - (width * 0.2f), height * 0.62f),
        size = Size(width * 0.4f, 5.dp.toPx()),
        cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
    )
}
