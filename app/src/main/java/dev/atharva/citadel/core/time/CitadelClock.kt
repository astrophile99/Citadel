package dev.atharva.citadel.core.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The Citadel keeps its own time.
 *
 * Everything visual is driven from a single continuous [SkyMoment] rather than four
 * discrete buckets, so the light actually moves through the day instead of snapping
 * between four presets at fixed hours.
 */
interface CitadelClock {
    fun now(): LocalDateTime
    fun today(): LocalDate = now().toLocalDate()
    fun todayKey(): String = today().format(DAY_KEY)
    fun epochMillis(): Long

    companion object {
        val DAY_KEY: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

object SystemClock : CitadelClock {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun epochMillis(): Long = System.currentTimeMillis()
}

/** The four named phases. Used for copy, the Guardian's voice, and ritual timing — never for colour. */
enum class DayPhase { DAWN, DAY, DUSK, NIGHT }

private const val MINUTES_PER_DAY = 1440

// Phase boundaries, in minutes past midnight.
private const val DAWN_START = 5 * 60 + 30      // 05:30
private const val DAY_START = 11 * 60 + 30      // 11:30
private const val DUSK_START = 17 * 60          // 17:00
private const val NIGHT_START = 21 * 60         // 21:00

private const val SUNRISE = 6 * 60              // 06:00
private const val SUNSET = 18 * 60 + 30         // 18:30

/**
 * A single instant of the Citadel's sky, expressed as continuous fractions.
 * Every scene layer reads from this and nothing else.
 */
@Immutable
data class SkyMoment(
    /** 0..1439 */
    val minuteOfDay: Int,
    val phase: DayPhase,
    /** 0 = broad daylight, 1 = deep night. Drives stars, contrast and mood. */
    val nightFactor: Float,
    /** 0 = eastern horizon, 1 = western horizon. Only meaningful while [sunVisible] > 0. */
    val sunTrack: Float,
    val sunVisible: Float,
    val moonTrack: Float,
    val moonVisible: Float,
    /** Peaks at sunrise and sunset — the warm band sitting on the horizon. */
    val horizonWarmth: Float
) {
    val isNight: Boolean get() = phase == DayPhase.NIGHT
    /** True during the hours when preparing tomorrow feels natural. */
    val isRitualHour: Boolean get() = minuteOfDay >= DUSK_START || minuteOfDay < 4 * 60

    companion object {
        fun at(time: LocalDateTime): SkyMoment = at(time.hour * 60 + time.minute)

        fun at(minuteOfDay: Int): SkyMoment {
            val m = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)

            val phase = when {
                m < DAWN_START -> DayPhase.NIGHT
                m < DAY_START -> DayPhase.DAWN
                m < DUSK_START -> DayPhase.DAY
                m < NIGHT_START -> DayPhase.DUSK
                else -> DayPhase.NIGHT
            }

            // Night eases out across the hour before sunrise and eases in after sunset.
            val nightFactor = when {
                m <= SUNRISE - 75 -> 1f
                m < SUNRISE + 45 -> 1f - smoothStep((m - (SUNRISE - 75)).toFloat() / 120f)
                m <= SUNSET - 45 -> 0f
                m < SUNSET + 75 -> smoothStep((m - (SUNSET - 45)).toFloat() / 120f)
                else -> 1f
            }

            val dayLength = (SUNSET - SUNRISE).toFloat()
            val rawSun = (m - SUNRISE) / dayLength
            val sunVisible = when {
                m < SUNRISE - 25 || m > SUNSET + 25 -> 0f
                m < SUNRISE + 25 -> ((m - (SUNRISE - 25)) / 50f).coerceIn(0f, 1f)
                m > SUNSET - 25 -> (((SUNSET + 25) - m) / 50f).coerceIn(0f, 1f)
                else -> 1f
            }

            // The moon takes the other half of the clock.
            val nightLength = (MINUTES_PER_DAY - (SUNSET - SUNRISE)).toFloat()
            val minutesSinceSunset = when {
                m >= SUNSET -> (m - SUNSET).toFloat()
                else -> (m + MINUTES_PER_DAY - SUNSET).toFloat()
            }
            val rawMoon = minutesSinceSunset / nightLength
            val moonVisible = nightFactor

            // Warmth hugging the horizon: strongest exactly at sunrise and sunset.
            val toSunrise = circularDistance(m, SUNRISE)
            val toSunset = circularDistance(m, SUNSET)
            val horizonWarmth = maxOf(
                1f - (toSunrise / 95f).coerceIn(0f, 1f),
                1f - (toSunset / 95f).coerceIn(0f, 1f)
            ).let { smoothStep(it) }

            return SkyMoment(
                minuteOfDay = m,
                phase = phase,
                nightFactor = nightFactor,
                sunTrack = rawSun.coerceIn(0f, 1f),
                sunVisible = sunVisible,
                moonTrack = rawMoon.coerceIn(0f, 1f),
                moonVisible = moonVisible,
                horizonWarmth = horizonWarmth
            )
        }

        private fun circularDistance(a: Int, b: Int): Float {
            val d = kotlin.math.abs(a - b)
            return minOf(d, MINUTES_PER_DAY - d).toFloat()
        }

        private fun smoothStep(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }
    }
}

/**
 * The sky as live state. Re-reads the wall clock every half minute, so leaving the app
 * open through sunset actually shows sunset.
 */
@Composable
fun rememberSkyMoment(clock: CitadelClock = SystemClock): State<SkyMoment> {
    val initial = remember(clock) { mutableStateOf(SkyMoment.at(clock.now())) }
    return produceState(initialValue = initial.value, clock) {
        while (true) {
            value = SkyMoment.at(clock.now())
            delay(30_000L)
        }
    }
}
