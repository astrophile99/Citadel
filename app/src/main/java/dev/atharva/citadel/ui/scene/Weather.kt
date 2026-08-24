package dev.atharva.citadel.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Mist over the meadows.
 *
 * Mist is the only thing in the Citadel that reports on absence, and it is deliberately
 * the gentlest possible way to do it: nothing is broken, nothing is missing, the valley
 * has simply gone quiet. One kept promise blows most of it away.
 */
fun DrawScope.drawMist(
    colors: SceneColors,
    horizonY: Float,
    density: Float,
    time: Float,
    reveal: Float
) {
    if (density <= 0.01f) return
    val w = size.width
    val h = size.height
    val strength = density * reveal

    // Three bands of valley air, all sitting at or below the foot of the walls so the
    // keep is never fogged out. Each puff is a soft radial falloff stretched wide — fog
    // is horizontal, and a row of circles reads as bubbles rather than weather.
    val bands = arrayOf(
        MistBand(y = horizonY + h * 0.052f, radius = h * 0.030f, speed = 0.0030f, alpha = 0.30f, phase = 0.0f),
        MistBand(y = horizonY + h * 0.082f, radius = h * 0.040f, speed = -0.0019f, alpha = 0.34f, phase = 0.4f),
        MistBand(y = horizonY + h * 0.118f, radius = h * 0.050f, speed = 0.0012f, alpha = 0.24f, phase = 0.7f)
    )

    for (band in bands) {
        val travel = ((band.phase + time * band.speed) % 1f + 1f) % 1f
        val step = w * 0.34f
        var x = -step + travel * step
        var i = 0
        while (x < w + step) {
            val bob = sin((time * 0.14f + i) * PI.toFloat()) * h * 0.003f
            val radius = band.radius * (0.85f + 0.15f * sin(i * 1.7f))
            val centre = Offset(x, band.y + bob)
            // Stretched five to one: a bank of air lying along the valley floor.
            softPuff(centre, radius, colors.mistTint, band.alpha * strength, stretch = 5f)
            x += step * 0.55f
            i++
        }
    }
}

private class MistBand(
    val y: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float,
    val phase: Float
)

/**
 * The dissolve.
 *
 * This is the single most important gradient in the app. The world does not end at an
 * edge and the mission list does not begin at one — the land simply fades into the
 * surface the promises are written on, so the two are one continuous place rather than
 * an illustration with a list stapled underneath it.
 */
fun DrawScope.drawVeil(
    veilColor: Color,
    startFraction: Float,
    endFraction: Float,
    strength: Float = 1f
) {
    if (strength <= 0.005f) return
    val h = size.height
    val startY = h * startFraction
    val endY = h * endFraction
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to veilColor.copy(alpha = 0f),
            0.38f to veilColor.copy(alpha = 0.55f * strength),
            0.72f to veilColor.copy(alpha = 0.92f * strength),
            1.00f to veilColor.copy(alpha = strength),
            startY = startY,
            endY = endY
        ),
        topLeft = Offset(0f, startY),
        size = Size(size.width, (endY - startY).coerceAtLeast(0f))
    )
    // Everything below the dissolve is simply the surface.
    if (endY < h) {
        drawRect(
            color = veilColor.copy(alpha = strength),
            topLeft = Offset(0f, endY),
            size = Size(size.width, h - endY)
        )
    }
}

/**
 * A promise was just kept.
 *
 * A single warm breath of light that expands out of the Citadel and is gone in about a
 * second. No burst, no particles, no sound — the point is that the world noticed, not
 * that the app celebrated.
 */
fun DrawScope.drawBloom(progress: Float, horizonY: Float) {
    if (progress <= 0f || progress >= 1f) return
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.68f, horizonY + h * 0.02f)

    val eased = 1f - (1f - progress) * (1f - progress)
    val radius = w * (0.10f + 0.75f * eased)
    val alpha = (1f - progress) * 0.30f

    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFD98A).copy(alpha = alpha * 0.55f),
            0.45f to Color(0xFFD4A84F).copy(alpha = alpha * 0.22f),
            1f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
