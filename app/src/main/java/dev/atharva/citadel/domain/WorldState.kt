package dev.atharva.citadel.domain

import androidx.compose.runtime.Immutable
import dev.atharva.citadel.core.time.SkyMoment
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.Mission
import kotlin.math.min

/**
 * Everything the world needs to draw itself, and nothing else.
 *
 * The scene never sees a Mission. It sees light, warmth and weather — which is the
 * whole point: the Commander's real work arrives in the kingdom as atmosphere.
 */
@Immutable
data class WorldState(
    val sky: SkyMoment,
    val keptToday: Int = 0,
    val preparedToday: Int = 0,
    /** Today's progress, weighted by impact. Drives the fire, the windows, the mist. */
    val lightFraction: Float = 0f,
    /** Wall lanterns burning tonight. One per promise kept, up to the length of the wall. */
    val lanternsLit: Int = 0,
    /** Long-term consistency, 0..1. Grows over weeks and never falls. */
    val villageWarmth: Float = 0f,
    /** How far nature has crept back in during an absence. Never destruction — only quiet. */
    val wildness: Float = 0f,
    val ambience: Boolean = true
) {
    val everythingKept: Boolean get() = preparedToday > 0 && keptToday == preparedToday
    val dayIsUnwritten: Boolean get() = preparedToday == 0

    // Long-term life in the world, earned slowly and kept forever.
    val hasSprouts: Boolean get() = villageWarmth >= 0.20f
    val hasFlowers: Boolean get() = villageWarmth >= 0.45f
    val hasBirds: Boolean get() = villageWarmth >= 0.68f
    val hasHound: Boolean get() = villageWarmth >= 0.90f

    companion object {
        const val WALL_LANTERNS = 7

        fun from(
            data: CitadelData,
            sky: SkyMoment,
            todayKey: String,
            nowMillis: Long,
            ambience: Boolean
        ): WorldState {
            val today = data.missionsFor(todayKey)
            val kept = today.filter { it.isComplete }
            val light = lightOf(today, kept)

            return WorldState(
                sky = sky,
                keptToday = kept.size,
                preparedToday = today.size,
                lightFraction = light,
                lanternsLit = min(kept.size, WALL_LANTERNS),
                villageWarmth = warmthOf(data.kingdom.daysProtected),
                wildness = wildnessOf(data.kingdom.lastKeptAtMillis, nowMillis, light),
                ambience = ambience
            )
        }

        /**
         * Progress weighted by impact, so an Expedition moves the world more than a Skirmish.
         * A day with nothing prepared is not a day at zero — it is simply a day at rest.
         */
        private fun lightOf(today: List<Mission>, kept: List<Mission>): Float {
            if (today.isEmpty()) return 0f
            val total = today.sumOf { it.impact.worldWeight.toDouble() }.toFloat()
            if (total <= 0f) return 0f
            val earned = kept.sumOf { it.impact.worldWeight.toDouble() }.toFloat()
            return (earned / total).coerceIn(0f, 1f)
        }

        /**
         * A month of protected days brings the village fully to life. The curve is deliberately
         * slow early and generous later — the reward for staying is that the world stays warm.
         */
        private fun warmthOf(daysProtected: Int): Float {
            if (daysProtected <= 0) return 0f
            val t = (daysProtected / 30f).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        /**
         * Absence brings weather, not ruin. It peaks below 1 because the Citadel is never lost,
         * and the first promise kept today clears most of it immediately.
         */
        private fun wildnessOf(lastKeptAt: Long, nowMillis: Long, light: Float): Float {
            if (lastKeptAt <= 0L) return 0f
            val days = ((nowMillis - lastKeptAt) / MILLIS_PER_DAY).toFloat()
            val base = when {
                days < 1.5f -> 0f
                days >= 8f -> 0.85f
                else -> ((days - 1.5f) / 6.5f) * 0.85f
            }
            // Keeping even one promise blows most of the mist off the meadows.
            return (base * (1f - light * 1.8f)).coerceIn(0f, 0.85f)
        }

        private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24
    }
}
