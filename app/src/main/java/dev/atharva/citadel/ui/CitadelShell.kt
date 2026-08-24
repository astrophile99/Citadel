package dev.atharva.citadel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.atharva.citadel.core.time.rememberSkyMoment
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.domain.WorldState
import dev.atharva.citadel.ui.chronicle.ChronicleScreen
import dev.atharva.citadel.ui.components.GuardianUtterance
import dev.atharva.citadel.ui.hearth.HearthScreen
import dev.atharva.citadel.ui.nav.BarClearance
import dev.atharva.citadel.ui.nav.CitadelBar
import dev.atharva.citadel.ui.nav.Destination
import dev.atharva.citadel.ui.prepare.PrepareSheet
import dev.atharva.citadel.ui.prepare.PrepareTarget
import dev.atharva.citadel.ui.ritual.RitualScreen
import dev.atharva.citadel.ui.sanctuary.SanctuaryScreen
import dev.atharva.citadel.ui.scene.rememberArrival
import dev.atharva.citadel.ui.theme.CitadelTheme
import dev.atharva.citadel.ui.theme.citadelPalette

/**
 * The whole Citadel, assembled.
 *
 * Navigation is a handful of states rather than a navigation graph — with three places
 * and two overlays, a graph library would be more configuration than the app has routes.
 */
@Composable
fun CitadelShell(viewModel: CitadelViewModel = viewModel()) {
    val sky by rememberSkyMoment()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val utterance by viewModel.utterance.collectAsStateWithLifecycle()

    CitadelTheme(sky = sky) {
        val palette = citadelPalette

        var destination by remember { mutableStateOf(Destination.HEARTH) }
        var prepare by remember { mutableStateOf<PrepareTarget?>(null) }
        var inRitual by remember { mutableStateOf(false) }

        val world = remember(state.today, state.kingdom, sky, state.settings.ambience) {
            WorldState.from(
                data = dev.atharva.citadel.data.model.CitadelData(
                    missions = state.today,
                    kingdom = state.kingdom
                ),
                sky = sky,
                todayKey = state.todayKey,
                nowMillis = System.currentTimeMillis(),
                ambience = state.settings.ambience
            )
        }

        // The arrival plays once a day. Waiting for `ready` means it starts with the
        // Commander's real kingdom rather than replaying when the data lands.
        val shouldArrive = remember(state.ready) { state.ready && viewModel.shouldPlayArrival() }
        val arrival = rememberArrival(
            play = shouldArrive,
            onFinished = viewModel::arrivalPlayed
        )

        // The Guardian says one thing and then stops. Nothing has to be dismissed.
        LaunchedEffect(utterance?.id) {
            if (utterance != null) {
                kotlinx.coroutines.delay(5_200)
                viewModel.clearUtterance()
            }
        }

        BackHandler(enabled = inRitual || destination != Destination.HEARTH) {
            when {
                inRitual -> inRitual = false
                else -> destination = Destination.HEARTH
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    // A gentle cross-dissolve. Screens in the Citadel do not slide like cards.
                    (fadeIn(tween(420)) togetherWith fadeOut(tween(260)))
                },
                label = "destination"
            ) { current ->
                when (current) {
                    Destination.HEARTH -> HearthScreen(
                        state = state,
                        world = world,
                        arrival = arrival,
                        onToggle = viewModel::setKept,
                        onOpenMission = { prepare = PrepareTarget.Existing(it) },
                        onPrepare = { prepare = PrepareTarget.New(state.todayKey) },
                        onOpenRitual = { inRitual = true },
                        onGuardianTapped = viewModel::speak
                    )

                    Destination.CHRONICLE -> ChronicleScreen(
                        entries = state.chronicle,
                        waiting = state.resting,
                        onContinue = viewModel::continueMission,
                        onOpenMission = { prepare = PrepareTarget.Existing(it) }
                    )

                    Destination.SANCTUARY -> SanctuaryScreen(
                        settings = state.settings,
                        kingdom = state.kingdom,
                        onUpdate = viewModel::updateSettings
                    )
                }
            }

            CitadelBar(
                current = destination,
                onSelect = { destination = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            GuardianUtterance(
                text = utterance?.text,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = BarClearance - 12.dp)
            )

            // The evening ritual takes the whole screen. It is a small ceremony, and a
            // ceremony that shares the screen with a task list is not one.
            AnimatedContent(
                targetState = inRitual,
                transitionSpec = {
                    (fadeIn(tween(360)) + slideInVertically(tween(420)) { it / 8 }) togetherWith
                        (fadeOut(tween(240)) + slideOutVertically(tween(300)) { it / 8 })
                },
                label = "ritual"
            ) { open ->
                if (open) {
                    RitualScreen(
                        tomorrow = state.tomorrow,
                        suggested = state.settings.suggestedMissions,
                        onPrepare = {
                            prepare = PrepareTarget.New(state.tomorrowKey, forTomorrow = true)
                        },
                        onOpenMission = { prepare = PrepareTarget.Existing(it) },
                        onCloseGates = {
                            viewModel.closeTheGates()
                            inRitual = false
                        },
                        onBack = { inRitual = false }
                    )
                }
            }
        }

        prepare?.let { target ->
            PrepareSheet(
                target = target,
                onDismiss = { prepare = null },
                onCommit = { title, impact, recurrence ->
                    when (target) {
                        is PrepareTarget.New ->
                            viewModel.prepare(title, impact, recurrence, target.dayKey)

                        is PrepareTarget.Existing ->
                            viewModel.revise(target.mission, title, impact, recurrence)
                    }
                    prepare = null
                },
                onContinueTomorrow = { mission: Mission ->
                    viewModel.continueMission(mission)
                    prepare = null
                },
                onSetAside = { mission: Mission ->
                    viewModel.setAside(mission)
                    prepare = null
                },
                onRelease = { mission: Mission ->
                    viewModel.release(mission)
                    prepare = null
                }
            )
        }
    }
}
