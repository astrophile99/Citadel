package dev.atharva.citadel.domain

import androidx.compose.runtime.Immutable
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionStatus
import java.time.LocalDate

/**
 * Today and tomorrow, as every surface of the Citadel sees them — the Hearth, the widget
 * and the whispers all read from this, so they always agree.
 */
@Immutable
data class DayView(
    val todayKey: String,
    val tomorrowKey: String,
    val today: List<Mission>,
    val tomorrow: List<Mission>
) {
    val prepared: Int get() = today.size
    val kept: Int get() = today.count { it.isComplete }
    val waiting: List<Mission> get() = today.filterNot { it.isComplete }
    val allKept: Boolean get() = prepared > 0 && kept == prepared
    val unwritten: Boolean get() = prepared == 0

    companion object {
        fun of(data: CitadelData, todayKey: String): DayView {
            val tomorrowKey = runCatching { LocalDate.parse(todayKey).plusDays(1).toString() }
                .getOrDefault(todayKey)
            return DayView(
                todayKey = todayKey,
                tomorrowKey = tomorrowKey,
                today = data.missionsFor(todayKey).sortedWith(MissionOrder),
                tomorrow = data.missionsFor(tomorrowKey).sortedWith(MissionOrder)
            )
        }
    }
}

/**
 * Kept promises settle to the bottom without being hidden, and heavier promises sit above
 * lighter ones — so what is left undone is always what the eye lands on first.
 */
val MissionOrder: Comparator<Mission> = compareBy(
    { it.status == MissionStatus.COMPLETE },
    { -it.impact.worldWeight },
    { it.createdAt }
)
