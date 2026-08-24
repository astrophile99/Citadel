package dev.atharva.citadel.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.atharva.citadel.CitadelApp
import dev.atharva.citadel.data.CitadelSettings
import dev.atharva.citadel.data.Homecoming
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.domain.GuardianMoment
import dev.atharva.citadel.domain.GuardianVoice
import dev.atharva.citadel.system.Whispers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    private val clock = container.clock

    private val _utterance = MutableStateFlow<Utterance?>(null)
    val utterance: StateFlow<Utterance?> = _utterance.asStateFlow()

    private val _ready = MutableStateFlow(false)

    val uiState: StateFlow<CitadelUiState> = combine(
        repository.data,
        repository.homecoming,
        settings.settings,
        _ready
    ) { data, homecoming, prefs, ready ->
        val today = clock.todayKey()
        val tomorrow = LocalDate.parse(today).plusDays(1).toString()
        CitadelUiState(
            ready = ready,
            todayKey = today,
            tomorrowKey = tomorrow,
            today = data.missionsFor(today).sortedWith(missionOrder),
            tomorrow = data.missionsFor(tomorrow).sortedWith(missionOrder),
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
            _ready.value = true
            // Re-arm whispers on every open, so a changed time or a cleared alarm heals itself.
            val prefs = settings.settings.value
            Whispers.apply(
                context = getApplication(),
                enabled = prefs.whispers,
                dawnMinute = prefs.dawnWhisperMinute,
                eveningMinute = prefs.eveningWhisperMinute
            )
        }
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
        viewModelScope.launch { repository.continueMission(mission.id, clock.todayKey()) }
    }

    fun closeTheGates() {
        val state = uiState.value
        speak(GuardianVoice.moment(GuardianMoment.TomorrowSet, state.todayKey))
    }

    // ---- arrival & settings ---------------------------------------------------------

    /** True the first time the Citadel is opened on a given day, or always if the Commander prefers. */
    fun shouldPlayArrival(): Boolean {
        val today = clock.todayKey()
        if (settings.settings.value.alwaysArrive) return true
        return settings.lastArrivalDayKey != today
    }

    fun arrivalPlayed() {
        settings.lastArrivalDayKey = clock.todayKey()
        viewModelScope.launch { repository.markArrived() }
    }

    fun dismissHomecoming() = repository.dismissHomecoming()

    fun updateSettings(transform: (CitadelSettings) -> CitadelSettings) {
        settings.update(transform)
        val prefs = settings.settings.value
        Whispers.apply(
            context = getApplication(),
            enabled = prefs.whispers,
            dawnMinute = prefs.dawnWhisperMinute,
            eveningMinute = prefs.eveningWhisperMinute
        )
    }

    fun speak(text: String) {
        _utterance.value = Utterance(System.currentTimeMillis(), text)
    }

    fun clearUtterance() {
        _utterance.value = null
    }

    private companion object {
        /**
         * Kept promises settle to the bottom without being hidden, and heavier promises sit
         * above lighter ones — so what is left undone is always what the eye lands on first.
         */
        val missionOrder = compareBy<Mission>(
            { it.status == MissionStatus.COMPLETE },
            { -it.impact.worldWeight },
            { it.createdAt }
        )
    }
}
