package dev.atharva.citadel.ui.scene

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.atharva.citadel.core.time.SkyMoment

/**
 * The colours of the world at one instant.
 *
 * Built by blending a daylight set with a night set by [SkyMoment.nightFactor], then
 * laying the sunrise/sunset warmth on top. Because both inputs are continuous, so is the
 * result — there is no hour at which the Citadel visibly changes costume.
 */
@Immutable
data class SceneColors(
    val zenith: Color,
    val skyMid: Color,
    val horizon: Color,
    val glow: Color,
    val glowStrength: Float,
    val mountainFar: Color,
    val mountainNear: Color,
    val ridge: Color,
    val stone: Color,
    val foreground: Color,
    val starAlpha: Float,
    val mistTint: Color
) {
    companion object {

        // Daylight — still dark. The Citadel lives in a perpetual late blue hour.
        private val DAY_ZENITH = Color(0xFF0C121C)
        private val DAY_MID = Color(0xFF17202F)
        private val DAY_HORIZON = Color(0xFF2A3D54)
        private val DAY_MTN_FAR = Color(0xFF223148)
        private val DAY_MTN_NEAR = Color(0xFF1A2537)
        private val DAY_RIDGE = Color(0xFF141C29)
        private val DAY_STONE = Color(0xFF0E141E)
        private val DAY_FOREGROUND = Color(0xFF0A0F17)

        private val NIGHT_ZENITH = Color(0xFF04030A)
        private val NIGHT_MID = Color(0xFF0A0814)
        private val NIGHT_HORIZON = Color(0xFF171226)
        private val NIGHT_MTN_FAR = Color(0xFF161228)
        private val NIGHT_MTN_NEAR = Color(0xFF100D1D)
        private val NIGHT_RIDGE = Color(0xFF0A0813)
        private val NIGHT_STONE = Color(0xFF06050C)
        private val NIGHT_FOREGROUND = Color(0xFF040309)

        private val SUNRISE_GLOW = Color(0xFFD9924A)
        private val SUNSET_GLOW = Color(0xFFCF5E31)

        fun of(sky: SkyMoment): SceneColors {
            val n = sky.nightFactor
            val warmth = sky.horizonWarmth

            // Morning warmth is golden; evening warmth is redder.
            val morning = sky.minuteOfDay < 12 * 60
            val glowColor = if (morning) SUNRISE_GLOW else SUNSET_GLOW

            val horizonBase = lerp(DAY_HORIZON, NIGHT_HORIZON, n)

            return SceneColors(
                zenith = lerp(DAY_ZENITH, NIGHT_ZENITH, n),
                skyMid = lerp(DAY_MID, NIGHT_MID, n),
                // The horizon takes on the colour of the sun as it touches it.
                horizon = lerp(horizonBase, glowColor, warmth * 0.55f),
                glow = glowColor,
                glowStrength = warmth,
                mountainFar = lerp(DAY_MTN_FAR, NIGHT_MTN_FAR, n),
                mountainNear = lerp(DAY_MTN_NEAR, NIGHT_MTN_NEAR, n),
                ridge = lerp(DAY_RIDGE, NIGHT_RIDGE, n),
                stone = lerp(DAY_STONE, NIGHT_STONE, n),
                foreground = lerp(DAY_FOREGROUND, NIGHT_FOREGROUND, n),
                starAlpha = ((n - 0.25f) / 0.6f).coerceIn(0f, 1f),
                mistTint = lerp(Color(0xFFBFD0E4), Color(0xFF6E6C90), n)
            )
        }
    }
}
