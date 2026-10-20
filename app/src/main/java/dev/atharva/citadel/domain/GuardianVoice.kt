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

    // ---- the headline you are greeted with ------------------------------------------

    fun greeting(phase: DayPhase, homecoming: Homecoming): String = when (homecoming) {
        is Homecoming.First -> "Welcome to Citadel."
        is Homecoming.LongAway -> "Welcome back."
        is Homecoming.Away -> "Welcome back."
        else -> when (phase) {
            DayPhase.DAWN -> "Good morning."
            DayPhase.DAY -> "The day is yours."
            DayPhase.DUSK -> "Good evening."
            DayPhase.NIGHT -> "Good night."
        }
    }

    /** One line under the greeting describing the state of today, never the state of the user. */
    fun subtitle(phase: DayPhase, world: WorldState, homecoming: Homecoming): String {
        when (homecoming) {
            is Homecoming.First ->
                return "This is your castle. Add a task, and finishing it lights the first lantern."
            is Homecoming.LongAway ->
                return "Some mist rolled in while you were away. It clears as you get going."
            is Homecoming.Away ->
                return "Nothing was lost while you were away."
            else -> Unit
        }

        return when {
            world.dayIsUnwritten && phase == DayPhase.NIGHT ->
                "Nothing planned for today. That's okay."
            world.dayIsUnwritten ->
                "Nothing planned yet. Add one thing that matters."
            world.everythingKept && phase == DayPhase.NIGHT ->
                "Everything is done. Get some rest."
            world.everythingKept ->
                "Everything is done. Your castle is glowing."
            world.keptToday > 0 && phase == DayPhase.NIGHT ->
                "Lanterns are lit. The rest can wait until morning."
            world.keptToday > 0 && phase == DayPhase.DAWN ->
                "A lantern is already lit. Great start."
            world.keptToday > 0 ->
                "Your castle is brighter than it was this morning."
            phase == DayPhase.NIGHT ->
                "Come back whenever you're ready."
            phase == DayPhase.DUSK ->
                "The fire is lit. Finish what you can."
            else ->
                "Here's what you planned for today."
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
        preparedForTomorrow == 0 -> "Want to plan tomorrow?"
        preparedForTomorrow < suggested -> "Tomorrow is taking shape."
        else -> "Tomorrow is ready."
    }

    fun ritualClosing(preparedForTomorrow: Int): String = when {
        preparedForTomorrow == 0 -> "Nothing planned yet. That's okay too."
        preparedForTomorrow == 1 -> "One task for tomorrow. That's a real plan."
        else -> "Rest well. See you in the morning."
    }

    // ---- lines ----------------------------------------------------------------------
    // Plain, everyday words. Testers asked for language anyone can read at a glance.

    private val DAWN = listOf(
        "Start with something small.",
        "Mornings are a good time for the hard task.",
        "One task at a time is enough.",
        "A fresh start. Pick one thing to begin with.",
        "No rush. Just begin."
    )

    private val DAY = listOf(
        "Keep going. You're doing fine.",
        "Slow and steady still gets there.",
        "Take a short break, then do one more thing.",
        "Small steps build big walls.",
        "It's a calm day in the castle."
    )

    private val DUSK = listOf(
        "The day is winding down. Be kind to yourself.",
        "Whatever happened today, the evening is yours.",
        "Finish one more thing, or rest. Both are fine.",
        "Time to slow down.",
        "A good time to plan tomorrow."
    )

    private val NIGHT = listOf(
        "Rest well. Tomorrow is a new day.",
        "Sleep helps more than you think.",
        "Nothing needs you until morning.",
        "The castle is safe tonight. You can put your phone down.",
        "Good night. You did enough today."
    )

    private val WILD = listOf(
        "Welcome back. Nothing fell down.",
        "It got a little quiet while you were away. That's all.",
        "Coming back is the hardest part. You did it.",
        "Start small. The castle wakes up fast.",
        "Good to see you again."
    )

    private val FULFILLED = listOf(
        "Everything is done. Enjoy the rest of your day.",
        "All your lanterns are lit. Nothing else to do today.",
        "A great day. Well done.",
        "That's everything. Go relax.",
        "All done. The evening is yours."
    )

    private val FIRST_LIGHT = listOf(
        "The first lantern is lit!",
        "Nice! That one counts.",
        "There it is. Starting is the hardest part.",
        "One done. The castle is a little brighter."
    )

    private val ALL_KEPT = listOf(
        "All done! The lanterns will glow all night.",
        "Every lantern is lit.",
        "That's all of it. Great work today.",
        "Nothing left. Go enjoy your day."
    )

    private val TOMORROW_SET = listOf(
        "Tomorrow is planned. Rest well.",
        "All set for tomorrow. Sleep well.",
        "Tomorrow has a plan. That's enough for tonight.",
        "Done planning. Good night."
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
