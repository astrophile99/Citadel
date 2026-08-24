package dev.atharva.citadel.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.R
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

enum class Destination(
    val label: String,
    @param:DrawableRes val icon: Int
) {
    HEARTH("Hearth", R.drawable.ic_hearth),
    CHRONICLE("Chronicle", R.drawable.ic_chronicle),
    SANCTUARY("Sanctuary", R.drawable.ic_sanctuary)
}

/**
 * The way through the Citadel.
 *
 * Not a Material NavigationBar: the standard component paints an opaque tonal slab
 * across the bottom of the screen, which would cut the world off at a hard line — the
 * exact problem this whole rebuild exists to solve. This one dissolves into the veil,
 * and marks the current place with a lit point rather than a filled pill.
 */
@Composable
fun CitadelBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to palette.veil.copy(alpha = 0f),
                    0.30f to palette.veil.copy(alpha = 0.62f),
                    0.55f to palette.veil.copy(alpha = 0.96f),
                    1.00f to palette.veil
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                // A long run-up, so text passing underneath fades out well before it
                // reaches the icons instead of colliding with them.
                .padding(top = 40.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Destination.entries.forEach { destination ->
                BarItem(
                    destination = destination,
                    selected = destination == current,
                    onSelect = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    destination: Destination,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) {
            DawnGold
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(320),
        label = "barTint"
    )
    val lit by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "barLit"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onSelect
            )
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        // The lit point. A tab is "where the lantern is", not "where the pill is".
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer {
                    alpha = lit
                    scaleX = lit
                    scaleY = lit
                }
                .clip(CircleShape)
                .background(DawnGold)
        )
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/**
 * Height the content should clear so nothing ever comes to rest underneath the bar.
 * Generous on purpose: the gradient can hide a passing line, not a button.
 */
val BarClearance = 132.dp
