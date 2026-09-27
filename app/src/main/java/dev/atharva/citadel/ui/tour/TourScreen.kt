package dev.atharva.citadel.ui.tour

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.core.time.SkyMoment
import dev.atharva.citadel.domain.WorldState
import dev.atharva.citadel.ui.components.GoldButton
import dev.atharva.citadel.ui.components.QuietButton
import dev.atharva.citadel.ui.components.readableWidth
import dev.atharva.citadel.ui.scene.KingdomWorld
import dev.atharva.citadel.ui.scene.rememberSystemWantsStillness
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette
import kotlinx.coroutines.launch

/**
 * How the Citadel works, told by the Citadel.
 *
 * Every page changes the world behind it: the lanterns really do light on the page about
 * keeping promises, and the mist really does roll in on the page about being away. The
 * tour shows rather than explains, and it is short enough to finish.
 *
 * On first run the last page asks the one question the app needs answered — may the
 * kingdom whisper? — and then never asks again.
 */
@Composable
fun TourScreen(
    sky: SkyMoment,
    firstRun: Boolean,
    ambience: Boolean,
    onFinish: (enableWhispers: Boolean?) -> Unit
) {
    val palette = citadelPalette
    val pages = remember { TourPages }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val stillness = rememberSystemWantsStillness()
    val last = pager.currentPage == pages.lastIndex

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onFinish(granted) }

    val askForWhispers: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onFinish(true)
        }
    }

    // Back steps through the pages. On the first page of the first run it is not handled
    // at all, so it leaves the app as Back should — nobody is ever trapped in a tour.
    BackHandler(enabled = pager.currentPage > 0 || !firstRun) {
        if (pager.currentPage > 0) {
            scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
        } else {
            onFinish(null)
        }
    }

    val world = remember(pager.currentPage, sky, ambience) {
        pages[pager.currentPage].demo(sky).copy(ambience = ambience)
    }

    // The tour has no arrival of its own; the world is simply there, fully drawn.
    val settled = remember { mutableFloatStateOf(1f) }
    val bloom = remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        KingdomWorld(
            world = world,
            arrivalProgress = settled,
            recession = { 0f },
            parallaxPx = { 0f },
            bloom = bloom,
            veilColor = palette.veil,
            veilTop = 0.40f,
            stillness = stillness,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Skip lives up in the sky, out of the way of reading.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val showSkip = !last
                Text(
                    text = if (firstRun) "Skip" else "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    modifier = Modifier
                        .graphicsLayer { alpha = if (showSkip) 1f else 0f }
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = showSkip, role = Role.Button) {
                            if (firstRun) {
                                // Skipping still passes the one question worth asking.
                                scope.launch { pager.animateScrollToPage(pages.lastIndex) }
                            } else {
                                onFinish(null)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Page text is anchored to the bottom of the pager, so however long a page is,
            // the space above it stays with the world.
            Spacer(Modifier.weight(0.5f))

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top
            ) { index ->
                PageText(page = pages[index], firstRun = firstRun)
            }

            Column(
                modifier = Modifier
                    .readableWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LanternDots(current = pager.currentPage, count = pages.size)
                Spacer(Modifier.height(6.dp))

                when {
                    !last -> GoldButton(
                        text = "Next",
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    firstRun -> {
                        GoldButton(
                            text = "Let the kingdom whisper",
                            onClick = askForWhispers,
                            modifier = Modifier.fillMaxWidth()
                        )
                        QuietButton(
                            text = "Enter without whispers",
                            onClick = { onFinish(false) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> GoldButton(
                        text = "Back to the Citadel",
                        onClick = { onFinish(null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PageText(page: TourPage, firstRun: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        PageBody(page, firstRun)
    }
}

@Composable
private fun PageBody(page: TourPage, firstRun: Boolean) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = page.eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = DawnGold.copy(alpha = 0.9f)
        )
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        )
        if (page.footnote != null && firstRun) {
            Text(
                text = page.footnote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f)
            )
        }
    }
}

/** Progress through the tour, as lanterns lighting along a wall. */
@Composable
private fun LanternDots(current: Int, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Page ${current + 1} of $count" }
    ) {
        repeat(count) { index ->
            val lit by animateFloatAsState(
                targetValue = when {
                    index == current -> 1f
                    index < current -> 0.55f
                    else -> 0f
                },
                animationSpec = tween(420),
                label = "tourLantern$index"
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(DawnGold.copy(alpha = 0.18f * lit))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (lit > 0.01f) {
                                DawnGold.copy(alpha = 0.35f + 0.65f * lit)
                            } else {
                                citadelPalette.outline.copy(alpha = 0.8f)
                            }
                        )
                )
            }
        }
    }
}

private class TourPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val footnote: String? = null,
    val demo: (SkyMoment) -> WorldState
)

private val TourPages = listOf(
    TourPage(
        eyebrow = "Welcome",
        title = "This is your Citadel.",
        body = "Not a to-do list. A small kingdom that grows a little brighter every time " +
            "you keep a promise to yourself — and waits patiently when you can't.",
        demo = { sky -> WorldState(sky = sky, lightFraction = 0.12f, lanternsLit = 1, villageWarmth = 0.25f) }
    ),
    TourPage(
        eyebrow = "Missions",
        title = "Name what matters.",
        body = "Each real thing you mean to do becomes a mission. Give it a weight — a " +
            "Skirmish, a Fortification or an Expedition. Around five a day is plenty. One is enough.",
        demo = { sky -> WorldState(sky = sky, lightFraction = 0.12f, lanternsLit = 1, villageWarmth = 0.25f) }
    ),
    TourPage(
        eyebrow = "The reward",
        title = "Keep one, and the kingdom answers.",
        body = "Tap a mission when it's done. A lantern lights along the wall, the windows " +
            "warm, the fire grows. Weeks of keeping bring flowers, birds, and one sleepy hound.",
        demo = { sky ->
            WorldState(sky = sky, keptToday = 4, preparedToday = 5, lightFraction = 0.88f, lanternsLit = 5, villageWarmth = 0.55f)
        }
    ),
    TourPage(
        eyebrow = "No guilt",
        title = "Nothing is ever lost.",
        body = "Unfinished missions rest in the Chronicle, ready to continue whenever you " +
            "like. Time away brings a little mist over the valley — never ruin. There are no streaks to break.",
        demo = { sky -> WorldState(sky = sky, lightFraction = 0.05f, lanternsLit = 0, villageWarmth = 0.25f, wildness = 0.62f) }
    ),
    TourPage(
        eyebrow = "The ritual",
        title = "Prepare tomorrow tonight.",
        body = "Before sleep, set down tomorrow's missions. Through the day the kingdom can " +
            "whisper — at dawn to plan, at midday, afternoon and evening to follow through, " +
            "and at night to prepare.",
        footnote = "You can change how often, or silence them entirely, in the Sanctuary.",
        demo = { sky -> WorldState(sky = sky, lightFraction = 0.6f, lanternsLit = 4, villageWarmth = 0.7f) }
    )
)
