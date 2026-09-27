package dev.atharva.citadel.ui.scene

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.atharva.citadel.domain.WorldState

/**
 * The world.
 *
 * Not a header, not a card, not a hero image with a list underneath — this fills the
 * entire window, behind the system bars and behind every promise, and the mission list
 * is written onto the land rather than placed below it.
 *
 * Performance shape worth preserving: every animated input is read *inside* the
 * `drawBehind` lambda. Motion, scroll and the arrival therefore invalidate the draw
 * phase only. Nothing in this composable recomposes while the world is moving.
 */
@Composable
fun KingdomWorld(
    world: WorldState,
    arrivalProgress: State<Float>,
    recession: () -> Float,
    parallaxPx: () -> Float,
    bloom: State<Float>,
    veilColor: Color,
    veilTop: Float,
    stillness: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = remember(world.sky.minuteOfDay / 3) { SceneColors.of(world.sky) }
    val ambients = remember(
        world.sky.phase,
        world.wildness > 0.35f,
        world.lightFraction > 0f,
        world.hasBirds
    ) { dominantAmbients(world) }

    val stars = remember { StarField(count = 110) }
    val skyScratch = remember { SkyScratch() }
    val landScratch = remember { LandScratch() }

    val moving = world.ambience && !stillness
    val time by rememberAmbientTime(enabled = moving)

    // The world reacts rather than snapping. A lantern easing on over a second reads as
    // something happening in a place; the same lantern appearing instantly reads as a boolean.
    val light = animateFloatAsState(world.lightFraction, tween(1400), label = "light")
    val lanterns = animateFloatAsState(world.lanternsLit.toFloat(), tween(1100), label = "lanterns")
    val wild = animateFloatAsState(world.wildness, tween(2200), label = "wildness")
    val warmth = animateFloatAsState(world.villageWarmth, tween(1800), label = "warmth")

    val description = remember(world.keptToday, world.preparedToday, world.sky.phase) {
        describe(world)
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .graphicsLayer {
                val p = arrivalProgress.value
                val scale = Arrival.cameraScale(p)
                scaleX = scale
                scaleY = scale
                translationY = Arrival.cameraLift(p) + parallaxPx()
            }
            .drawBehind {
                val p = arrivalProgress.value
                val t = if (moving) time else STILL_FRAME
                val skyIn = Arrival.skyReveal(p)
                val landIn = Arrival.landReveal(p)

                val horizonY = size.height * SCENE_HORIZON
                val animated = world.copy(
                    lightFraction = light.value,
                    lanternsLit = lanterns.value.toInt(),
                    wildness = wild.value,
                    villageWarmth = warmth.value
                )

                drawSky(
                    sky = world.sky,
                    colors = colors,
                    horizonY = horizonY,
                    stars = stars,
                    scratch = skyScratch,
                    time = t,
                    ambients = ambients,
                    reveal = skyIn
                )

                // Clouds draw apart during the arrival, as though the view is descending through them.
                if (p < 1f) {
                    drawArrivalCover(colors, horizonY, Arrival.parting(p))
                }

                drawLand(
                    world = animated,
                    colors = colors,
                    horizonY = horizonY,
                    scratch = landScratch,
                    time = t,
                    ambients = ambients,
                    reveal = landIn
                )

                if (Ambient.MIST in ambients) {
                    val dawnMist = if (world.sky.phase == dev.atharva.citadel.core.time.DayPhase.DAWN) 0.18f else 0f
                    drawMist(
                        colors = colors,
                        horizonY = horizonY,
                        density = (wild.value + dawnMist).coerceIn(0f, 1f) * Arrival.mistBoost(p),
                        time = t,
                        reveal = landIn
                    )
                }

                drawBloom(bloom.value, horizonY)

                // The single recession. Scrolling pulls the surface up over the land;
                // nothing is dimmed twice and nothing ends at a hard edge.
                val r = recession()
                drawVeil(
                    veilColor = veilColor,
                    startFraction = lerp(veilTop, -0.05f, r),
                    endFraction = lerp(veilTop + 0.30f, 0.22f, r)
                )
            }
    )
}

/**
 * The cover the Commander descends through on arrival: a soft bank of cloud that draws
 * back to both sides rather than simply fading, so the world feels entered rather than shown.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrivalCover(
    colors: SceneColors,
    horizonY: Float,
    parting: Float
) {
    if (parting >= 1f) return
    val w = size.width
    val opacity = (1f - parting)
    val spread = w * 0.62f * parting

    // Two banks, sliding apart. Each puff is a radial falloff rather than a flat disc —
    // hard-edged circles read as overlapping shapes, which is the one thing cloud must not do.
    val tint = colors.mistTint
    for (i in 0 until 6) {
        val y = horizonY * (0.06f + i * 0.15f)
        val r = w * (0.30f - i * 0.016f)
        val a = 0.42f * opacity * (1f - i * 0.06f)
        softPuff(Offset(w * 0.24f - spread, y), r, tint, a, stretch = 1.3f)
        softPuff(Offset(w * 0.76f + spread, y), r * 0.94f, tint, a, stretch = 1.3f)
    }
    // A last veil of haze over everything, burning off as the world resolves.
    drawRect(colors.zenith.copy(alpha = 0.72f * opacity))
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

/**
 * What the world looks like, for a Commander who is listening rather than looking.
 * TalkBack should get the state of the kingdom, not "graphic".
 */
internal fun describe(world: WorldState): String {
    val phase = when (world.sky.phase) {
        dev.atharva.citadel.core.time.DayPhase.DAWN -> "Dawn"
        dev.atharva.citadel.core.time.DayPhase.DAY -> "Daylight"
        dev.atharva.citadel.core.time.DayPhase.DUSK -> "Dusk"
        dev.atharva.citadel.core.time.DayPhase.NIGHT -> "Night"
    }
    val lights = when {
        world.lanternsLit == 0 -> "no lanterns lit yet"
        world.lanternsLit == 1 -> "one lantern lit along the wall"
        else -> "${world.lanternsLit} lanterns lit along the wall"
    }
    val weather = if (world.wildness > 0.35f) ", mist over the valley" else ""
    return "$phase over the Citadel, $lights$weather."
}

/** The horizon sits high enough that the land, not the list, owns the opening view. */
internal const val SCENE_HORIZON = 0.455f

/** When motion is off, the world is frozen at a deliberately flattering instant. */
internal const val STILL_FRAME = 12.4f
