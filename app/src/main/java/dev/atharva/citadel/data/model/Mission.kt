package dev.atharva.citadel.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * A Mission is a real-world promise the Commander made to themselves.
 * The app does not own it. It only remembers it.
 */
@Immutable
data class Mission(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val impact: MissionImpact = MissionImpact.MODERATE,
    val status: MissionStatus = MissionStatus.ACTIVE,
    /** The day this mission belongs to, ISO local date. */
    val dayKey: String,
    val recurrence: Recurrence = Recurrence.ONCE,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    /**
     * How many times this promise has been carried forward.
     * Never surfaced as a count. Used only to let the Guardian be gentler about it.
     */
    val carried: Int = 0,
    /**
     * Whether this promise has ever counted towards the kingdom's permanent totals.
     *
     * Un-checking a mis-tap and checking it again is a correction, not a second promise
     * kept, so the totals must not move the second time.
     */
    val counted: Boolean = false
) {
    val isComplete: Boolean get() = status == MissionStatus.COMPLETE
    val isActive: Boolean get() = status == MissionStatus.ACTIVE
}

/**
 * Impact is emotional weight, not difficulty. It answers "how much of me does this take?"
 * rather than "how hard is this?".
 */
enum class MissionImpact(val label: String, val worldWeight: Float) {
    MINOR("Skirmish", 0.6f),
    MODERATE("Fortification", 1.0f),
    MAJOR("Expedition", 1.8f);

    val description: String
        get() = when (this) {
            MINOR -> "A small thing, quickly done"
            MODERATE -> "Steady work that holds the walls"
            MAJOR -> "This one matters"
        }
}

enum class MissionStatus {
    ACTIVE,
    COMPLETE,

    /**
     * Not "failed" and not "overdue". A promise that is simply still waiting.
     * Resting missions live in the Chronicle and can be continued at any time.
     */
    RESTING
}

enum class Recurrence {
    ONCE,
    DAILY;

    val label: String get() = if (this == DAILY) "Every day" else "Once"
}
