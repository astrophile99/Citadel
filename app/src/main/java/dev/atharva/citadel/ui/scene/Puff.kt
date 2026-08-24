package dev.atharva.citadel.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

/**
 * The one primitive every soft thing in the sky is built from.
 *
 * Cloud, mist and the arrival cover were each drawn with flat-alpha circles at first, and
 * every one of them read as a pile of overlapping discs rather than as air. A radial
 * falloff costs one brush and fixes all three, so they all come through here now.
 *
 * [stretch] flattens the puff horizontally — fog lies along a valley, it does not float.
 */
internal fun DrawScope.softPuff(
    center: Offset,
    radius: Float,
    tint: Color,
    alpha: Float,
    stretch: Float = 1f,
    core: Float = 0.45f
) {
    if (alpha <= 0.004f || radius <= 0f) return

    val draw: DrawScope.() -> Unit = {
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to tint.copy(alpha = alpha),
                core to tint.copy(alpha = alpha * 0.62f),
                1.00f to Color.Transparent,
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }

    if (stretch == 1f) draw() else scale(scaleX = stretch, scaleY = 1f, pivot = center) { draw() }
}
