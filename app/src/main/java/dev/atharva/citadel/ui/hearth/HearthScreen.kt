package dev.atharva.citadel.ui.hearth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.R
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.domain.GuardianVoice
import dev.atharva.citadel.domain.WorldState
import dev.atharva.citadel.ui.CitadelUiState
import dev.atharva.citadel.ui.components.GoldButton
import dev.atharva.citadel.ui.components.MissionCard
import dev.atharva.citadel.ui.components.QuietButton
import dev.atharva.citadel.ui.components.QuietState
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.nav.BarClearance
import dev.atharva.citadel.ui.scene.Arrival
import dev.atharva.citadel.ui.scene.ArrivalController
import dev.atharva.citadel.ui.scene.KingdomWorld
import dev.atharva.citadel.ui.scene.rememberSystemWantsStillness
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/**
 * The Hearth.
 *
 * The composition is the whole argument of this app, so it is worth being explicit about
 * it: the world is not a header above the promises. The world is the screen. The promises
 * are written onto its lower ground and scroll up out of it, and the only thing separating
 * the two is a gradient. There is no card, no rounded illustration, no boundary to cross.
 */
@Composable
fun HearthScreen(
    state: CitadelUiState,
    world: WorldState,
    arrival: ArrivalController,
    onToggle: (Mission, Boolean) -> Unit,
    onOpenMission: (Mission) -> Unit,
    onPrepare: () -> Unit,
    onOpenRitual: () -> Unit,
    onGuardianTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val stillness = rememberSystemWantsStillness()

    // The world reacting to a promise being kept: one warm breath, once, then gone.
    val bloom = remember { Animatable(0f) }
    LaunchedEffect(state.keptToday) {
        if (state.keptToday > 0) {
            bloom.snapTo(0f)
            bloom.animateTo(1f, tween(1500))
            bloom.snapTo(0f)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportPx = with(density) { maxHeight.toPx() }

        // The world owns the opening view. Expressed as a fraction of the actual viewport
        // rather than a fixed dp, so the composition holds on every screen size — and tuned
        // so that the greeting plus about two promises sit below it before the fold.
        // Note this is where the *text* begins, not where the world ends — the land keeps
        // going behind and beneath everything. The citadel sits around 0.42 of the viewport,
        // so beginning here leaves the kingdom entirely uncovered while still putting the
        // greeting and roughly two promises above the fold.
        val worldSpace = maxHeight * 0.50f

        // How far the surface has been pulled up over the land. One value, one recession.
        val recession = { (scroll.value / (viewportPx * 0.42f)).coerceIn(0f, 1f) }
        val parallax = { -scroll.value * 0.30f }

        KingdomWorld(
            world = world,
            arrivalProgress = arrival.progress,
            recession = recession,
            parallaxPx = parallax,
            bloom = bloom.asState(),
            veilColor = palette.veil,
            veilTop = 0.52f,
            stillness = stillness,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Before the world has settled, a touch means "I am here, get on with it".
                        // Afterwards, it is how the Commander asks the Guardian for a thought.
                        if (arrival.progress.value < 1f) {
                            arrival.skip()
                        } else {
                            onGuardianTapped(
                                GuardianVoice.thought(world.sky.phase, state.todayKey, world)
                            )
                        }
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            Spacer(Modifier.height(worldSpace))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Greeting(state = state, world = world, arrival = arrival)

                Promises(
                    state = state,
                    arrival = arrival,
                    onToggle = onToggle,
                    onOpenMission = onOpenMission,
                    onPrepare = onPrepare
                )

                if (world.sky.isRitualHour) {
                    RitualInvitation(
                        preparedForTomorrow = state.tomorrow.size,
                        suggested = state.settings.suggestedMissions,
                        arrival = arrival,
                        onOpen = onOpenRitual
                    )
                }
            }

            Spacer(Modifier.height(BarClearance))
        }

        // Anything that scrolls up under the status bar fades out rather than colliding
        // with the clock.
        TopFade()
    }
}

@Composable
private fun Greeting(
    state: CitadelUiState,
    world: WorldState,
    arrival: ArrivalController
) {
    val thought = remember(world.sky.phase, state.todayKey, world.everythingKept, world.wildness > 0.5f) {
        GuardianVoice.thought(world.sky.phase, state.todayKey, world)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = Arrival.greeting(arrival.progress.value) },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = GuardianVoice.greeting(world.sky.phase, state.homecoming),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = GuardianVoice.subtitle(world.sky.phase, world, state.homecoming),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f)
        )

        // The Guardian's standing thought. Quiet, set apart by a single gold rule.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .drawBehind {
                    drawRect(
                        color = DawnGold.copy(alpha = 0.45f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 4f),
                        size = androidx.compose.ui.geometry.Size(1.5.dp.toPx(), size.height - 8f)
                    )
                }
                .padding(start = 16.dp)
        ) {
            Text(
                text = thought,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
            )
        }
    }
}

@Composable
private fun Promises(
    state: CitadelUiState,
    arrival: ArrivalController,
    onToggle: (Mission, Boolean) -> Unit,
    onOpenMission: (Mission) -> Unit,
    onPrepare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = Arrival.missions(arrival.progress.value)
                translationY = Arrival.missionsLift(arrival.progress.value).dp.toPx()
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.today.isEmpty()) {
            QuietState(
                line = "The hearth is quiet today.",
                detail = "Name one thing worth protecting."
            )
            Spacer(Modifier.height(4.dp))
            GoldButton(
                text = "Prepare a mission",
                onClick = onPrepare,
                icon = painterResource(R.drawable.ic_prepare),
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }

        SectionLabel("Today")

        state.today.forEach { mission ->
            MissionCard(
                mission = mission,
                onToggle = { kept -> onToggle(mission, kept) },
                onOpen = { onOpenMission(mission) }
            )
        }

        Spacer(Modifier.height(2.dp))
        QuietButton(
            text = "Prepare another",
            onClick = onPrepare,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The evening ritual, offered rather than demanded.
 *
 * It appears only in the ritual hours and it never says how many promises are missing.
 */
@Composable
private fun RitualInvitation(
    preparedForTomorrow: Int,
    suggested: Int,
    arrival: ArrivalController,
    onOpen: () -> Unit
) {
    val palette = citadelPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = Arrival.missions(arrival.progress.value) },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.outline.copy(alpha = 0.4f))
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = GuardianVoice.ritualInvitation(preparedForTomorrow, suggested),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
        )
        QuietButton(
            text = if (preparedForTomorrow == 0) "Prepare tomorrow" else "Look at tomorrow",
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A short fade across the status bar, so content scrolling up behind the clock dissolves
 * instead of colliding with it. Edge-to-edge without a top bar needs this or it reads as a bug.
 */
@Composable
private fun BoxScope.TopFade() {
    val palette = citadelPalette
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(inset + 22.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    0f to palette.background.copy(alpha = 0.72f),
                    0.55f to palette.background.copy(alpha = 0.32f),
                    1f to palette.background.copy(alpha = 0f)
                )
            )
    )
}
