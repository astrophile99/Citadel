package dev.atharva.citadel.data

import dev.atharva.citadel.core.time.CitadelClock
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.data.store.CitadelStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The single source of truth for the Citadel.
 *
 * Two rules are enforced here rather than in the UI, so no screen can ever break them:
 *  1. Nothing is destroyed by the passage of time. Unfinished promises become
 *     [MissionStatus.RESTING], never deleted, and can always be continued.
 *  2. [dev.atharva.citadel.data.model.KingdomState] only ever moves forward.
 */
class CitadelRepository(
    private val store: CitadelStorage,
    private val clock: CitadelClock
) {
    private val mutex = Mutex()

    private val _data = MutableStateFlow(CitadelData.Empty)
    val data: StateFlow<CitadelData> = _data.asStateFlow()

    private val _homecoming = MutableStateFlow<Homecoming>(Homecoming.None)

    /** Set once by the day sweep, describing how long the Commander has been away. */
    val homecoming: StateFlow<Homecoming> = _homecoming.asStateFlow()

    /**
     * Wakes the Citadel.
     *
     * The day sweep runs here, on open, rather than from a midnight background job.
     * Android will not reliably wake an app at midnight and it does not need to —
     * the kingdom simply notices what day it is when the Commander arrives.
     */
    suspend fun awaken() = mutex.withLock {
        val loaded = store.read()
        val swept = sweep(loaded)
        _data.value = swept.data
        _homecoming.value = swept.homecoming
        if (swept.changed) store.write(swept.data)
    }

    // ---- promises -------------------------------------------------------------------

    suspend fun prepare(
        title: String,
        impact: MissionImpact,
        recurrence: Recurrence,
        dayKey: String = clock.todayKey()
    ) = edit { data ->
        val clean = title.trim()
        if (clean.isEmpty()) return@edit data
        val mission = Mission(
            title = clean,
            impact = impact,
            dayKey = dayKey,
            recurrence = recurrence,
            createdAt = clock.epochMillis()
        )
        data.copy(missions = data.missions + mission)
    }

    suspend fun setKept(missionId: String, kept: Boolean) = edit { data ->
        val existing = data.missions.firstOrNull { it.id == missionId } ?: return@edit data
        if (existing.isComplete == kept) return@edit data

        val now = clock.epochMillis()
        val today = clock.todayKey()
        val updated = existing.copy(
            status = if (kept) MissionStatus.COMPLETE else MissionStatus.ACTIVE,
            completedAt = if (kept) now else null,
            counted = existing.counted || kept
        )

        val kingdom = if (kept) {
            // Both totals are idempotent. A promise counts once in its life, and a day
            // counts once in its day — otherwise un-checking and re-checking a single
            // promise would quietly inflate a record that is supposed to be trustworthy.
            val firstTimeForThisPromise = !existing.counted
            val firstTimeToday = data.kingdom.lastProtectedDayKey != today
            data.kingdom.copy(
                totalKept = data.kingdom.totalKept + if (firstTimeForThisPromise) 1 else 0,
                daysProtected = data.kingdom.daysProtected + if (firstTimeToday) 1 else 0,
                lastProtectedDayKey = today,
                lastKeptAtMillis = now
            )
        } else {
            // Un-checking corrects a mis-tap. It never claws back permanent progress.
            data.kingdom
        }

        data.copy(
            missions = data.missions.map { if (it.id == missionId) updated else it },
            kingdom = kingdom
        )
    }

    suspend fun revise(
        missionId: String,
        title: String,
        impact: MissionImpact,
        recurrence: Recurrence
    ) = edit { data ->
        val clean = title.trim()
        if (clean.isEmpty()) return@edit data
        data.copy(
            missions = data.missions.map {
                if (it.id == missionId) {
                    it.copy(title = clean, impact = impact, recurrence = recurrence)
                } else {
                    it
                }
            }
        )
    }

    /** Letting a promise go. The Commander chooses this, and it is the only way anything is removed. */
    suspend fun release(missionId: String) = edit { data ->
        data.copy(missions = data.missions.filterNot { it.id == missionId })
    }

    /**
     * Brings a resting promise back to today. Not a recovery and not a penalty —
     * the Commander simply is not finished with it yet.
     */
    suspend fun continueMission(missionId: String, dayKey: String = clock.todayKey()) = edit { data ->
        val resting = data.missions.firstOrNull { it.id == missionId } ?: return@edit data
        data.copy(
            missions = data.missions.map {
                if (it.id == missionId) {
                    resting.copy(
                        status = MissionStatus.ACTIVE,
                        dayKey = dayKey,
                        completedAt = null,
                        carried = resting.carried + 1
                    )
                } else {
                    it
                }
            }
        )
    }

    /** Puts a promise down for now without deleting it. It waits in the Chronicle. */
    suspend fun setAside(missionId: String) = edit { data ->
        data.copy(
            missions = data.missions.map {
                if (it.id == missionId) it.copy(status = MissionStatus.RESTING) else it
            }
        )
    }

    suspend fun markArrived() = edit { data ->
        if (data.kingdom.hasArrived) data else data.copy(kingdom = data.kingdom.copy(hasArrived = true))
    }

    fun dismissHomecoming() {
        _homecoming.value = Homecoming.None
    }

    private suspend fun edit(transform: (CitadelData) -> CitadelData) = mutex.withLock {
        val next = transform(_data.value)
        if (next != _data.value) {
            _data.value = next
            store.write(next)
        }
    }

    // ---- the lazy day sweep ---------------------------------------------------------

    private data class SweepResult(
        val data: CitadelData,
        val homecoming: Homecoming,
        val changed: Boolean
    )

    private fun sweep(data: CitadelData): SweepResult {
        val today = clock.todayKey()
        val kingdom = data.kingdom

        // First ever arrival: found the Citadel, sweep nothing.
        if (kingdom.foundedDayKey.isBlank()) {
            return SweepResult(
                data = data.copy(
                    kingdom = kingdom.copy(foundedDayKey = today, lastActiveDayKey = today)
                ),
                homecoming = Homecoming.First,
                changed = true
            )
        }

        if (kingdom.lastActiveDayKey == today) {
            return SweepResult(data, Homecoming.None, changed = false)
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
                    createdAt = clock.epochMillis()
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

        return SweepResult(
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

    private fun closingNote(kept: Int, prepared: Int): String = when {
        prepared == 0 -> "A still day. The walls kept themselves."
        kept == 0 -> "A quiet day. The hearth stayed lit regardless."
        kept == prepared && kept >= 3 -> "Every promise kept. The lanterns burned late."
        kept == prepared -> "All that was set down was done."
        kept >= 3 -> "Good work along the walls."
        else -> "Some ground gained. That is how ground is gained."
    }

    private companion object {
        /** Roughly two years of days. Older entries fade; nothing recent is ever thrown away. */
        const val MAX_CHRONICLE_DAYS = 730
    }
}

/** How long the Commander has been gone. Used only to choose a warmer greeting. */
sealed interface Homecoming {
    data object None : Homecoming
    data object First : Homecoming
    data object NewDay : Homecoming
    data class Away(val days: Int) : Homecoming
    data class LongAway(val days: Int) : Homecoming
}
