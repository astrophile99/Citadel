package dev.atharva.citadel

import dev.atharva.citadel.core.time.CitadelClock
import dev.atharva.citadel.data.CitadelRepository
import dev.atharva.citadel.data.Homecoming
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.data.store.CitadelStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The promises the Citadel makes to the Commander, as tests.
 *
 * These are the rules that must never regress, because breaking any of them turns the
 * app back into the kind of thing it exists to replace.
 */
class DaySweepTest {

    private class FixedClock(var moment: LocalDateTime) : CitadelClock {
        override fun now(): LocalDateTime = moment
        override fun epochMillis(): Long = 1_700_000_000_000L
    }

    private class MemoryStore(var data: CitadelData = CitadelData.Empty) : CitadelStorage {
        override suspend fun read(): CitadelData = data
        override suspend fun write(data: CitadelData) {
            this.data = data
        }
    }

    private fun mission(
        title: String,
        dayKey: String,
        status: MissionStatus = MissionStatus.ACTIVE,
        recurrence: Recurrence = Recurrence.ONCE
    ) = Mission(
        id = title,
        title = title,
        impact = MissionImpact.MODERATE,
        status = status,
        dayKey = dayKey,
        recurrence = recurrence
    )

    @Test
    fun `unfinished promises rest instead of disappearing`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(
                    mission("Call the bank", "2026-03-10"),
                    mission("Walk", "2026-03-10", MissionStatus.COMPLETE)
                ),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-10"
                )
            )
        )
        val repository = CitadelRepository(store, clock)

        repository.awaken()

        val unfinished = repository.data.value.missions.first { it.title == "Call the bank" }
        assertEquals(MissionStatus.RESTING, unfinished.status)
        // It is still there. Nothing the passage of time does can delete a promise.
        assertNotNull(repository.data.value.missions.firstOrNull { it.title == "Call the bank" })
    }

    @Test
    fun `a closed day becomes a chronicle entry that names what was kept`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(
                    mission("Walk", "2026-03-10", MissionStatus.COMPLETE),
                    mission("Read", "2026-03-10")
                ),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-10"
                )
            )
        )
        val repository = CitadelRepository(store, clock)

        repository.awaken()

        val entry = repository.data.value.chronicle.first { it.dayKey == "2026-03-10" }
        assertEquals(listOf("Walk"), entry.kept)
        assertEquals(listOf("Read"), entry.waiting)
        // The note is written in the Citadel's voice, never as a score.
        assertFalse(entry.note.contains("fail", ignoreCase = true))
        assertFalse(entry.note.contains("missed", ignoreCase = true))
        assertFalse(entry.note.contains("overdue", ignoreCase = true))
    }

    @Test
    fun `daily promises are reissued fresh for the new day`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 6, 30))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(
                    mission("Drink water", "2026-03-10", recurrence = Recurrence.DAILY)
                ),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-10"
                )
            )
        )
        val repository = CitadelRepository(store, clock)

        repository.awaken()

        val today = repository.data.value.missionsFor("2026-03-11")
        assertEquals(1, today.size)
        assertEquals("Drink water", today.first().title)
        assertEquals(MissionStatus.ACTIVE, today.first().status)
        // And the old copy does not linger in the Chronicle as a second, waiting version.
        assertTrue(repository.data.value.resting().none { it.title == "Drink water" })
    }

    @Test
    fun `permanent progress survives an absence`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 4, 20, 10, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(mission("Old thing", "2026-03-10")),
                kingdom = KingdomState(
                    foundedDayKey = "2026-01-01",
                    totalKept = 84,
                    daysProtected = 31,
                    lastActiveDayKey = "2026-03-10"
                )
            )
        )
        val repository = CitadelRepository(store, clock)

        repository.awaken()

        val kingdom = repository.data.value.kingdom
        assertEquals(84, kingdom.totalKept)
        assertEquals(31, kingdom.daysProtected)
        assertEquals("2026-01-01", kingdom.foundedDayKey)
        assertTrue(repository.homecoming.value is Homecoming.LongAway)
    }

    @Test
    fun `continuing a resting promise brings it to today without penalty`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(mission("Call the bank", "2026-03-04", MissionStatus.RESTING)),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)
        repository.awaken()

        repository.continueMission("Call the bank")

        val carried = repository.data.value.missionsFor("2026-03-11").first()
        assertEquals(MissionStatus.ACTIVE, carried.status)
        assertEquals("2026-03-11", carried.dayKey)
        assertEquals(1, carried.carried)
    }

    @Test
    fun `un-keeping a promise never reduces permanent progress`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(mission("Walk", "2026-03-11")),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)
        repository.awaken()

        repository.setKept("Walk", true)
        val afterKeeping = repository.data.value.kingdom
        repository.setKept("Walk", false)
        val afterUndo = repository.data.value.kingdom

        assertEquals(1, afterKeeping.totalKept)
        assertEquals(1, afterKeeping.daysProtected)
        assertEquals(afterKeeping.totalKept, afterUndo.totalKept)
        assertEquals(afterKeeping.daysProtected, afterUndo.daysProtected)
    }

    @Test
    fun `a day protected counts once no matter how many promises are kept`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(
                    mission("Walk", "2026-03-11"),
                    mission("Read", "2026-03-11"),
                    mission("Cook", "2026-03-11")
                ),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)
        repository.awaken()

        repository.setKept("Walk", true)
        repository.setKept("Read", true)
        repository.setKept("Cook", true)

        assertEquals(3, repository.data.value.kingdom.totalKept)
        assertEquals(1, repository.data.value.kingdom.daysProtected)
    }

    @Test
    fun `re-keeping the same promise does not inflate the totals`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(mission("Walk", "2026-03-11")),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)
        repository.awaken()

        // A mis-tap, a correction, and then keeping it for real.
        repository.setKept("Walk", true)
        repository.setKept("Walk", false)
        repository.setKept("Walk", true)
        repository.setKept("Walk", false)
        repository.setKept("Walk", true)

        val kingdom = repository.data.value.kingdom
        assertEquals(1, kingdom.totalKept)
        assertEquals(1, kingdom.daysProtected)
    }

    @Test
    fun `a second day protected still counts`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(
                    mission("Walk", "2026-03-11"),
                    mission("Read", "2026-03-12")
                ),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)
        repository.awaken()
        repository.setKept("Walk", true)

        // The Commander comes back the next day and keeps another.
        clock.moment = LocalDateTime.of(2026, 3, 12, 9, 0)
        repository.setKept("Read", true)

        val kingdom = repository.data.value.kingdom
        assertEquals(2, kingdom.totalKept)
        assertEquals(2, kingdom.daysProtected)
    }

    @Test
    fun `opening twice on the same day sweeps nothing`() = runBlocking {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 11, 9, 0))
        val store = MemoryStore(
            CitadelData(
                missions = listOf(mission("Walk", "2026-03-11")),
                kingdom = KingdomState(
                    foundedDayKey = "2026-03-01",
                    lastActiveDayKey = "2026-03-11"
                )
            )
        )
        val repository = CitadelRepository(store, clock)

        repository.awaken()
        repository.awaken()

        assertEquals(MissionStatus.ACTIVE, repository.data.value.missions.first().status)
        assertEquals(Homecoming.None, repository.homecoming.value)
    }
}
