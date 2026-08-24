package dev.atharva.citadel.data

import android.content.Context
import androidx.compose.runtime.Immutable
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
    /** Notifications. Two a day at most, and never about what was missed. */
    val whispers: Boolean = false,
    val dawnWhisperMinute: Int = 7 * 60 + 30,
    val eveningWhisperMinute: Int = 21 * 60,
    /** How many missions the evening ritual gently suggests. Never enforced. */
    val suggestedMissions: Int = 5,
    /** Replays the arrival on every open instead of only the first of the day. */
    val alwaysArrive: Boolean = false
)

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("citadel.settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<CitadelSettings> = _settings.asStateFlow()

    private fun read() = CitadelSettings(
        ambience = prefs.getBoolean(KEY_AMBIENCE, true),
        whispers = prefs.getBoolean(KEY_WHISPERS, false),
        dawnWhisperMinute = prefs.getInt(KEY_DAWN, 7 * 60 + 30),
        eveningWhisperMinute = prefs.getInt(KEY_EVENING, 21 * 60),
        suggestedMissions = prefs.getInt(KEY_SUGGESTED, 5),
        alwaysArrive = prefs.getBoolean(KEY_ALWAYS_ARRIVE, false)
    )

    fun update(transform: (CitadelSettings) -> CitadelSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putBoolean(KEY_AMBIENCE, next.ambience)
            .putBoolean(KEY_WHISPERS, next.whispers)
            .putInt(KEY_DAWN, next.dawnWhisperMinute)
            .putInt(KEY_EVENING, next.eveningWhisperMinute)
            .putInt(KEY_SUGGESTED, next.suggestedMissions)
            .putBoolean(KEY_ALWAYS_ARRIVE, next.alwaysArrive)
            .apply()
        _settings.value = next
    }

    /** The last day the arrival sequence was shown, so it plays once a day and not on every glance. */
    var lastArrivalDayKey: String
        get() = prefs.getString(KEY_LAST_ARRIVAL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_ARRIVAL, value).apply()

    private companion object {
        const val KEY_AMBIENCE = "ambience"
        const val KEY_WHISPERS = "whispers"
        const val KEY_DAWN = "dawn_whisper"
        const val KEY_EVENING = "evening_whisper"
        const val KEY_SUGGESTED = "suggested_missions"
        const val KEY_ALWAYS_ARRIVE = "always_arrive"
        const val KEY_LAST_ARRIVAL = "last_arrival_day"
    }
}
