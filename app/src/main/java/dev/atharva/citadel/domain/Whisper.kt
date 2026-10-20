package dev.atharva.citadel.domain

import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** Planning whispers help set the day's promises; watch whispers help keep them. */
enum class WhisperKind { PLAN, WATCH }

/** Where tapping a whisper takes the Commander. */
enum class WhisperRoute { HEARTH, PREPARE, RITUAL }

/**
 * The five hours at which the kingdom may speak.
 *
 * Only the first and last are set directly by the Commander — their morning and their
 * night. The three watches in between are spread proportionally across that span, so a
 * night owl's afternoon lands at a sensible hour for a night owl.
 */
enum class WhisperSlot(val title: String, val kind: WhisperKind, val position: Float) {
    DAWN("Good morning", WhisperKind.PLAN, 0.00f),
    MIDDAY("Midday check-in", WhisperKind.WATCH, 0.32f),
    AFTERNOON("Afternoon check-in", WhisperKind.WATCH, 0.62f),
    EVENING("Evening check-in", WhisperKind.WATCH, 0.86f),
    NIGHT("Plan tomorrow", WhisperKind.PLAN, 1.00f)
}

/** How often the kingdom speaks. Every option keeps both planning whispers. */
enum class WhisperCadence(val label: String, val detail: String, val slots: List<WhisperSlot>) {
    FULL(
        "5 a day",
        "Morning, midday, afternoon, evening and night.",
        WhisperSlot.entries.toList()
    ),
    STEADY(
        "4 a day",
        "Morning, afternoon, evening and night.",
        listOf(WhisperSlot.DAWN, WhisperSlot.AFTERNOON, WhisperSlot.EVENING, WhisperSlot.NIGHT)
    ),
    QUIET(
        "2 a day",
        "One in the morning to plan your day, one at night to plan tomorrow.",
        listOf(WhisperSlot.DAWN, WhisperSlot.NIGHT)
    );

    companion object {
        fun fromName(name: String?): WhisperCadence = entries.firstOrNull { it.name == name } ?: FULL
    }
}

object WhisperSchedule {

    /** The morning whisper may be set between these minutes of the day… */
    val MORNING_RANGE = (5 * 60)..(11 * 60)

    /** …and the night whisper between these. Keeping both inside one day keeps the arithmetic honest. */
    val NIGHT_RANGE = (19 * 60)..(23 * 60 + 30)

    /** The minute of the day at which [slot] speaks, rounded to five minutes. */
    fun minuteOf(slot: WhisperSlot, morningMinute: Int, nightMinute: Int): Int {
        val morning = morningMinute.coerceIn(MORNING_RANGE)
        val night = nightMinute.coerceIn(NIGHT_RANGE)
        val raw = morning + (night - morning) * slot.position
        return ((raw / 5f).roundToInt() * 5).coerceIn(0, 24 * 60 - 1)
    }

    data class Next(val slot: WhisperSlot, val at: LocalDateTime)

    /**
     * The next whisper strictly after [now]. A slot less than a minute away is treated as
     * already passed, so a whisper that has just been delivered never schedules itself.
     */
    fun next(
        now: LocalDateTime,
        cadence: WhisperCadence,
        morningMinute: Int,
        nightMinute: Int
    ): Next? {
        if (cadence.slots.isEmpty()) return null
        val threshold = now.plusMinutes(1)
        return (0L..1L).flatMap { offset ->
            val date = now.toLocalDate().plusDays(offset)
            cadence.slots.map { slot ->
                val minute = minuteOf(slot, morningMinute, nightMinute)
                Next(slot, date.atTime(minute / 60, minute % 60))
            }
        }
            .filter { it.at.isAfter(threshold) }
            .minByOrNull { it.at }
    }
}

/** One whisper, ready to post. */
data class WhisperMessage(
    val slot: WhisperSlot,
    val title: String,
    val text: String,
    val route: WhisperRoute,
    /** True when this is the day's single "every lantern is lit" message. */
    val celebrates: Boolean = false
)

/**
 * What the kingdom says.
 *
 * Rules this object must never break — and the tests check them:
 *  - No guilt. Nothing is "overdue", "missed", "late", "behind" or "failed".
 *  - No streaks, and never a count of what is left undone.
 *  - Once every lantern is lit, the day's watches say so once and then fall silent.
 */
object WhisperCopy {

    fun compose(
        slot: WhisperSlot,
        day: DayView,
        alreadyCelebrated: Boolean
    ): WhisperMessage? {
        val next = day.waiting.firstOrNull()?.let { shorten(it.title) }
        val first = day.today.firstOrNull()?.let { shorten(it.title) }
        val seed = day.todayKey

        return when (slot) {
            WhisperSlot.DAWN -> when {
                day.unwritten -> message(
                    slot, WhisperRoute.PREPARE, seed,
                    "What's one thing you want to do today?",
                    "A new day. Add a task to get started.",
                    "Nothing planned yet. One task is enough to begin."
                )
                day.allKept -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Everything is already done. The day is yours."
                )
                else -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Today: ${ordersLine(first!!, day.prepared)}",
                    "First up today: $first.",
                    "You have ${tasks(day.prepared)} today. Start with the easiest one."
                )
            }

            WhisperSlot.MIDDAY -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "Nothing planned yet. Even one small task lights a lantern.",
                    "It's midday. There's still time to add one thing."
                ),
                none = listOf(
                    "One task lights the first lantern. How about $next?",
                    "No rush, but $next would be a good one to do next."
                ),
                some = listOf(
                    "${done(day.kept).replaceFirstChar { it.uppercase() }} before noon. Next up: $next.",
                    "Nice progress, ${done(day.kept)}. Next: $next."
                ),
                all = "Everything done by midday. Enjoy the rest of it."
            )

            WhisperSlot.AFTERNOON -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "There's still time today to add one thing.",
                    "A quiet afternoon. One task would light a lantern."
                ),
                none = listOf(
                    "There's still time for $next.",
                    "$next is waiting whenever you're ready."
                ),
                some = listOf(
                    "${day.kept} of ${day.prepared} done. Your castle is brighter than this morning.",
                    "Good progress today. $next would light another lantern."
                ),
                all = "Everything is done, and the afternoon is still yours."
            )

            WhisperSlot.EVENING -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "Want to plan tomorrow tonight? It only takes a minute."
                ),
                none = listOf(
                    "Did you finish anything today? Tick it off to light a lantern.",
                    "Evening check-in. Tick off whatever you got done today."
                ),
                some = listOf(
                    "${done(day.kept).replaceFirstChar { it.uppercase() }} today. Tick off anything else you finished.",
                    "${done(day.kept).replaceFirstChar { it.uppercase() }} today. Nice work."
                ),
                all = "Everything done today. Great work.",
                unwrittenRoute = WhisperRoute.RITUAL
            )

            WhisperSlot.NIGHT -> when {
                day.tomorrow.isNotEmpty() -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Tomorrow is already planned. Rest well.",
                    "All set for tomorrow. Sleep well."
                )
                else -> message(
                    slot, WhisperRoute.RITUAL, seed,
                    "Plan tomorrow before bed? It only takes a minute.",
                    "What do you want to do tomorrow? Add it now and the morning is easier.",
                    "Take a minute to plan tomorrow, if you like."
                )
            }
        }
    }

    private fun watch(
        slot: WhisperSlot,
        day: DayView,
        alreadyCelebrated: Boolean,
        unwritten: List<String>,
        none: List<String>,
        some: List<String>,
        all: String,
        unwrittenRoute: WhisperRoute = WhisperRoute.PREPARE
    ): WhisperMessage? = when {
        day.unwritten -> message(slot, unwrittenRoute, day.todayKey, *unwritten.toTypedArray())
        // The wall is full. Say so once a day, then let the watches rest.
        day.allKept && alreadyCelebrated -> null
        day.allKept -> message(slot, WhisperRoute.HEARTH, day.todayKey, all).copy(celebrates = true)
        day.kept == 0 -> message(slot, WhisperRoute.HEARTH, day.todayKey, *none.toTypedArray())
        else -> message(slot, WhisperRoute.HEARTH, day.todayKey, *some.toTypedArray())
    }

    private fun message(
        slot: WhisperSlot,
        route: WhisperRoute,
        seed: String,
        vararg lines: String
    ): WhisperMessage {
        // Stable for a given day and slot, different across days.
        val index = abs(seed.hashCode() * 31 + slot.ordinal * 7) % lines.size
        return WhisperMessage(slot, slot.title, lines[index], route)
    }

    private fun ordersLine(first: String, prepared: Int): String = when (prepared) {
        1 -> "$first."
        2 -> "$first, and one more."
        else -> "$first, and ${prepared - 1} more."
    }

    private fun tasks(n: Int) = if (n == 1) "one task" else "$n tasks"

    private fun done(n: Int) = if (n == 1) "one done" else "$n done"

    /** Task titles are the user's own words; long ones are trimmed, never rewritten. */
    fun shorten(title: String, max: Int = 42): String {
        val clean = title.trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= max) clean else clean.take(max - 1).trimEnd() + "…"
    }

    /** For tests and previews: the words the kingdom is never allowed to use. */
    val FORBIDDEN = listOf("overdue", "missed", "failed", "fail ", "streak", "behind", "late ", "hurry")
}
