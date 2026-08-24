package com.example.citadel.ui.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.theme.TimeOfDay

fun DrawScope.drawCloudLayer(
    timeOfDay: TimeOfDay,
    cloud1Progress: Float,
    cloud2Progress: Float,
    cloudSeparation: Float
) {
    if (timeOfDay == TimeOfDay.NIGHT) return

    val width = size.width
    val height = size.height

    // Upper cloud band (Parts outward left and right during arrival sequence)
    val partOffsetLeft = -width * 0.45f * cloudSeparation
    val partOffsetRight = width * 0.45f * cloudSeparation

    // Cloud 1 (Drifting left-to-right + arrival separation)
    val baseCloud1X = width * cloud1Progress
    val cloud1X = baseCloud1X + partOffsetLeft
    
    drawRoundRect(
        color = Color.White.copy(alpha = (0.12f * (1.0f - (cloudSeparation * 0.5f))).coerceIn(0f, 1f)),
        topLeft = Offset(cloud1X - 45.dp.toPx(), height * 0.12f),
        size = Size(90.dp.toPx(), 14.dp.toPx()),
        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
    )

    // Cloud 2 (Drifting right-to-left + arrival separation)
    val baseCloud2X = width * cloud2Progress
    val cloud2X = baseCloud2X + partOffsetRight

    drawRoundRect(
        color = Color.White.copy(alpha = (0.08f * (1.0f - (cloudSeparation * 0.5f))).coerceIn(0f, 1f)),
        topLeft = Offset(cloud2X - 30.dp.toPx(), height * 0.24f),
        size = Size(65.dp.toPx(), 10.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    )
}
