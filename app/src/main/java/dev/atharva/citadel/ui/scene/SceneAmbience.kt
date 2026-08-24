package dev.atharva.citadel.ui.scene

import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.atharva.citadel.core.time.DayPhase
import dev.atharva.citadel.domain.WorldState

/**
 * Ambient motion in the Citadel comes from exactly one clock.
 *
 * The previous scene ran seventeen independent infinite animations, several of which
 * kept invalidating the canvas to drive effects that were not being drawn. Here a single
 * float advances with the frame clock and every drifting, flickering, breathing thing is
 * a pure function of it — one state write per frame, no matter how much is moving.
 *
 * The value is only ever read inside draw lambdas, so motion never triggers recomposition.
 */
@Composable
fun rememberAmbientTime(enabled: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameMillis { millis ->
                time.floatValue = millis / 1000f
            }
        }
    }
    return time
}

/**
 * True when the Commander has asked the system to remove animations.
 *
 * An app built around calm should be the first to honour this, not the last.
 */
@Composable
fun rememberSystemWantsStillness(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/** The things that can be moving in the world at any moment. */
enum class Ambient { CLOUDS, STARS, MIST, EMBERS, SMOKE, BIRDS }

/**
 * The Dominant State System.
 *
 * Never more than two moving layers at once. The world should breathe, not perform —
 * a single cloud crossing an empty sky reads as more alive than six effects competing,
 * and it is the difference between atmosphere and a screensaver.
 */
fun dominantAmbients(world: WorldState): Set<Ambient> {
    // Weather always wins. If the valley has gone quiet, that is the story being told.
    if (world.wildness > 0.35f) {
        return if (world.lightFraction > 0f) setOf(Ambient.MIST, Ambient.SMOKE) else setOf(Ambient.MIST)
    }

    val hearthIsLit = world.lightFraction > 0f

    return when (world.sky.phase) {
        // Morning mist coming off the meadows, and the birds if they have been earned.
        DayPhase.DAWN ->
            if (world.hasBirds) setOf(Ambient.MIST, Ambient.BIRDS) else setOf(Ambient.MIST, Ambient.SMOKE)

        DayPhase.DAY ->
            if (hearthIsLit) setOf(Ambient.CLOUDS, Ambient.SMOKE) else setOf(Ambient.CLOUDS)

        // Evening belongs to the fire.
        DayPhase.DUSK ->
            if (hearthIsLit) setOf(Ambient.EMBERS, Ambient.SMOKE) else setOf(Ambient.CLOUDS, Ambient.EMBERS)

        DayPhase.NIGHT ->
            if (hearthIsLit) setOf(Ambient.STARS, Ambient.EMBERS) else setOf(Ambient.STARS)
    }
}
