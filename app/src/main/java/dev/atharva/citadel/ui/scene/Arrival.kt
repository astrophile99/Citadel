package dev.atharva.citadel.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * The arrival.
 *
 * Opening the Citadel should feel like walking in, not like an app launching — but a
 * cinematic the Commander has already seen four hundred times is an obstacle, not an
 * atmosphere. So: a little over two seconds, once a day, interruptible by any touch.
 *
 * Every stage is a pure function of one progress value, and every consumer reads it
 * inside a draw or graphicsLayer lambda, so the whole sequence costs no recomposition.
 */
object Arrival {

    const val DEFAULT_DURATION_MS = 2200

    /** The sky is there from the first frame; only its haze burns off. */
    fun skyReveal(p: Float) = span(p, 0.00f, 0.22f)

    /** Cloud cover draws back to either side. */
    fun parting(p: Float) = ease(span(p, 0.04f, 0.52f))

    /** The land, then the Citadel on it. */
    fun landReveal(p: Float) = ease(span(p, 0.20f, 0.58f))

    /** The camera coming to rest. */
    private fun settle(p: Float) = ease(span(p, 0.30f, 0.78f))

    fun cameraScale(p: Float) = 1.055f - 0.055f * settle(p)
    fun cameraLift(p: Float) = -22f * (1f - settle(p))

    /** Heavy mist at the start, thinning as the world resolves. */
    fun mistBoost(p: Float) = 1f + 2.6f * (1f - ease(span(p, 0.10f, 0.66f)))

    fun greeting(p: Float) = ease(span(p, 0.54f, 0.80f))

    fun missions(p: Float) = ease(span(p, 0.70f, 1.00f))

    /** Promises rise out of the land rather than fading in on top of it. */
    fun missionsLift(p: Float) = 44f * (1f - missions(p))

    private fun span(p: Float, from: Float, to: Float): Float =
        ((p - from) / (to - from)).coerceIn(0f, 1f)

    private fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}

class ArrivalController(
    val progress: State<Float>,
    val skip: () -> Unit
)

@Composable
fun rememberArrival(
    play: Boolean,
    durationMs: Int = Arrival.DEFAULT_DURATION_MS,
    onFinished: () -> Unit = {}
): ArrivalController {
    val animatable = remember { Animatable(if (play) 0f else 1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(play) {
        if (!play) {
            animatable.snapTo(1f)
            return@LaunchedEffect
        }
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    return remember(animatable) {
        ArrivalController(
            progress = animatable.asState(),
            skip = {
                if (animatable.value < 1f) {
                    scope.launch {
                        // Not a cut — a quick fold forward, so skipping still lands somewhere.
                        animatable.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                        onFinished()
                    }
                }
            }
        )
    }
}
