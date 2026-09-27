package dev.atharva.citadel.domain

import dev.atharva.citadel.core.time.DayPhase
import dev.atharva.citadel.data.Homecoming
import kotlin.math.abs

/**
 * The Guardian speaks rarely and briefly.
 *
 * Three rules hold this whole file together:
 *  - Never mention what was missed.
 *  - Never mention counts, streaks, or the app itself.
 *  - When there is nothing worth saying, say nothing.
 *
 * Lines are chosen deterministically from the day key, so the Guardian keeps the same
 * thought for a whole day instead of shuffling every time the screen recomposes.
 */
object GuardianVoice {

    // ---- the headline the Commander is greeted with ---------------------------------

    fun greeting(phase: DayPhase, homecoming: Homecoming): String = when (homecoming) {
        is Homecoming.First -> "The gates are open, Commander."
        is Homecoming.LongAway -> "Welcome home, Commander."
        is Homecoming.Away -> "Welcome back, Commander."
        else -> when (phase) {
            DayPhase.DAWN -> "Good morning, Commander."
            DayPhase.DAY -> "The day is yours, Commander."
            DayPhase.DUSK -> "Welcome home, Commander."
            DayPhase.NIGHT -> "The watch is set, Commander."
        }
    }

    /** One line under the greeting describing the state of today, never the state of the user. */
    fun subtitle(phase: DayPhase, world: WorldState, homecoming: Homecoming): String {
        when (homecoming) {
            is Homecoming.First ->
                return "This is your Citadel. It will be here whenever you are."
            is Homecoming.LongAway ->
                return "The mist came in while you were gone. It always lifts."
            is Homecoming.Away ->
                return "Nothing was lost. The hearth kept itself."
            else -> Unit
        }

        return when {
            world.dayIsUnwritten && phase == DayPhase.NIGHT ->
                "Nothing is set down for today. That is allowed."
            world.dayIsUnwritten ->
                "Nothing is set down yet. Name what matters and the rest can wait."
            world.everythingKept && phase == DayPhase.NIGHT ->
                "Everything you set down is done. Rest properly."
            world.everythingKept ->
                "Everything you set down is done. The walls are bright."
            world.keptToday > 0 && phase == DayPhase.NIGHT ->
                "The lanterns are lit along the wall. Let the rest wait for morning."
            world.keptToday > 0 && phase == DayPhase.DAWN ->
                "A lantern is already lit. The day has started well."
            world.keptToday > 0 ->
                "The wall is warmer than it was this morning."
            phase == DayPhase.NIGHT ->
                "The gates are open whenever you return."
            phase == DayPhase.DUSK ->
                "The fire is stoked. Come and sit when you are ready."
            else ->
                "Here is what you chose to protect today."
        }
    }

    // ---- the quiet observation ------------------------------------------------------

    /**
     * The Guardian's thought for the day. Stable for a whole day, chosen from the phase
     * so the same kingdom feels different at dawn and at midnight.
     */
    fun thought(phase: DayPhase, dayKey: String, world: WorldState): String {
        if (world.wildness > 0.5f) return pick(WILD, dayKey, phase.ordinal)
        if (world.everythingKept) return pick(FULFILLED, dayKey, phase.ordinal)
        return when (phase) {
            DayPhase.DAWN -> pick(DAWN, dayKey, 0)
            DayPhase.DAY -> pick(DAY, dayKey, 1)
            DayPhase.DUSK -> pick(DUSK, dayKey, 2)
            DayPhase.NIGHT -> pick(NIGHT, dayKey, 3)
        }
    }

    /**
     * Something worth remarking on just happened. Most of the time this returns null,
     * and that silence is the feature.
     */
    fun moment(event: GuardianMoment, dayKey: String): String = when (event) {
        GuardianMoment.FirstLight -> pick(FIRST_LIGHT, dayKey, 11)
        GuardianMoment.AllKept -> pick(ALL_KEPT, dayKey, 13)
        GuardianMoment.TomorrowSet -> pick(TOMORROW_SET, dayKey, 17)
    }

    // ---- the ritual -----------------------------------------------------------------

    fun ritualInvitation(preparedForTomorrow: Int, suggested: Int): String = when {
        preparedForTomorrow == 0 -> "The scouts are waiting for tomorrow's orders."
        preparedForTomorrow < suggested -> "The road is taking shape."
        else -> "The gates are ready for morning."
    }

    fun ritualClosing(preparedForTomorrow: Int): String = when {
        preparedForTomorrow == 0 -> "Nothing set down. Tomorrow can decide for itself."
        preparedForTomorrow == 1 -> "One promise for the morning. That is a real plan."
        else -> "Rest now. We begin again at dawn."
    }

    // ---- lines ----------------------------------------------------------------------

    private val DAWN = listOf(
        "The kettle is on. Warm your hands before the march.",
        "Mist is coming off the meadows. It will burn away by noon.",
        "The scouts came back with clear roads. Nothing waiting in ambush.",
        "Morning is the cheapest hour to spend. Spend a little of it well.",
        "The gate was opened before you woke. Someone always does."
    )

    private val DAY = listOf(
        "The walls hold. They mostly just need someone to walk them.",
        "A deep well gives slowly. Draw from it slowly.",
        "Wind from the east today. Good weather for hard things.",
        "One thing at a time is not slow. It is how walls get built.",
        "The kingdom is quiet, which is what a kingdom should be."
    )

    private val DUSK = listOf(
        "The sun is off the ridge. Put your boots by the fire.",
        "Whatever the day took, the evening gives a little back.",
        "The light goes, the hearth stays. That has always been the arrangement.",
        "The watch is changing at the gates. Yours can change too.",
        "Long marches end the same way as short ones. Sitting down."
    )

    private val NIGHT = listOf(
        "The stars are steady tonight. They keep watch better than I do.",
        "Sleep fortifies more than stone does.",
        "The embers will hold until morning. They usually do.",
        "Nothing needs your attention until dawn. Nothing.",
        "The Citadel is safe tonight, Commander. Put it down."
    )

    private val WILD = listOf(
        "The ivy got ambitious while you were away. It always does.",
        "Nothing fell down. It only got quiet.",
        "The road back is shorter than the road out. It always is.",
        "The fire was banked, not out. It only needs a breath.",
        "The gates recognised your step before I did."
    )

    private val FULFILLED = listOf(
        "Everything you named is done. Sit down.",
        "The wall is fully lit. There is nothing else asked of you today.",
        "A clean day. Those are rarer than they should be.",
        "That is the whole list. Go and be somewhere else.",
        "Well marched, Commander. The rest of the evening is yours."
    )

    private val FIRST_LIGHT = listOf(
        "The first lantern is lit.",
        "That one counted. The mist is already thinning.",
        "There it is. The hard part was starting.",
        "One light along the wall changes the whole valley."
    )

    private val ALL_KEPT = listOf(
        "Every promise kept. The lanterns will burn late tonight.",
        "The wall is lit end to end.",
        "That is all of it. Close the app, Commander.",
        "Nothing left on the board. Go live the rest of it."
    )

    private val TOMORROW_SET = listOf(
        "The gates are ready. Rest now — we begin again at dawn.",
        "The orders are with the scouts. Sleep.",
        "Tomorrow has a shape. That is enough for tonight.",
        "The watch is set for the night, Commander."
    )

    /**
     * A stable choice for a given day. Not random at runtime — the same day always
     * produces the same line, so the Guardian does not stutter across recompositions.
     */
    private fun pick(lines: List<String>, dayKey: String, salt: Int): String {
        if (lines.isEmpty()) return ""
        val hash = abs(dayKey.hashCode() * 31 + salt * 131)
        return lines[hash % lines.size]
    }
}

enum class GuardianMoment { FirstLight, AllKept, TomorrowSet }
