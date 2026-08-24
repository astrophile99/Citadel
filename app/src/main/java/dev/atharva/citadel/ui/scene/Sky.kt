package dev.atharva.citadel.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.atharva.citadel.core.time.SkyMoment
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A fixed arrangement of stars, generated once and then permanent.
 *
 * The night sky over the Citadel is the same night sky every night. Re-rolling it on
 * every composition would make the constellations crawl, which nobody would consciously
 * notice and everybody would subconsciously distrust.
 */
class StarField(count: Int, seed: Int = 41) {
    val x = FloatArray(count)
    val y = FloatArray(count)
    val radius = FloatArray(count)
    val phase = FloatArray(count)
    val twinkles = BooleanArray(count)

    init {
        val random = Random(seed)
        for (i in 0 until count) {
            x[i] = random.nextFloat()
            // Weighted towards the top of the sky, thinning out near the horizon haze.
            y[i] = random.nextFloat() * random.nextFloat()
            radius[i] = 0.6f + random.nextFloat() * 1.5f
            phase[i] = random.nextFloat() * 6.283f
            // Only a handful twinkle. A sky where every star pulses looks like a fault.
            twinkles[i] = random.nextFloat() < 0.22f
        }
    }
}

/** Reusable scratch paths, so nothing is allocated inside the draw loop. */
class SkyScratch {
    val moonBase = Path()
    val moonCutter = Path()
    val moon = Path()
}

fun DrawScope.drawSky(
    sky: SkyMoment,
    colors: SceneColors,
    horizonY: Float,
    stars: StarField,
    scratch: SkyScratch,
    time: Float,
    ambients: Set<Ambient>,
    reveal: Float
) {
    val w = size.width
    val h = size.height

    // ---- the gradient itself ----
    drawRect(
        brush = Brush.verticalGradient(
            0f to colors.zenith,
            0.45f to colors.skyMid,
            1f to colors.horizon,
            startY = 0f,
            endY = horizonY * 1.05f
        ),
        size = Size(w, horizonY * 1.05f)
    )

    // ---- stars ----
    if (colors.starAlpha > 0.01f && Ambient.STARS in ambients) {
        val fade = colors.starAlpha * reveal
        for (i in stars.x.indices) {
            val sx = stars.x[i] * w
            val sy = stars.y[i] * horizonY * 0.92f
            val twinkle = if (stars.twinkles[i]) {
                0.55f + 0.45f * sin(time * 0.7f + stars.phase[i])
            } else {
                1f
            }
            // Stars fade out towards the horizon haze rather than stopping at a line.
            val heightFade = (1f - (sy / (horizonY * 0.95f))).coerceIn(0f, 1f)
            val alpha = fade * twinkle * (0.25f + 0.75f * heightFade)
            if (alpha <= 0.02f) continue
            drawCircle(
                color = Color(0xFFFFF8E7).copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = stars.radius[i],
                center = Offset(sx, sy)
            )
        }
    }

    // ---- the sun and the moon, on the same arc, half a day apart ----
    val arcTop = horizonY * 0.16f
    if (sky.sunVisible > 0.01f) {
        val p = arcPosition(sky.sunTrack, w, horizonY, arcTop)
        drawCelestial(
            center = p,
            radius = 26f,
            core = Color(0xFFFFF3D4),
            halo = colors.glow,
            alpha = sky.sunVisible * reveal,
            haloScale = 4.2f + colors.glowStrength * 3f
        )
    }
    if (sky.moonVisible > 0.01f) {
        val p = arcPosition(sky.moonTrack, w, horizonY, arcTop)
        drawMoon(p, 20f, sky.moonVisible * reveal, scratch)
    }

    // ---- the warm band the sun leaves sitting on the horizon ----
    if (colors.glowStrength > 0.01f) {
        val anchorX = if (sky.sunVisible > 0.01f) {
            arcPosition(sky.sunTrack, w, horizonY, arcTop).x
        } else {
            w * 0.5f
        }
        drawRect(
            brush = Brush.radialGradient(
                0f to colors.glow.copy(alpha = 0.30f * colors.glowStrength * reveal),
                0.5f to colors.glow.copy(alpha = 0.11f * colors.glowStrength * reveal),
                1f to Color.Transparent,
                center = Offset(anchorX, horizonY),
                radius = w * 0.85f
            ),
            topLeft = Offset(0f, horizonY - h * 0.30f),
            size = Size(w, h * 0.32f)
        )
    }

    // ---- clouds ----
    if (Ambient.CLOUDS in ambients) {
        drawClouds(w, horizonY, colors, time, reveal)
    }
}

private fun arcPosition(track: Float, width: Float, horizonY: Float, arcTop: Float): Offset {
    val t = track.coerceIn(0f, 1f)
    val x = width * (0.10f + 0.80f * t)
    val lift = sin(t * PI).toFloat()
    val y = horizonY - (horizonY - arcTop) * lift
    return Offset(x, y)
}

private fun DrawScope.drawCelestial(
    center: Offset,
    radius: Float,
    core: Color,
    halo: Color,
    alpha: Float,
    haloScale: Float
) {
    if (alpha <= 0.01f) return
    drawCircle(
        brush = Brush.radialGradient(
            0f to halo.copy(alpha = 0.34f * alpha),
            0.45f to halo.copy(alpha = 0.10f * alpha),
            1f to Color.Transparent,
            center = center,
            radius = radius * haloScale
        ),
        radius = radius * haloScale,
        center = center
    )
    drawCircle(color = core.copy(alpha = alpha), radius = radius, center = center)
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, alpha: Float, scratch: SkyScratch) {
    if (alpha <= 0.01f) return

    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFBFC6E8).copy(alpha = 0.26f * alpha),
            0.5f to Color(0xFF8E8CC4).copy(alpha = 0.08f * alpha),
            1f to Color.Transparent,
            center = center,
            radius = radius * 5f
        ),
        radius = radius * 5f,
        center = center
    )

    // A waxing crescent, cut once and reused. No path arithmetic per frame beyond this.
    scratch.moonBase.reset()
    scratch.moonBase.addOval(
        Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    )
    scratch.moonCutter.reset()
    val cx = center.x + radius * 0.46f
    val cy = center.y - radius * 0.16f
    scratch.moonCutter.addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
    scratch.moon.reset()
    scratch.moon.op(scratch.moonBase, scratch.moonCutter, PathOperation.Difference)

    drawPath(scratch.moon, Color(0xFFE8EAF2).copy(alpha = alpha))
}

/**
 * Cloud. Built from soft puffs with a flattened underside, drifting at three depths so
 * the sky has somewhere to be — never more than a suggestion of weather.
 */
private fun DrawScope.drawClouds(
    width: Float,
    horizonY: Float,
    colors: SceneColors,
    time: Float,
    reveal: Float
) {
    val bank = arrayOf(
        CloudBank(y = 0.20f, scale = 1.0f, speed = 0.0060f, alpha = 0.070f, offset = 0.10f),
        CloudBank(y = 0.34f, scale = 0.72f, speed = 0.0038f, alpha = 0.052f, offset = 0.62f),
        CloudBank(y = 0.50f, scale = 1.25f, speed = 0.0021f, alpha = 0.038f, offset = 0.35f)
    )

    for (cloud in bank) {
        // Travels a full screen width plus its own size, then wraps.
        val span = width * 1.6f
        val raw = (cloud.offset + time * cloud.speed) % 1f
        val x = -width * 0.3f + raw * span
        val y = horizonY * cloud.y
        val r = width * 0.075f * cloud.scale
        val a = cloud.alpha * reveal
        val tint = Color.White

        softPuff(Offset(x, y), r * 1.00f, tint, a, stretch = 1.7f)
        softPuff(Offset(x + r * 0.95f, y + r * 0.06f), r * 1.30f, tint, a, stretch = 1.8f)
        softPuff(Offset(x + r * 2.00f, y + r * 0.02f), r * 0.90f, tint, a, stretch = 1.6f)
        softPuff(Offset(x + r * 0.45f, y - r * 0.42f), r * 0.66f, tint, a * 0.8f, stretch = 1.4f)
        softPuff(Offset(x + r * 1.55f, y - r * 0.36f), r * 0.58f, tint, a * 0.8f, stretch = 1.4f)
    }
}

private class CloudBank(
    val y: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float,
    val offset: Float
)

/** Small helper for gentle periodic motion without reaching for an animator. */
internal fun breathe(time: Float, period: Float, phase: Float = 0f): Float =
    0.5f + 0.5f * sin((time / period + phase) * 2f * PI.toFloat())

internal fun drift(time: Float, period: Float, phase: Float = 0f): Float =
    cos((time / period + phase) * 2f * PI.toFloat())
