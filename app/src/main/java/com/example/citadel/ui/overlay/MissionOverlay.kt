package com.example.citadel.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.scene.ArrivalState

enum class MissionDifficulty {
    MINOR, MODERATE, MAJOR
}

data class Mission(
    val id: Int,
    val title: String,
    val difficulty: MissionDifficulty,
    val isCompleted: Boolean = false
)

@Composable
fun MissionOverlay(
    missions: List<Mission>,
    arrivalState: ArrivalState,
    onToggleMission: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = arrivalState.journalOffsetDp.dp.toPx()
                alpha = arrivalState.journalAlpha
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "TODAY'S PROMISES",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        missions.forEach { mission ->
            MissionCard(
                mission = mission,
                onToggleMission = onToggleMission
            )
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onToggleMission: (Int) -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (mission.isCompleted) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        label = "CardColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "CardScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (mission.isCompleted) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            }
        ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onToggleMission(mission.id) }
            )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom circular checked indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (mission.isCompleted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (mission.isCompleted) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (mission.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (mission.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = if (mission.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (mission.difficulty) {
                                    MissionDifficulty.MINOR -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    MissionDifficulty.MODERATE -> MaterialTheme.colorScheme.secondary
                                    MissionDifficulty.MAJOR -> MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                    Text(
                        text = getDifficultyLabel(mission.difficulty),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mission.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

private fun getDifficultyLabel(difficulty: MissionDifficulty): String {
    return when (difficulty) {
        MissionDifficulty.MINOR -> "Minor Promise"
        MissionDifficulty.MODERATE -> "Moderate Fortification"
        MissionDifficulty.MAJOR -> "Major Expedition"
    }
}
