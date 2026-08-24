package dev.atharva.citadel

import dev.atharva.citadel.core.time.DayPhase
import dev.atharva.citadel.core.time.SkyMoment
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.domain.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyMomentTest {

    @Test
    fun `the sky never jumps`() {
        // Continuity is the whole reason the clock is a curve rather than four buckets:
        // if any minute-to-minute step is visible, the illusion of real light is gone.
        var previous = SkyMoment.at(0)
        for (minute in 1 until 1440) {
            val current = SkyMoment.at(minute)
            val step = kotlin.math.abs(current.nightFactor - previous.nightFactor)
            assertTrue("night factor jumped at minute $minute by $step", step < 0.05f)
            previous = current
        }
    }

    @Test
    fun `phases land where the copy expects them`() {
        assertEquals(DayPhase.NIGHT, SkyMoment.at(2 * 60).phase)
        assertEquals(DayPhase.DAWN, SkyMoment.at(7 * 60).phase)
        assertEquals(DayPhase.DAY, SkyMoment.at(13 * 60).phase)
        assertEquals(DayPhase.DUSK, SkyMoment.at(19 * 60).phase)
        assertEquals(DayPhase.NIGHT, SkyMoment.at(23 * 60).phase)
    }

    @Test
    fun `it is dark at night and light at noon`() {
        assertEquals(1f, SkyMoment.at(3 * 60).nightFactor, 0.001f)
        assertEquals(0f, SkyMoment.at(12 * 60).nightFactor, 0.001f)
    }

    @Test
    fun `the horizon is warmest at sunrise and sunset`() {
        val dawn = SkyMoment.at(6 * 60).horizonWarmth
        val noon = SkyMoment.at(12 * 60).horizonWarmth
        val dusk = SkyMoment.at(18 * 60 + 30).horizonWarmth
        assertTrue(dawn > 0.9f)
        assertTrue(dusk > 0.9f)
        assertTrue(noon < 0.1f)
    }

    @Test
    fun `the ritual hours cover the evening and the very small hours`() {
        assertTrue(SkyMoment.at(21 * 60).isRitualHour)
        assertTrue(SkyMoment.at(1 * 60).isRitualHour)
        assertTrue(!SkyMoment.at(11 * 60).isRitualHour)
    }
}

class WorldStateTest {

    private val noon = SkyMoment.at(12 * 60)
    private val dayMillis = 1000L * 60 * 60 * 24

    private fun mission(title: String, kept: Boolean, impact: MissionImpact = MissionImpact.MODERATE) =
        Mission(
            id = title,
            title = title,
            impact = impact,
            status = if (kept) MissionStatus.COMPLETE else MissionStatus.ACTIVE,
            dayKey = "2026-03-11"
        )

    @Test
    fun `light is weighted by impact, so an expedition moves the world more`() {
        val world = WorldState.from(
            data = CitadelData(
                missions = listOf(
                    mission("Big", kept = true, impact = MissionImpact.MAJOR),
                    mission("Small", kept = false, impact = MissionImpact.MINOR)
                )
            ),
            sky = noon,
            todayKey = "2026-03-11",
            nowMillis = 0L,
            ambience = true
        )
        // 1.8 of 2.4 total weight.
        assertEquals(0.75f, world.lightFraction, 0.01f)
    }

    @Test
    fun `a day with nothing prepared is at rest, not at zero`() {
        val world = WorldState.from(
            data = CitadelData(),
            sky = noon,
            todayKey = "2026-03-11",
            nowMillis = 0L,
            ambience = true
        )
        assertTrue(world.dayIsUnwritten)
        assertEquals(0f, world.wildness, 0.001f)
    }

    @Test
    fun `absence brings mist but never takes the Citadel`() {
        val now = 30 * dayMillis
        val world = WorldState.from(
            data = CitadelData(
                kingdom = KingdomState(lastKeptAtMillis = now - 20 * dayMillis)
            ),
            sky = noon,
            todayKey = "2026-03-11",
            nowMillis = now,
            ambience = true
        )
        assertTrue(world.wildness > 0.5f)
        // Deliberately capped below 1: the world gets quiet, it never gets lost.
        assertTrue(world.wildness <= 0.85f)
    }

    @Test
    fun `keeping one promise clears most of the mist`() {
        val now = 30 * dayMillis
        val data = CitadelData(
            missions = listOf(mission("Walk", kept = true), mission("Read", kept = false)),
            kingdom = KingdomState(lastKeptAtMillis = now - 20 * dayMillis)
        )
        val world = WorldState.from(data, noon, "2026-03-11", now, ambience = true)
        assertTrue("mist should thin sharply on the first kept promise", world.wildness < 0.2f)
    }

    @Test
    fun `village warmth grows over weeks and is monotonic`() {
        var previous = -1f
        for (days in 0..40) {
            val world = WorldState.from(
                data = CitadelData(kingdom = KingdomState(daysProtected = days)),
                sky = noon,
                todayKey = "2026-03-11",
                nowMillis = 0L,
                ambience = true
            )
            assertTrue("warmth went backwards at $days days", world.villageWarmth >= previous)
            previous = world.villageWarmth
        }
        assertEquals(1f, previous, 0.001f)
    }

    @Test
    fun `long term life arrives in the documented order`() {
        fun warmthAt(days: Int) = WorldState.from(
            data = CitadelData(kingdom = KingdomState(daysProtected = days)),
            sky = noon,
            todayKey = "2026-03-11",
            nowMillis = 0L,
            ambience = true
        )
        assertTrue(warmthAt(2).let { !it.hasSprouts })
        assertTrue(warmthAt(9).hasSprouts)
        assertTrue(warmthAt(16).hasFlowers)
        assertTrue(warmthAt(22).hasBirds)
        assertTrue(warmthAt(30).hasHound)
    }

    @Test
    fun `lanterns never exceed the length of the wall`() {
        val many = (1..20).map { mission("Promise $it", kept = true) }
        val world = WorldState.from(
            data = CitadelData(missions = many),
            sky = noon,
            todayKey = "2026-03-11",
            nowMillis = 0L,
            ambience = true
        )
        assertEquals(WorldState.WALL_LANTERNS, world.lanternsLit)
    }
}
