package dev.atharva.citadel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/** Small tracked-out heading. Used sparingly — most sections need no label at all. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, tint: Color = DawnGold) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = tint.copy(alpha = 0.85f),
        modifier = modifier
    )
}

/**
 * The gold action.
 *
 * Gold is the only interactive accent in the Citadel and it is spent carefully — there
 * should rarely be two of these on one screen.
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null
) {
    val alpha = if (enabled) 1f else 0.35f
    Surface(
        color = DawnGold.copy(alpha = 0.14f * alpha),
        contentColor = DawnGold.copy(alpha = alpha),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DawnGold.copy(alpha = 0.42f * alpha)),
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The quieter alternative. Never competes with gold. */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = citadelPalette
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.outline.copy(alpha = if (enabled) 0.55f else 0.2f)),
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The Guardian, saying something.
 *
 * Floats over the world, holds for a few seconds, and leaves. There is no dialogue tree
 * behind it and no way to ask for more — that restraint is the character.
 */
@Composable
fun GuardianUtterance(
    text: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 },
        exit = fadeOut(tween(900)) + slideOutVertically(tween(900)) { it / 4 },
        modifier = modifier
    ) {
        val palette = citadelPalette
        Surface(
            color = palette.surfaceElevated.copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DawnGold.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(DawnGold.copy(alpha = 0.85f))
                )
                Text(
                    text = text.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                )
            }
        }
    }
}

/** A hairline the same colour as the walls. Used to separate without drawing a box. */
@Composable
fun Hairline(modifier: Modifier = Modifier, alpha: Float = 0.35f) {
    val palette = citadelPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.outline.copy(alpha = alpha))
            .clearAndSetSemantics { }
    )
}

/**
 * An empty state that describes the world rather than the absence of data.
 * "No tasks" is a database talking. "The hearth is quiet today" is a place.
 */
@Composable
fun QuietState(
    line: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            textAlign = TextAlign.Center
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VSpace(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun HSpace(width: Int) = Spacer(Modifier.width(width.dp))
