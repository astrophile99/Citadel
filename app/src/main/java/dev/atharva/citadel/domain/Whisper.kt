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
    DAWN("Dawn has arrived", WhisperKind.PLAN, 0.00f),
    MIDDAY("The midday watch", WhisperKind.WATCH, 0.32f),
    AFTERNOON("The afternoon watch", WhisperKind.WATCH, 0.62f),
    EVENING("Evening at the hearth", WhisperKind.WATCH, 0.86f),
    NIGHT("The night watch", WhisperKind.PLAN, 1.00f)
}

/** How often the kingdom speaks. Every option keeps both planning whispers. */
enum class WhisperCadence(val label: String, val detail: String, val slots: List<WhisperSlot>) {
    FULL(
        "Through the day",
        "Five whispers: dawn, midday, afternoon, evening and night.",
        WhisperSlot.entries.toList()
    ),
    STEADY(
        "Steady",
        "Four whispers: dawn, afternoon, evening and night.",
        listOf(WhisperSlot.DAWN, WhisperSlot.AFTERNOON, WhisperSlot.EVENING, WhisperSlot.NIGHT)
    ),
    QUIET(
        "Quiet",
        "Two whispers: one to plan the day, one to prepare tomorrow.",
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
                    "Name one thing worth protecting today. The gates are open.",
                    "A fresh morning over the Citadel. What will you set down today?",
                    "The scouts are waiting for today's orders. One is enough to begin."
                )
                day.allKept -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Every lantern is already lit. The day is entirely yours."
                )
                else -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Today's orders: ${ordersLine(first!!, day.prepared)} The gates are open.",
                    "Dawn over the Citadel. First on the wall today: $first.",
                    "${promises(day.prepared).replaceFirstChar { it.uppercase() }} set down for today. Begin with whichever feels lightest."
                )
            }

            WhisperSlot.MIDDAY -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "The day is still unwritten. Even one small promise lights a lantern.",
                    "Midday over the Citadel. There's still room to name one thing."
                ),
                none = listOf(
                    "One lantern changes the whole valley. $next would light the first.",
                    "Midday, and nothing is urgent — but $next would warm the walls."
                ),
                some = listOf(
                    "${lanterns(day.kept)} lit before noon. Next on the wall: $next.",
                    "The walls are warming — ${lanterns(day.kept)} lit. $next is up next."
                ),
                all = "Every lantern lit by midday. Go and enjoy the rest of it."
            )

            WhisperSlot.AFTERNOON -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "The afternoon light is long. It isn't too late to name one thing.",
                    "A quiet afternoon over the Citadel. One promise would light a lantern."
                ),
                none = listOf(
                    "The afternoon light is long. There's time yet for $next.",
                    "The afternoon watch has begun. $next is waiting whenever you are."
                ),
                some = listOf(
                    "${day.kept} of ${day.prepared} lanterns lit. The walls are warmer than they were at dawn.",
                    "Good ground gained today. $next would light another lantern."
                ),
                all = "Every lantern is lit, and the afternoon is still yours."
            )

            WhisperSlot.EVENING -> watch(
                slot, day, alreadyCelebrated,
                unwritten = listOf(
                    "The fire is stoked for the evening. Tomorrow can be planned tonight, if you like."
                ),
                none = listOf(
                    "The fire is stoked. If anything got done today, come and light its lantern.",
                    "Evening at the hearth. Come and mark whatever you kept today."
                ),
                some = listOf(
                    "The fire is stoked — ${lanterns(day.kept)} already lit. Come and mark the rest of what you kept.",
                    "Evening at the hearth. ${lanterns(day.kept).replaceFirstChar { it.uppercase() }} burn along the wall tonight."
                ),
                all = "Every lantern is lit tonight. Well marched, Commander.",
                unwrittenRoute = WhisperRoute.RITUAL
            )

            WhisperSlot.NIGHT -> when {
                day.tomorrow.isNotEmpty() -> message(
                    slot, WhisperRoute.HEARTH, seed,
                    "Tomorrow already has its orders. Rest now — we begin again at dawn.",
                    "The gates are ready for morning. Sleep well, Commander."
                )
                else -> message(
                    slot, WhisperRoute.RITUAL, seed,
                    "The scouts await tomorrow's orders. A minute now makes the morning lighter.",
                    "Before you sleep: what will tomorrow protect? The ritual takes a minute.",
                    "The watch is changing. Tomorrow can be given a shape, if you like."
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

    private fun promises(n: Int) = if (n == 1) "one promise" else "$n promises"

    private fun lanterns(n: Int) = if (n == 1) "one lantern" else "$n lanterns"

    /** Mission titles are the Commander's own words; long ones are trimmed, never rewritten. */
    fun shorten(title: String, max: Int = 42): String {
        val clean = title.trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= max) clean else clean.take(max - 1).trimEnd() + "…"
    }

    /** For tests and previews: the words the kingdom is never allowed to use. */
    val FORBIDDEN = listOf("overdue", "missed", "failed", "fail ", "streak", "behind", "late ", "hurry")
}
