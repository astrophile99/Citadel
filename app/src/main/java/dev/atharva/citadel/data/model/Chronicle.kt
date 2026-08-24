package dev.atharva.citadel.data.model

import androidx.compose.runtime.Immutable

/**
 * One day, remembered. The Chronicle is a story, not an archive of failures —
 * so an entry records what was kept *and* what is still waiting, in the same warm voice.
 */
@Immutable
data class ChronicleEntry(
    val dayKey: String,
    val kept: List<String> = emptyList(),
    val waiting: List<String> = emptyList(),
    val prepared: Int = 0,
    /** The Guardian's closing line for that day. */
    val note: String = ""
) {
    val keptCount: Int get() = kept.size
    val wasQuiet: Boolean get() = kept.isEmpty()
}

/**
 * Progress that can never be taken away.
 *
 * Every field here is monotonic: nothing in this app is allowed to make these numbers
 * go down. Absence changes the weather, never the structure.
 */
@Immutable
data class KingdomState(
    val foundedDayKey: String = "",
    val totalKept: Int = 0,
    /** Days on which at least one promise was kept. Not a streak — a total. */
    val daysProtected: Int = 0,
    val lastActiveDayKey: String = "",
    /** The most recent day on which a promise was kept. Guards [daysProtected] against double counting. */
    val lastProtectedDayKey: String = "",
    val lastKeptAtMillis: Long = 0L,
    /** Set once the Commander has seen the world for the first time. */
    val hasArrived: Boolean = false
)
