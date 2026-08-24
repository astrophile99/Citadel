package com.example.citadel.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.theme.DawnGold
import com.example.citadel.ui.theme.TimeOfDay
import com.example.citadel.ui.theme.TwilightViolet

fun DrawScope.drawSkyLayer(
    timeOfDay: TimeOfDay,
    starAlpha1: Float,
    starAlpha2: Float,
    starAlpha3: Float,
    sunGlowScale: Float
) {
    val width = size.width
    val height = size.height

    // 1. Draw Time-of-Day Sky Gradient
    val skyColors = when (timeOfDay) {
        TimeOfDay.DAWN -> listOf(Color(0xFF0D1420), Color(0xFF24223A), Color(0xFF593F4B), Color(0xFFBC8544))
        TimeOfDay.DAY -> listOf(Color(0xFF0D1117), Color(0xFF141A24), Color(0xFF1F2D3D))
        TimeOfDay.DUSK -> listOf(Color(0xFF151111), Color(0xFF3A1F26), Color(0xFF7B3831), Color(0xFFC85A32))
        TimeOfDay.NIGHT -> listOf(Color(0xFF07050A), Color(0xFF0F0B18), Color(0xFF1D142B))
    }
    
    drawRect(brush = Brush.verticalGradient(colors = skyColors))

    // 2. Night Stars
    if (timeOfDay == TimeOfDay.NIGHT) {
        val starPositions = listOf(
            Offset(width * 0.12f, height * 0.16f) to (2.5f * starAlpha1),
            Offset(width * 0.45f, height * 0.12f) to (2.0f * starAlpha2),
            Offset(width * 0.78f, height * 0.20f) to (3.0f * starAlpha3),
            Offset(width * 0.60f, height * 0.10f) to (1.5f * starAlpha1),
            Offset(width * 0.20f, height * 0.28f) to (2.2f * starAlpha2),
            Offset(width * 0.90f, height * 0.14f) to (2.0f * starAlpha3),
            Offset(width * 0.35f, height * 0.22f) to (1.8f * starAlpha1),
            Offset(width * 0.70f, height * 0.30f) to (2.5f * starAlpha2)
        )
        for ((pos, alphaRadius) in starPositions) {
            drawCircle(
                color = Color(0xFFFFFDF5).copy(alpha = (alphaRadius / 3f).coerceIn(0f, 1f)),
                radius = alphaRadius.dp.toPx(),
                center = pos
            )
        }
    }

    // 3. Celestial Bodies (Sun or Moon)
    when (timeOfDay) {
        TimeOfDay.DAWN -> {
            val sunX = width * 0.22f
            val sunY = height * 0.45f
            val r = 20.dp.toPx()
            drawCircle(
                color = DawnGold.copy(alpha = 0.15f * sunGlowScale),
                radius = r * 1.5f,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = DawnGold,
                radius = r,
                center = Offset(sunX, sunY)
            )
        }
        TimeOfDay.DAY -> {
            val sunX = width * 0.30f
            val sunY = height * 0.16f
            val r = 24.dp.toPx()
            drawCircle(
                color = Color(0xFFFFF9DB).copy(alpha = 0.2f * sunGlowScale),
                radius = r * 1.6f,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFFFFDF5),
                radius = r,
                center = Offset(sunX, sunY)
            )
        }
        TimeOfDay.DUSK -> {
            val sunX = width * 0.78f
            val sunY = height * 0.40f
            val r = 22.dp.toPx()
            drawCircle(
                color = Color(0xFFFF7A44).copy(alpha = 0.25f * sunGlowScale),
                radius = r * 1.5f,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFFF9244),
                radius = r,
                center = Offset(sunX, sunY)
            )
        }
        TimeOfDay.NIGHT -> {
            val moonX = width * 0.25f
            val moonY = height * 0.18f
            val r = 16.dp.toPx()
            val moonBase = Path().apply {
                addOval(Rect(moonX - r, moonY - r, moonX + r, moonY + r))
            }
            val moonCutter = Path().apply {
                addOval(Rect(moonX + r * 0.4f - r, moonY - r * 0.2f - r, moonX + r * 0.4f + r, moonY - r * 0.2f + r))
            }
            val moonCrescent = Path().apply {
                op(moonBase, moonCutter, PathOperation.Difference)
            }
            drawCircle(
                color = TwilightViolet.copy(alpha = 0.15f),
                radius = r * 1.8f,
                center = Offset(moonX, moonY)
            )
            drawPath(moonCrescent, Color(0xFFE2E6EC))
        }
    }
}
