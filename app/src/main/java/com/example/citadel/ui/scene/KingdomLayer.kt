package com.example.citadel.ui.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.theme.DawnGold
import com.example.citadel.ui.theme.TimeOfDay
import kotlin.math.PI
import kotlin.math.sin

fun DrawScope.drawKingdomLayer(
    sceneState: SceneState,
    primaryColor: Color,
    smokeProgress1: Float,
    smokeProgress2: Float,
    lanternAlpha: Float,
    birdFlap: Float,
    bird1RestingFraction: Float,
    bird2RestingFraction: Float,
    bird3RestingFraction: Float,
    mountainPath: Path,
    hillPath1: Path,
    hillPath2: Path,
    foregroundHillPath: Path
) {
    val width = size.width
    val height = size.height
    val timeOfDay = sceneState.timeOfDay
    val campfireIntensity = sceneState.campfireIntensity
    val completedCount = sceneState.completedCount

    // 1. Distant Mountains (Atmospheric depth)
    val mountainColor = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFF2E2C4A).copy(alpha = 0.4f)
        TimeOfDay.DAY -> Color(0xFF1E283A).copy(alpha = 0.35f)
        TimeOfDay.DUSK -> Color(0xFF4C2A3A).copy(alpha = 0.5f)
        TimeOfDay.NIGHT -> Color(0xFF130E20).copy(alpha = 0.4f)
    }
    mountainPath.reset()
    mountainPath.moveTo(0f, height)
    mountainPath.lineTo(width * 0.15f, height * 0.35f)
    mountainPath.lineTo(width * 0.35f, height * 0.26f)
    mountainPath.lineTo(width * 0.55f, height * 0.38f)
    mountainPath.lineTo(width * 0.70f, height * 0.22f)
    mountainPath.lineTo(width * 0.85f, height * 0.32f)
    mountainPath.lineTo(width, height)
    mountainPath.close()
    drawPath(mountainPath, mountainColor)

    // 2. Midground Hills
    val midgroundColor = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFF141C2B)
        TimeOfDay.DAY -> Color(0xFF18212D)
        TimeOfDay.DUSK -> Color(0xFF221C1C)
        TimeOfDay.NIGHT -> Color(0xFF110E18)
    }
    
    // Left slope
    hillPath1.reset()
    hillPath1.moveTo(0f, height)
    hillPath1.lineTo(0f, height * 0.46f)
    hillPath1.quadraticTo(width * 0.35f, height * 0.56f, width * 0.7f, height)
    hillPath1.close()
    drawPath(hillPath1, midgroundColor)

    // Right slope containing the Castle Keep
    hillPath2.reset()
    hillPath2.moveTo(width * 0.35f, height)
    hillPath2.quadraticTo(width * 0.65f, height * 0.42f, width, height * 0.48f)
    hillPath2.lineTo(width, height)
    hillPath2.close()
    drawPath(hillPath2, midgroundColor)

    // 3. Keep Silhouette
    val keepLeft = width * 0.70f
    val keepBaseY = height * 0.49f
    val keepColor = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFF0F1520)
        TimeOfDay.DAY -> Color(0xFF121A24)
        TimeOfDay.DUSK -> Color(0xFF1B1515)
        TimeOfDay.NIGHT -> Color(0xFF0B0910)
    }
    
    // Castle main tower
    val towerW = 22.dp.toPx()
    val towerH = 54.dp.toPx()
    val towerX = keepLeft + 6.dp.toPx()
    val towerTop = keepBaseY - towerH
    
    drawRect(
        color = keepColor,
        topLeft = Offset(towerX, towerTop),
        size = Size(towerW, towerH)
    )

    // Castle side walls
    drawRect(
        color = keepColor,
        topLeft = Offset(towerX - 16.dp.toPx(), keepBaseY - 26.dp.toPx()),
        size = Size(16.dp.toPx(), 26.dp.toPx())
    )
    drawRect(
        color = keepColor,
        topLeft = Offset(towerX + towerW, keepBaseY - 22.dp.toPx()),
        size = Size(18.dp.toPx(), 22.dp.toPx())
    )

    // Tower triangular roof
    val roofP = Path().apply {
        moveTo(towerX - 2.dp.toPx(), towerTop)
        lineTo(towerX + (towerW / 2f), towerTop - 12.dp.toPx())
        lineTo(towerX + towerW + 2.dp.toPx(), towerTop)
        close()
    }
    drawPath(roofP, primaryColor.copy(alpha = 0.75f))

    // Chimney on Left Roof
    val chimneyLeft = towerX + 3.dp.toPx()
    val chimneyTop = towerTop - 4.dp.toPx()
    val chimneyW = 3.dp.toPx()
    val chimneyH = 6.dp.toPx()
    drawRect(
        color = keepColor,
        topLeft = Offset(chimneyLeft, chimneyTop),
        size = Size(chimneyW, chimneyH)
    )

    // Chimney smoke particles rising
    val smokeAlphaBase = 0.4f * (0.4f + 0.6f * campfireIntensity)
    // Smoke 1
    val sm1Y = chimneyTop - 32.dp.toPx() * smokeProgress1
    val sm1X = chimneyLeft + (chimneyW / 2f) + sin(smokeProgress1 * 3f * PI).toFloat() * 5.dp.toPx()
    drawCircle(
        color = Color.LightGray.copy(alpha = ((1f - smokeProgress1) * smokeAlphaBase).coerceIn(0f, 1f)),
        radius = (1.5f.dp.toPx() + 5.dp.toPx() * smokeProgress1),
        center = Offset(sm1X, sm1Y)
    )
    // Smoke 2
    val sm2Y = chimneyTop - 28.dp.toPx() * smokeProgress2
    val sm2X = chimneyLeft + (chimneyW / 2f) + sin(smokeProgress2 * 2.5f * PI).toFloat() * -4.dp.toPx()
    drawCircle(
        color = Color.LightGray.copy(alpha = ((1f - smokeProgress2) * smokeAlphaBase).coerceIn(0f, 1f)),
        radius = (1.2f.dp.toPx() + 4.dp.toPx() * smokeProgress2),
        center = Offset(sm2X, sm2Y)
    )

    // Tower window glow
    val windowAmberDim = Color(0xFF6E5630)
    val windowGoldBright = Color(0xFFFFD56B)
    
    val towerWindowColor = Color(
        red = windowAmberDim.red + (windowGoldBright.red - windowAmberDim.red) * campfireIntensity,
        green = windowAmberDim.green + (windowGoldBright.green - windowAmberDim.green) * campfireIntensity,
        blue = windowAmberDim.blue + (windowGoldBright.blue - windowAmberDim.blue) * campfireIntensity,
        alpha = windowAmberDim.alpha + (windowGoldBright.alpha - windowAmberDim.alpha) * campfireIntensity
    )
    
    if (completedCount > 0) {
        drawCircle(
            color = windowGoldBright.copy(alpha = 0.2f + 0.15f * campfireIntensity),
            radius = 8.dp.toPx() + 4.dp.toPx() * campfireIntensity,
            center = Offset(towerX + (towerW / 2f), towerTop + 12.dp.toPx())
        )
    }
    drawRect(
        color = towerWindowColor,
        topLeft = Offset(towerX + (towerW / 2f) - 3.dp.toPx(), towerTop + 8.dp.toPx()),
        size = Size(6.dp.toPx(), 8.dp.toPx())
    )

    // Side wall inhabitant windows
    val leftWindowColor = if (completedCount >= 2) windowGoldBright else Color.Black.copy(alpha = 0.3f)
    drawRect(
        color = leftWindowColor,
        topLeft = Offset(towerX - 10.dp.toPx(), keepBaseY - 15.dp.toPx()),
        size = Size(3.dp.toPx(), 5.dp.toPx())
    )

    val rightWindowColor = if (completedCount >= 3) windowGoldBright else Color.Black.copy(alpha = 0.3f)
    drawRect(
        color = rightWindowColor,
        topLeft = Offset(towerX + towerW + 7.dp.toPx(), keepBaseY - 13.dp.toPx()),
        size = Size(3.dp.toPx(), 5.dp.toPx())
    )

    // Keep Gate
    val gateW = 8.dp.toPx()
    val gateH = 12.dp.toPx()
    val gateX = towerX + (towerW / 2f) - (gateW / 2f)
    val gateY = keepBaseY - gateH
    
    val gateP = Path().apply {
        moveTo(gateX, keepBaseY)
        lineTo(gateX, gateY + 4.dp.toPx())
        quadraticTo(gateX + (gateW / 2f), gateY, gateX + gateW, gateY + 4.dp.toPx())
        lineTo(gateX + gateW, keepBaseY)
        close()
    }
    drawPath(gateP, Color.Black.copy(alpha = 0.5f))

    // Hanging Wall Lanterns
    if (completedCount >= 1) {
        val lX = towerX - 8.dp.toPx()
        val lY = keepBaseY - 16.dp.toPx()
        drawLine(Color.DarkGray, Offset(lX, lY - 6.dp.toPx()), Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.25f * lanternAlpha), radius = 6.dp.toPx(), center = Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(lX, lY))
    }
    if (completedCount >= 2) {
        val lX = towerX + towerW + 8.dp.toPx()
        val lY = keepBaseY - 12.dp.toPx()
        drawLine(Color.DarkGray, Offset(lX, lY - 6.dp.toPx()), Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.25f * lanternAlpha), radius = 6.dp.toPx(), center = Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(lX, lY))
    }
    if (completedCount >= 3) {
        val lX = towerX + (towerW / 2f)
        val lY = towerTop - 18.dp.toPx()
        drawLine(Color.DarkGray, Offset(lX, lY - 6.dp.toPx()), Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.25f * lanternAlpha), radius = 6.dp.toPx(), center = Offset(lX, lY))
        drawCircle(DawnGold.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(lX, lY))
    }

    // 4. Foreground Hill & Campfire Base
    val foregroundColor = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFF1E283A)
        TimeOfDay.DAY -> Color(0xFF222C39)
        TimeOfDay.DUSK -> Color(0xFF2E2525)
        TimeOfDay.NIGHT -> Color(0xFF1A1624)
    }
    foregroundHillPath.reset()
    foregroundHillPath.moveTo(0f, height)
    foregroundHillPath.lineTo(0f, height * 0.50f)
    foregroundHillPath.quadraticTo(width * 0.45f, height * 0.58f, width * 0.85f, height)
    foregroundHillPath.close()
    drawPath(foregroundHillPath, foregroundColor)

    // 5. Firewood
    val fireX = width * 0.26f
    val fireY = height * 0.53f
    drawLine(
        color = Color(0xFF332014),
        start = Offset(fireX - 6.dp.toPx(), fireY + 3.dp.toPx()),
        end = Offset(fireX + 6.dp.toPx(), fireY - 1.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = Color(0xFF332014),
        start = Offset(fireX - 4.dp.toPx(), fireY - 1.dp.toPx()),
        end = Offset(fireX + 4.dp.toPx(), fireY + 3.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )

    // 6. Companion Silhouette
    val companionColor = keepColor.copy(alpha = 0.85f)
    when (timeOfDay) {
        TimeOfDay.DAWN -> {
            val benchX = fireX + 26.dp.toPx()
            val benchY = fireY + 3.dp.toPx()
            drawLine(companionColor, Offset(benchX - 6.dp.toPx(), benchY), Offset(benchX + 6.dp.toPx(), benchY), strokeWidth = 1.5.dp.toPx())
            drawLine(companionColor, Offset(benchX - 4.dp.toPx(), benchY), Offset(benchX - 4.dp.toPx(), benchY + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
            drawLine(companionColor, Offset(benchX + 4.dp.toPx(), benchY), Offset(benchX + 4.dp.toPx(), benchY + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
            drawCircle(companionColor, radius = 2.dp.toPx(), center = Offset(benchX, benchY - 5.dp.toPx()))
            drawRoundRect(
                color = companionColor,
                topLeft = Offset(benchX - 2.5f.dp.toPx(), benchY - 3.5f.dp.toPx()),
                size = Size(5.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawCircle(DawnGold.copy(alpha = lanternAlpha), radius = 1.0.dp.toPx(), center = Offset(benchX - 1.dp.toPx(), benchY - 2.dp.toPx()))
        }
        TimeOfDay.DAY -> {
            val standX = keepLeft - 14.dp.toPx()
            val standY = keepBaseY
            drawRoundRect(
                color = companionColor,
                topLeft = Offset(standX - 2.dp.toPx(), standY - 10.dp.toPx()),
                size = Size(4.dp.toPx(), 10.dp.toPx()),
                cornerRadius = CornerRadius(1.5f.dp.toPx(), 1.5f.dp.toPx())
            )
            drawCircle(companionColor, radius = 2.dp.toPx(), center = Offset(standX, standY - 12.dp.toPx()))
        }
        TimeOfDay.DUSK -> {
            val crouchX = fireX - 16.dp.toPx()
            val crouchY = fireY + 3.dp.toPx()
            drawCircle(companionColor, radius = 1.8.dp.toPx(), center = Offset(crouchX + 1.dp.toPx(), crouchY - 5.dp.toPx()))
            val bentP = Path().apply {
                moveTo(crouchX - 2.dp.toPx(), crouchY)
                lineTo(crouchX + 2.dp.toPx(), crouchY - 4.dp.toPx())
                lineTo(crouchX + 1.dp.toPx(), crouchY - 1.dp.toPx())
                close()
            }
            drawPath(bentP, companionColor)
            drawLine(Color.DarkGray, Offset(crouchX + 1.dp.toPx(), crouchY - 2.dp.toPx()), Offset(fireX - 2.dp.toPx(), fireY + 1.dp.toPx()))
        }
        TimeOfDay.NIGHT -> {
            val wallX = towerX - 8.dp.toPx()
            val wallY = keepBaseY - 32.dp.toPx()
            drawRoundRect(
                color = companionColor,
                topLeft = Offset(wallX - 1.5f.dp.toPx(), wallY - 8.dp.toPx()),
                size = Size(3.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawCircle(companionColor, radius = 1.5.dp.toPx(), center = Offset(wallX, wallY - 9.5f.dp.toPx()))
            drawLine(Color.DarkGray, Offset(wallX + 3.dp.toPx(), wallY - 10.dp.toPx()), Offset(wallX + 3.dp.toPx(), wallY), strokeWidth = 0.8.dp.toPx())
            drawCircle(DawnGold.copy(alpha = lanternAlpha), radius = 1.5.dp.toPx(), center = Offset(wallX + 3.dp.toPx(), wallY - 10.dp.toPx()))
        }
    }

    // 7. Dawn Birds
    if (timeOfDay == TimeOfDay.DAWN) {
        val birdColor = keepColor.copy(alpha = 0.6f)
        
        // Bird 1
        val b1FlyingX = width * 0.38f
        val b1FlyingY = height * 0.15f
        val b1RestingX = towerX + (towerW / 2f) - 3.dp.toPx()
        val b1RestingY = towerTop - 16.dp.toPx() - 2.dp.toPx()
        val b1X = b1FlyingX + (b1RestingX - b1FlyingX) * bird1RestingFraction
        val b1Y = b1FlyingY + (b1RestingY - b1FlyingY) * bird1RestingFraction
        
        if (completedCount >= 1) {
            drawCircle(birdColor, radius = 1.5.dp.toPx(), center = Offset(b1X, b1Y))
        } else {
            drawLine(birdColor, start = Offset(b1X, b1Y), end = Offset(b1X - 3.dp.toPx(), b1Y - 1.5f.dp.toPx() + birdFlap))
            drawLine(birdColor, start = Offset(b1X, b1Y), end = Offset(b1X + 3.dp.toPx(), b1Y - 1.5f.dp.toPx() + birdFlap))
        }

        // Bird 2
        val b2FlyingX = width * 0.55f
        val b2FlyingY = height * 0.22f
        val b2RestingX = towerX - 10.dp.toPx()
        val b2RestingY = keepBaseY - 32.dp.toPx() - 2.dp.toPx()
        val b2X = b2FlyingX + (b2RestingX - b2FlyingX) * bird2RestingFraction
        val b2Y = b2FlyingY + (b2RestingY - b2FlyingY) * bird2RestingFraction
        
        if (completedCount >= 2) {
            drawCircle(birdColor, radius = 1.5.dp.toPx(), center = Offset(b2X, b2Y))
        } else {
            drawLine(birdColor, start = Offset(b2X, b2Y), end = Offset(b2X - 3.dp.toPx(), b2Y - 1.5f.dp.toPx() + birdFlap))
            drawLine(birdColor, start = Offset(b2X, b2Y), end = Offset(b2X + 3.dp.toPx(), b2Y - 1.5f.dp.toPx() + birdFlap))
        }

        // Bird 3
        val b3FlyingX = width * 0.72f
        val b3FlyingY = height * 0.12f
        val b3RestingX = towerX + towerW + 8.dp.toPx()
        val b3RestingY = keepBaseY - 26.dp.toPx() - 2.dp.toPx()
        val b3X = b3FlyingX + (b3RestingX - b3FlyingX) * bird3RestingFraction
        val b3Y = b3FlyingY + (b3RestingY - b3FlyingY) * bird3RestingFraction
        
        if (completedCount >= 3) {
            drawCircle(birdColor, radius = 1.5.dp.toPx(), center = Offset(b3X, b3Y))
        } else {
            drawLine(birdColor, start = Offset(b3X, b3Y), end = Offset(b3X - 3.dp.toPx(), b3Y - 1.5f.dp.toPx() + birdFlap))
            drawLine(birdColor, start = Offset(b3X, b3Y), end = Offset(b3X + 3.dp.toPx(), b3Y - 1.5f.dp.toPx() + birdFlap))
        }
    }
}
