package dev.atharva.citadel.ui.ritual

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.domain.GuardianVoice
import dev.atharva.citadel.ui.components.GoldButton
import dev.atharva.citadel.ui.components.QuietButton
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.components.StatusBarFade
import dev.atharva.citadel.ui.components.readableWidth
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/**
 * The nightly ritual.
 *
 * A short ceremony, not a planning workflow. The suggested number of promises is shown
 * as unlit lanterns rather than a counter, and the copy is written so that stopping at
 * two feels like a decision rather than a shortfall — because for most evenings, it is.
 */
@Composable
fun RitualScreen(
    tomorrow: List<Mission>,
    suggested: Int,
    onPrepare: () -> Unit,
    onOpenMission: (Mission) -> Unit,
    onCloseGates: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette
    val scroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to palette.background,
                    0.6f to palette.surface,
                    1f to palette.background
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .readableWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                text = "Back",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Tomorrow",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = GuardianVoice.ritualInvitation(tomorrow.size, suggested),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(28.dp))
            LanternRow(lit = tomorrow.size, of = suggested)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Around $suggested is a good night's plan. Fewer is also a plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(32.dp))

            if (tomorrow.isNotEmpty()) {
                SectionLabel("The orders")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tomorrow.forEach { mission ->
                        RitualRow(mission = mission, onClick = { onOpenMission(mission) })
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            GoldButton(
                text = "Prepare a mission",
                onClick = onPrepare,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            QuietButton(
                text = "Close the gates",
                onClick = onCloseGates,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = GuardianVoice.ritualClosing(tomorrow.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(48.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        StatusBarFade()
    }
}

/**
 * The plan, as lanterns waiting to be lit.
 *
 * Showing "3 / 5" would invite the Commander to feel two short. Showing five lantern
 * hooks with three lanterns hung on them just shows the evening as it is.
 */
@Composable
private fun LanternRow(lit: Int, of: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { }
    ) {
        repeat(maxOf(of, lit)) { index ->
            val on = index < lit
            val glow by animateFloatAsState(
                targetValue = if (on) 1f else 0f,
                animationSpec = tween(500, delayMillis = index * 60),
                label = "lantern$index"
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DawnGold.copy(alpha = 0.18f * glow))
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (on) {
                                DawnGold.copy(alpha = glow)
                            } else {
                                citadelPalette.outline.copy(alpha = 0.7f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun RitualRow(mission: Mission, onClick: () -> Unit) {
    val palette = citadelPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(palette.surfaceElevated.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(DawnGold.copy(alpha = 0.75f))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = mission.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = mission.impact.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
