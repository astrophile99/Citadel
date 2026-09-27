package dev.atharva.citadel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.atharva.citadel.core.time.rememberSkyMoment
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.domain.WhisperRoute
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
import dev.atharva.citadel.ui.tour.TourScreen

/**
 * The whole Citadel, assembled.
 *
 * Navigation is a handful of states rather than a navigation graph — with three places
 * and three overlays, a graph library would be more configuration than the app has routes.
 */
@Composable
fun CitadelShell(viewModel: CitadelViewModel) {
    val sky by rememberSkyMoment(viewModel.clock)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val utterance by viewModel.utterance.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()

    CitadelTheme(sky = sky) {
        val palette = citadelPalette

        var destination by rememberSaveable { mutableStateOf(Destination.HEARTH) }
        var inRitual by rememberSaveable { mutableStateOf(false) }
        var replayingTour by rememberSaveable { mutableStateOf(false) }
        var prepare by remember { mutableStateOf<PrepareTarget?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            // Until the Citadel has been read from disk there is nothing honest to show.
            // It takes milliseconds; the window is already the same obsidian.
            if (!state.ready) return@Box

            val tourDone = state.settings.hasSeenTour

            if (tourDone) {
                val world = remember(state.today, state.kingdom, sky, state.settings.ambience) {
                    WorldState.from(
                        data = CitadelData(missions = state.today, kingdom = state.kingdom),
                        sky = sky,
                        todayKey = state.todayKey,
                        nowMillis = System.currentTimeMillis(),
                        ambience = state.settings.ambience
                    )
                }

                // The arrival plays once a day, and after the first-run tour — the tour is
                // the gate, and this is walking through it.
                val arrivalToken = remember(state.todayKey) {
                    if (viewModel.shouldPlayArrival()) state.todayKey else null
                }
                val arrival = rememberArrival(
                    token = arrivalToken,
                    onFinished = viewModel::arrivalPlayed
                )

                // A tapped whisper says where to go.
                LaunchedEffect(route) {
                    when (route) {
                        WhisperRoute.RITUAL -> {
                            destination = Destination.HEARTH
                            inRitual = true
                        }
                        WhisperRoute.PREPARE -> {
                            destination = Destination.HEARTH
                            inRitual = false
                            prepare = PrepareTarget.New(state.todayKey)
                        }
                        WhisperRoute.HEARTH -> {
                            destination = Destination.HEARTH
                            inRitual = false
                        }
                        null -> return@LaunchedEffect
                    }
                    viewModel.routeHandled()
                }

                // The Guardian says one thing and then stops. Nothing has to be dismissed.
                LaunchedEffect(utterance?.id) {
                    if (utterance != null) {
                        kotlinx.coroutines.delay(5_200)
                        viewModel.clearUtterance()
                    }
                }

                BackHandler(enabled = !replayingTour && (inRitual || destination != Destination.HEARTH)) {
                    when {
                        inRitual -> inRitual = false
                        else -> destination = Destination.HEARTH
                    }
                }

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
                            onUpdate = viewModel::updateSettings,
                            onReplayTour = { replayingTour = true }
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

            // The tour: on the very first open, and whenever the Commander asks to see it again.
            AnimatedVisibility(
                visible = !tourDone || replayingTour,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(700))
            ) {
                TourScreen(
                    sky = sky,
                    firstRun = !tourDone,
                    ambience = state.settings.ambience,
                    onFinish = { enableWhispers ->
                        if (!tourDone) viewModel.finishTour(enableWhispers)
                        replayingTour = false
                    }
                )
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
                canMoveToTomorrow = (target as? PrepareTarget.Existing)
                    ?.mission?.dayKey != state.tomorrowKey,
                onContinueTomorrow = { mission: Mission ->
                    viewModel.moveToTomorrow(mission)
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
