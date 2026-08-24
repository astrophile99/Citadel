package dev.atharva.citadel.data.model

import androidx.compose.runtime.Immutable

/** The entire remembered state of one Citadel. */
@Immutable
data class CitadelData(
    val missions: List<Mission> = emptyList(),
    val chronicle: List<ChronicleEntry> = emptyList(),
    val kingdom: KingdomState = KingdomState()
) {
    fun missionsFor(dayKey: String): List<Mission> =
        missions.filter { it.dayKey == dayKey && it.status != MissionStatus.RESTING }

    fun resting(): List<Mission> =
        missions.filter { it.status == MissionStatus.RESTING }.sortedByDescending { it.dayKey }

    companion object {
        val Empty = CitadelData()
    }
}
