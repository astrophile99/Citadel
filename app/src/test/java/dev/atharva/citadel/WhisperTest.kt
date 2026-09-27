package dev.atharva.citadel

import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.domain.DaySweep
import dev.atharva.citadel.domain.DayView
import dev.atharva.citadel.domain.WhisperCadence
import dev.atharva.citadel.domain.WhisperCopy
import dev.atharva.citadel.domain.WhisperRoute
import dev.atharva.citadel.domain.WhisperSchedule
import dev.atharva.citadel.domain.WhisperSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WhisperScheduleTest {

    private val morning = 7 * 60 + 30
    private val night = 21 * 60

    @Test
    fun `the five whispers are spread across the Commander's day in order`() {
        val minutes = WhisperSlot.entries.map { WhisperSchedule.minuteOf(it, morning, night) }
        assertEquals(morning, minutes.first())
        assertEquals(night, minutes.last())
        assertEquals(minutes.sorted(), minutes)
        assertEquals(5, minutes.distinct().size)
        // Rounded to five minutes, so nobody gets a whisper at 11:47.
        assertTrue(minutes.all { it % 5 == 0 })
    }

    @Test
    fun `cadences keep both planning whispers`() {
        WhisperCadence.entries.forEach { cadence ->
            assertTrue(WhisperSlot.DAWN in cadence.slots)
            assertTrue(WhisperSlot.NIGHT in cadence.slots)
        }
        assertEquals(5, WhisperCadence.FULL.slots.size)
        assertEquals(4, WhisperCadence.STEADY.slots.size)
        assertEquals(2, WhisperCadence.QUIET.slots.size)
    }

    @Test
    fun `the next whisper is the soonest one still ahead`() {
        val next = WhisperSchedule.next(LocalDateTime.of(2026, 3, 11, 8, 0), WhisperCadence.FULL, morning, night)
        assertNotNull(next)
        assertEquals(WhisperSlot.MIDDAY, next!!.slot)
        assertEquals(11, next.at.dayOfMonth)
    }

    @Test
    fun `after the night whisper the next is tomorrow's dawn`() {
        val next = WhisperSchedule.next(LocalDateTime.of(2026, 3, 11, 22, 0), WhisperCadence.FULL, morning, night)
        assertEquals(WhisperSlot.DAWN, next!!.slot)
        assertEquals(12, next.at.dayOfMonth)
        assertEquals(7, next.at.hour)
        assertEquals(30, next.at.minute)
    }

    @Test
    fun `a whisper that has just fired never schedules itself again`() {
        val justFired = LocalDateTime.of(2026, 3, 11, 7, 30, 20)
        val next = WhisperSchedule.next(justFired, WhisperCadence.FULL, morning, night)
        assertEquals(WhisperSlot.MIDDAY, next!!.slot)
    }

    @Test
    fun `the quiet cadence skips the watches`() {
        val next = WhisperSchedule.next(LocalDateTime.of(2026, 3, 11, 8, 0), WhisperCadence.QUIET, morning, night)
        assertEquals(WhisperSlot.NIGHT, next!!.slot)
    }

    @Test
    fun `out of range times are held inside the day`() {
        assertEquals(WhisperSchedule.MORNING_RANGE.first, WhisperSchedule.minuteOf(WhisperSlot.DAWN, 60, night))
        assertEquals(WhisperSchedule.NIGHT_RANGE.last, WhisperSchedule.minuteOf(WhisperSlot.NIGHT, morning, 24 * 60 + 90))
    }
}

class WhisperCopyTest {

    private val today = "2026-03-11"

    private fun mission(title: String, kept: Boolean = false, day: String = today) = Mission(
        id = title + day,
        title = title,
        status = if (kept) MissionStatus.COMPLETE else MissionStatus.ACTIVE,
        dayKey = day
    )

    private fun dayOf(vararg missions: Mission) = DayView.of(CitadelData(missions = missions.toList()), today)

    private val shapes = listOf(
        dayOf(),
        dayOf(mission("Walk before dark")),
        dayOf(mission("Walk before dark", kept = true), mission("Read twenty pages")),
        dayOf(mission("Walk before dark", kept = true), mission("Read twenty pages", kept = true)),
        dayOf(mission("Walk"), mission("Call home", day = "2026-03-12"))
    )

    @Test
    fun `the kingdom never uses guilt, in any slot, in any state`() {
        for (slot in WhisperSlot.entries) {
            for (day in shapes) {
                for (celebrated in listOf(false, true)) {
                    val message = WhisperCopy.compose(slot, day, celebrated) ?: continue
                    val text = (message.title + " " + message.text).lowercase() + " "
                    WhisperCopy.FORBIDDEN.forEach { word ->
                        assertFalse("'$word' in $slot: ${message.text}", text.contains(word))
                    }
                    assertTrue(message.text.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `an empty day at dawn opens the prepare sheet`() {
        val message = WhisperCopy.compose(WhisperSlot.DAWN, dayOf(), alreadyCelebrated = false)
        assertEquals(WhisperRoute.PREPARE, message!!.route)
    }

    @Test
    fun `the night whisper opens the ritual when tomorrow is empty`() {
        val message = WhisperCopy.compose(WhisperSlot.NIGHT, dayOf(mission("Walk")), alreadyCelebrated = false)
        assertEquals(WhisperRoute.RITUAL, message!!.route)
    }

    @Test
    fun `the night whisper rests when tomorrow is already prepared`() {
        val day = dayOf(mission("Walk"), mission("Call home", day = "2026-03-12"))
        val message = WhisperCopy.compose(WhisperSlot.NIGHT, day, alreadyCelebrated = false)
        assertEquals(WhisperRoute.HEARTH, message!!.route)
        assertTrue(message.text.contains("Rest") || message.text.contains("Sleep"))
    }

    @Test
    fun `a full wall is celebrated once, then the watches fall silent`() {
        val full = dayOf(mission("Walk", kept = true), mission("Read", kept = true))
        val first = WhisperCopy.compose(WhisperSlot.AFTERNOON, full, alreadyCelebrated = false)
        assertNotNull(first)
        assertTrue(first!!.celebrates)
        assertNull(WhisperCopy.compose(WhisperSlot.EVENING, full, alreadyCelebrated = true))
        // Planning whispers still speak: the night is about tomorrow, not today.
        assertNotNull(WhisperCopy.compose(WhisperSlot.NIGHT, full, alreadyCelebrated = true))
    }

    @Test
    fun `watches name the next promise in the Commander's own words`() {
        val message = WhisperCopy.compose(WhisperSlot.MIDDAY, dayOf(mission("Walk before dark")), false)
        assertTrue(message!!.text.contains("Walk before dark"))
    }

    @Test
    fun `long titles are trimmed, never rewritten`() {
        val long = "Finish the quarterly report and send it to everyone on the review list"
        val short = WhisperCopy.shorten(long)
        assertTrue(short.length <= 42)
        assertTrue(short.endsWith("…"))
        assertTrue(long.startsWith(short.dropLast(1)))
    }

    @Test
    fun `the same day always produces the same words`() {
        val day = dayOf(mission("Walk"))
        assertEquals(
            WhisperCopy.compose(WhisperSlot.AFTERNOON, day, false),
            WhisperCopy.compose(WhisperSlot.AFTERNOON, day, false)
        )
    }
}

class ProjectionTest {

    @Test
    fun `projection shows last night's plan this morning without writing anything`() {
        val data = CitadelData(
            missions = listOf(
                Mission(id = "a", title = "Walk", dayKey = "2026-03-12"),
                Mission(id = "b", title = "Stretch", dayKey = "2026-03-11", recurrence = Recurrence.DAILY)
            ),
            kingdom = KingdomState(foundedDayKey = "2026-03-01", lastActiveDayKey = "2026-03-11")
        )
        val projected = DaySweep.project(data, "2026-03-12", 0L)
        val day = DayView.of(projected, "2026-03-12")

        assertEquals(setOf("Walk", "Stretch"), day.today.map { it.title }.toSet())
        // The original is untouched; only the app's own open may persist a sweep.
        assertEquals("2026-03-11", data.kingdom.lastActiveDayKey)
    }
}
