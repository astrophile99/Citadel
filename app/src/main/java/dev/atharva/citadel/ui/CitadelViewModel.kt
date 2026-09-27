package dev.atharva.citadel.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.atharva.citadel.CitadelApp
import dev.atharva.citadel.core.time.CitadelClock
import dev.atharva.citadel.data.CitadelSettings
import dev.atharva.citadel.data.Homecoming
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.domain.DayView
import dev.atharva.citadel.domain.GuardianMoment
import dev.atharva.citadel.domain.GuardianVoice
import dev.atharva.citadel.domain.WhisperRoute
import dev.atharva.citadel.system.Whispers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CitadelUiState(
    val ready: Boolean = false,
    val todayKey: String = "",
    val tomorrowKey: String = "",
    val today: List<Mission> = emptyList(),
    val tomorrow: List<Mission> = emptyList(),
    val resting: List<Mission> = emptyList(),
    val chronicle: List<ChronicleEntry> = emptyList(),
    val kingdom: KingdomState = KingdomState(),
    val homecoming: Homecoming = Homecoming.None,
    val settings: CitadelSettings = CitadelSettings()
) {
    val keptToday: Int get() = today.count { it.isComplete }
}

/** A single line from the Guardian, shown once and then allowed to fade. */
@Immutable
data class Utterance(val id: Long, val text: String)

/**
 * One view model for the whole Citadel.
 *
 * The world, the missions and the Chronicle are one shared state — splitting them across
 * four view models would mean synchronising them back together, which is more machinery
 * than this app has business owning.
 */
class CitadelViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CitadelApp).container
    private val repository = container.repository
    private val settings = container.settings

    /** The Citadel's clock. Real time in release builds; pinnable in debug builds. */
    val clock: CitadelClock = container.clock

    private val _utterance = MutableStateFlow<Utterance?>(null)
    val utterance: StateFlow<Utterance?> = _utterance.asStateFlow()

    /** Where a tapped whisper asked to take the Commander. Consumed once by the UI. */
    private val _route = MutableStateFlow<WhisperRoute?>(null)
    val route: StateFlow<WhisperRoute?> = _route.asStateFlow()

    private val _ready = MutableStateFlow(false)

    /**
     * The day the Citadel believes it is. Advanced on return to the app, so a Citadel
     * left open overnight wakes to the new day instead of showing yesterday.
     */
    private val _today = MutableStateFlow(clock.todayKey())

    val uiState: StateFlow<CitadelUiState> = combine(
        repository.data,
        repository.homecoming,
        settings.settings,
        _ready,
        _today
    ) { data, homecoming, prefs, ready, todayKey ->
        val day = DayView.of(data, todayKey)
        CitadelUiState(
            ready = ready,
            todayKey = day.todayKey,
            tomorrowKey = day.tomorrowKey,
            today = day.today,
            tomorrow = day.tomorrow,
            resting = data.resting(),
            chronicle = data.chronicle,
            kingdom = data.kingdom,
            homecoming = homecoming,
            settings = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CitadelUiState()
    )

    init {
        viewModelScope.launch {
            repository.awaken()
            _today.value = clock.todayKey()
            _ready.value = true
            // Re-arm on every open, so a cleared alarm or a changed clock heals itself.
            Whispers.reschedule(getApplication())
        }
    }

    // ---- presence -------------------------------------------------------------------

    /** The Commander has arrived (again). */
    fun onForeground() {
        settings.lastOpenedAtMillis = System.currentTimeMillis()
        Whispers.clearShown(getApplication())

        val today = clock.todayKey()
        if (_ready.value && today != _today.value) {
            // A new day began while the Citadel sat in the background. Sweep it properly.
            viewModelScope.launch {
                repository.awaken()
                _today.value = today
            }
        }
    }

    fun onBackground() {
        settings.lastOpenedAtMillis = System.currentTimeMillis()
    }

    fun open(route: WhisperRoute) {
        _route.value = route
    }

    fun routeHandled() {
        _route.value = null
    }

    // ---- promises -------------------------------------------------------------------

    fun prepare(title: String, impact: MissionImpact, recurrence: Recurrence, dayKey: String) {
        viewModelScope.launch { repository.prepare(title, impact, recurrence, dayKey) }
    }

    fun setKept(mission: Mission, kept: Boolean) {
        viewModelScope.launch {
            val before = uiState.value
            repository.setKept(mission.id, kept)

            if (!kept) return@launch
            // The Guardian speaks twice a day at most: at the first light, and when the wall is full.
            val keptAfter = before.keptToday + 1
            val prepared = before.today.size
            val moment = when {
                prepared > 0 && keptAfter == prepared -> GuardianMoment.AllKept
                keptAfter == 1 -> GuardianMoment.FirstLight
                else -> null
            }
            moment?.let { speak(GuardianVoice.moment(it, before.todayKey)) }
        }
    }

    fun revise(mission: Mission, title: String, impact: MissionImpact, recurrence: Recurrence) {
        viewModelScope.launch { repository.revise(mission.id, title, impact, recurrence) }
    }

    fun release(mission: Mission) {
        viewModelScope.launch { repository.release(mission.id) }
    }

    fun setAside(mission: Mission) {
        viewModelScope.launch { repository.setAside(mission.id) }
    }

    fun continueMission(mission: Mission) {
        viewModelScope.launch { repository.continueMission(mission.id, _today.value) }
    }

    /** Carries a promise to tomorrow's wall — from today, or out of the Chronicle. */
    fun moveToTomorrow(mission: Mission) {
        viewModelScope.launch {
            repository.continueMission(mission.id, uiState.value.tomorrowKey.ifBlank { return@launch })
        }
    }

    fun closeTheGates() {
        speak(GuardianVoice.moment(GuardianMoment.TomorrowSet, _today.value))
    }

    // ---- arrival, tour & settings ----------------------------------------------------

    /** True the first time the Citadel is opened on a given day, or always if the Commander prefers. */
    fun shouldPlayArrival(): Boolean {
        if (settings.settings.value.alwaysArrive) return true
        return settings.lastArrivalDayKey != clock.todayKey()
    }

    fun arrivalPlayed() {
        settings.lastArrivalDayKey = clock.todayKey()
        viewModelScope.launch { repository.markArrived() }
    }

    /**
     * The first-run tour is over. [enableWhispers] is the Commander's answer to the one
     * question the tour asks; null means the question was not asked (a replay).
     */
    fun finishTour(enableWhispers: Boolean?) {
        settings.update { current ->
            current.copy(
                hasSeenTour = true,
                whispers = enableWhispers ?: current.whispers
            )
        }
        Whispers.reschedule(getApplication())
    }

    fun updateSettings(transform: (CitadelSettings) -> CitadelSettings) {
        settings.update(transform)
        Whispers.reschedule(getApplication())
    }

    fun speak(text: String) {
        _utterance.value = Utterance(System.nanoTime(), text)
    }

    fun clearUtterance() {
        _utterance.value = null
    }
}
