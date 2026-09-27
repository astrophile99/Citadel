package dev.atharva.citadel.domain

import dev.atharva.citadel.data.Homecoming
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The lazy day sweep, as a pure function.
 *
 * The repository runs it when the Commander opens the app and persists the result. The
 * widget and the whispers run it too, but only to *look* — they project how today will
 * appear once the Citadel wakes, and never write. Keeping one implementation means the
 * home screen and the app can never disagree about what today holds.
 */
object DaySweep {

    data class Result(
        val data: CitadelData,
        val homecoming: Homecoming,
        val changed: Boolean
    )

    fun sweep(data: CitadelData, today: String, nowMillis: Long): Result {
        val kingdom = data.kingdom

        // First ever arrival: found the Citadel, sweep nothing.
        if (kingdom.foundedDayKey.isBlank()) {
            return Result(
                data = data.copy(
                    kingdom = kingdom.copy(foundedDayKey = today, lastActiveDayKey = today)
                ),
                homecoming = Homecoming.First,
                changed = true
            )
        }

        if (kingdom.lastActiveDayKey == today) {
            return Result(data, Homecoming.None, changed = false)
        }

        val daysAway = runCatching {
            ChronoUnit.DAYS.between(
                LocalDate.parse(kingdom.lastActiveDayKey),
                LocalDate.parse(today)
            )
        }.getOrDefault(1L).coerceAtLeast(0L).toInt()

        // Close every day that has passed, quietly.
        val closedDays = data.missions
            .filter { it.dayKey < today && it.status != MissionStatus.RESTING }
            .groupBy { it.dayKey }

        val newEntries = closedDays.map { (dayKey, missions) ->
            val kept = missions.filter { it.isComplete }.map { it.title }
            val waiting = missions.filterNot { it.isComplete }.map { it.title }
            ChronicleEntry(
                dayKey = dayKey,
                kept = kept,
                waiting = waiting,
                prepared = missions.size,
                note = closingNote(kept.size, missions.size)
            )
        }

        // Unfinished promises lie down. They are not lost.
        val rested = data.missions.map { mission ->
            if (mission.dayKey < today && mission.status == MissionStatus.ACTIVE) {
                mission.copy(status = MissionStatus.RESTING)
            } else {
                mission
            }
        }

        // Daily promises are reissued fresh, carrying no memory of the day before.
        val dailyTemplates = data.missions
            .filter { it.recurrence == Recurrence.DAILY && it.dayKey < today }
            .distinctBy { it.title.lowercase() }
        val alreadyToday = data.missions
            .filter { it.dayKey == today }
            .map { it.title.lowercase() }
            .toSet()
        val reissued = dailyTemplates
            .filterNot { it.title.lowercase() in alreadyToday }
            .map {
                Mission(
                    title = it.title,
                    impact = it.impact,
                    dayKey = today,
                    recurrence = Recurrence.DAILY,
                    createdAt = nowMillis
                )
            }

        // A reissued daily promise supersedes its resting copy, so the Chronicle holds no duplicates.
        val reissuedTitles = reissued.map { it.title.lowercase() }.toSet()
        val pruned = rested.filterNot {
            it.status == MissionStatus.RESTING &&
                it.recurrence == Recurrence.DAILY &&
                it.title.lowercase() in reissuedTitles
        }

        val mergedChronicle = (newEntries + data.chronicle)
            .distinctBy { it.dayKey }
            .sortedByDescending { it.dayKey }
            .take(MAX_CHRONICLE_DAYS)

        return Result(
            data = data.copy(
                missions = pruned + reissued,
                chronicle = mergedChronicle,
                kingdom = kingdom.copy(lastActiveDayKey = today)
            ),
            homecoming = when {
                daysAway >= 7 -> Homecoming.LongAway(daysAway)
                daysAway >= 2 -> Homecoming.Away(daysAway)
                else -> Homecoming.NewDay
            },
            changed = true
        )
    }

    /**
     * How the Citadel will look once today has been woken, without waking it. Used by the
     * widget and the whispers, which are allowed to read but never to write.
     */
    fun project(data: CitadelData, today: String, nowMillis: Long): CitadelData =
        sweep(data, today, nowMillis).data

    private fun closingNote(kept: Int, prepared: Int): String = when {
        prepared == 0 -> "A still day. The walls kept themselves."
        kept == 0 -> "A quiet day. The hearth stayed lit regardless."
        kept == prepared && kept >= 3 -> "Every promise kept. The lanterns burned late."
        kept == prepared -> "All that was set down was done."
        kept >= 3 -> "Good work along the walls."
        else -> "Some ground gained. That is how ground is gained."
    }

    /** Roughly two years of days. Older entries fade; nothing recent is ever thrown away. */
    private const val MAX_CHRONICLE_DAYS = 730
}
