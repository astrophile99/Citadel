package dev.atharva.citadel.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/**
 * A promise, written onto the land.
 *
 * Deliberately not a to-do row: the card is translucent so the world shows faintly
 * through it, and a kept promise is never struck through. Strikethrough is the single
 * most checklist-shaped gesture in interface design, and it says "cancelled" when the
 * Citadel wants to say "kept".
 */
@Composable
fun MissionCard(
    mission: Mission,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette
    val kept = mission.isComplete

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "press"
    )

    val container by animateColorAsState(
        targetValue = if (kept) {
            AncientForest.copy(alpha = 0.10f)
        } else {
            palette.surfaceElevated.copy(alpha = 0.74f)
        },
        animationSpec = tween(600),
        label = "container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (kept) AncientForest.copy(alpha = 0.30f) else palette.outline.copy(alpha = 0.45f),
        animationSpec = tween(600),
        label = "border"
    )
    val titleColor by animateColorAsState(
        targetValue = if (kept) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(600),
        label = "title"
    )

    val state = if (kept) "Kept" else "Not yet kept"

    Surface(
        color = container,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                role = Role.Checkbox
                stateDescription = state
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onToggle(!kept) },
                onLongClick = onOpen,
                onClickLabel = if (kept) "Mark as not yet kept" else "Mark as kept",
                onLongClickLabel = "Open promise"
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeepMark(kept = kept)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor
                )
                ImpactLabel(impact = mission.impact, dimmed = kept, carried = mission.carried)
            }
        }
    }
}

/**
 * The keep mark.
 *
 * A ring that fills with a warm centre, rather than a checkmark. Checkmarks tick things
 * off a list; this is closer to a light coming on.
 */
@Composable
private fun KeepMark(kept: Boolean) {
    val fill by animateFloatAsState(
        targetValue = if (kept) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 220f),
        label = "keepMark"
    )
    val ring = if (kept) AncientForest else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(26.dp)
            .clearAndSetSemantics { }
            .drawBehind {
                val r = size.minDimension / 2f
                val centre = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = ring.copy(alpha = if (kept) 0.9f else 0.55f),
                    radius = r - 1.dp.toPx(),
                    center = centre,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
                if (fill > 0.01f) {
                    // The glow first, then the light itself.
                    drawCircle(
                        color = DawnGold.copy(alpha = 0.22f * fill),
                        radius = (r + 3.dp.toPx()) * fill,
                        center = centre
                    )
                    drawCircle(
                        color = AncientForest.copy(alpha = 0.35f * fill),
                        radius = (r - 3.dp.toPx()) * fill,
                        center = centre
                    )
                    drawCircle(
                        color = DawnGold.copy(alpha = fill),
                        radius = 4.5.dp.toPx() * fill,
                        center = centre
                    )
                }
            }
    )
}

@Composable
private fun ImpactLabel(impact: MissionImpact, dimmed: Boolean, carried: Int) {
    val tint = when (impact) {
        MissionImpact.MINOR -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        MissionImpact.MODERATE -> MaterialTheme.colorScheme.secondary
        MissionImpact.MAJOR -> DawnGold
    }
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dimmed) 0.45f else 0.85f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (dimmed) tint.copy(alpha = 0.35f) else tint)
        )
        // The label is always spelled out: the colour of the dot is decoration, never the
        // only carrier of meaning.
        Text(
            text = impact.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
        if (carried > 0) {
            // Stated once, never counted. "Carried" is a fact about the promise, not a score.
            Text(
                text = "·  CARRIED",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}
