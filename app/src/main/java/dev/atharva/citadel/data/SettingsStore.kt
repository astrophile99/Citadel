package dev.atharva.citadel.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import dev.atharva.citadel.domain.WhisperCadence
import dev.atharva.citadel.domain.WhisperSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Commander's preferences.
 *
 * Small enough that SharedPreferences is the honest choice — exposed as a StateFlow so
 * the UI observes it the same way it observes everything else.
 */
@Immutable
data class CitadelSettings(
    /** Atmospheric motion in the world. Off is a first-class way to use the app. */
    val ambience: Boolean = true,
    /** Whispers from the kingdom. Off until the Commander says yes. */
    val whispers: Boolean = false,
    val whisperCadence: WhisperCadence = WhisperCadence.FULL,
    /** When the day's planning whisper arrives. */
    val dawnWhisperMinute: Int = 7 * 60 + 30,
    /** When the night's ritual whisper arrives. */
    val eveningWhisperMinute: Int = 21 * 60,
    /** How many missions the evening ritual gently suggests. Never enforced. */
    val suggestedMissions: Int = 5,
    /** Replays the arrival on every open instead of only the first of the day. */
    val alwaysArrive: Boolean = false,
    /** Whether the Commander has been shown how the Citadel works. */
    val hasSeenTour: Boolean = false
)

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<CitadelSettings> = _settings.asStateFlow()

    private fun read() = CitadelSettings(
        ambience = prefs.getBoolean(KEY_AMBIENCE, true),
        whispers = prefs.getBoolean(KEY_WHISPERS, false),
        whisperCadence = WhisperCadence.fromName(prefs.getString(KEY_CADENCE, null)),
        dawnWhisperMinute = prefs.getInt(KEY_DAWN, 7 * 60 + 30).coerceIn(WhisperSchedule.MORNING_RANGE),
        eveningWhisperMinute = prefs.getInt(KEY_EVENING, 21 * 60).coerceIn(WhisperSchedule.NIGHT_RANGE),
        suggestedMissions = prefs.getInt(KEY_SUGGESTED, 5).coerceIn(1, 8),
        alwaysArrive = prefs.getBoolean(KEY_ALWAYS_ARRIVE, false),
        hasSeenTour = prefs.getBoolean(KEY_SEEN_TOUR, false)
    )

    fun update(transform: (CitadelSettings) -> CitadelSettings) {
        val next = transform(_settings.value).let {
            it.copy(
                dawnWhisperMinute = it.dawnWhisperMinute.coerceIn(WhisperSchedule.MORNING_RANGE),
                eveningWhisperMinute = it.eveningWhisperMinute.coerceIn(WhisperSchedule.NIGHT_RANGE),
                suggestedMissions = it.suggestedMissions.coerceIn(1, 8)
            )
        }
        prefs.edit {
            putBoolean(KEY_AMBIENCE, next.ambience)
            putBoolean(KEY_WHISPERS, next.whispers)
            putString(KEY_CADENCE, next.whisperCadence.name)
            putInt(KEY_DAWN, next.dawnWhisperMinute)
            putInt(KEY_EVENING, next.eveningWhisperMinute)
            putInt(KEY_SUGGESTED, next.suggestedMissions)
            putBoolean(KEY_ALWAYS_ARRIVE, next.alwaysArrive)
            putBoolean(KEY_SEEN_TOUR, next.hasSeenTour)
        }
        _settings.value = next
    }

    /** The last day the arrival sequence was shown, so it plays once a day and not on every glance. */
    var lastArrivalDayKey: String
        get() = prefs.getString(KEY_LAST_ARRIVAL, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_LAST_ARRIVAL, value) }

    /** When the Commander last opened the Citadel. A whisper right after a visit is noise. */
    var lastOpenedAtMillis: Long
        get() = prefs.getLong(KEY_LAST_OPENED, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_OPENED, value) }

    /** The day the "every lantern is lit" whisper was last sent. It is said once a day, at most. */
    var lastCelebratedDayKey: String
        get() = prefs.getString(KEY_LAST_CELEBRATED, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_LAST_CELEBRATED, value) }

    private companion object {
        const val FILE = "citadel.settings"
        const val KEY_AMBIENCE = "ambience"
        const val KEY_WHISPERS = "whispers"
        const val KEY_CADENCE = "whisper_cadence"
        const val KEY_DAWN = "dawn_whisper"
        const val KEY_EVENING = "evening_whisper"
        const val KEY_SUGGESTED = "suggested_missions"
        const val KEY_ALWAYS_ARRIVE = "always_arrive"
        const val KEY_SEEN_TOUR = "has_seen_tour"
        const val KEY_LAST_ARRIVAL = "last_arrival_day"
        const val KEY_LAST_OPENED = "last_opened_at"
        const val KEY_LAST_CELEBRATED = "last_celebrated_day"
    }
}
