package dev.atharva.citadel.ui.prepare

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.data.model.MissionImpact
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.ui.components.GoldButton
import dev.atharva.citadel.ui.components.Hairline
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/** What the sheet is being opened for. */
sealed interface PrepareTarget {
    /** A new promise, for the given day. */
    data class New(val dayKey: String, val forTomorrow: Boolean = false) : PrepareTarget

    /** An existing promise the Commander long-pressed. */
    data class Existing(val mission: Mission) : PrepareTarget
}

/**
 * Enlisting a promise.
 *
 * One field, three impacts, one switch. No due times, no subtasks, no tags, no priority
 * matrix — every one of those would be a place for the Commander to spend the evening
 * arranging their life instead of living it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareSheet(
    target: PrepareTarget,
    onDismiss: () -> Unit,
    onCommit: (title: String, impact: MissionImpact, recurrence: Recurrence) -> Unit,
    canMoveToTomorrow: Boolean = true,
    onContinueTomorrow: (Mission) -> Unit = {},
    onSetAside: (Mission) -> Unit = {},
    onRelease: (Mission) -> Unit = {}
) {
    val palette = citadelPalette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = (target as? PrepareTarget.Existing)?.mission

    var title by remember(target) { mutableStateOf(existing?.title.orEmpty()) }
    var impact by remember(target) { mutableStateOf(existing?.impact ?: MissionImpact.MODERATE) }
    var recurrence by remember(target) { mutableStateOf(existing?.recurrence ?: Recurrence.ONCE) }

    val focus = remember { FocusRequester() }
    LaunchedEffect(target) {
        if (existing == null) runCatching { focus.requestFocus() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.outline)
                    .clearAndSetSemantics { }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = when {
                    existing != null -> "This promise"
                    (target as? PrepareTarget.New)?.forTomorrow == true -> "For tomorrow"
                    else -> "A new promise"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                placeholder = {
                    Text(
                        "What are you protecting?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DawnGold.copy(alpha = 0.55f),
                    unfocusedBorderColor = palette.outline,
                    cursorColor = DawnGold,
                    focusedContainerColor = palette.surfaceElevated.copy(alpha = 0.55f),
                    unfocusedContainerColor = palette.surfaceElevated.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("How much of you it takes")
                MissionImpact.entries.forEach { option ->
                    ImpactOption(
                        impact = option,
                        selected = option == impact,
                        onSelect = { impact = option }
                    )
                }
            }

            RecurrenceRow(
                recurrence = recurrence,
                onChange = { recurrence = it }
            )

            GoldButton(
                text = if (existing != null) "Keep the change" else "Set the mission",
                onClick = { onCommit(title, impact, recurrence) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            if (existing != null) {
                Spacer(Modifier.height(2.dp))
                Hairline()
                Spacer(Modifier.height(2.dp))
                // Three ways to put a promise down, and not one of them is called "delete failed task".
                if (canMoveToTomorrow) {
                    QuietAction("Continue tomorrow") { onContinueTomorrow(existing) }
                }
                QuietAction("Set aside for now") { onSetAside(existing) }
                QuietAction("Let it go", tint = MaterialTheme.colorScheme.error) { onRelease(existing) }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ImpactOption(
    impact: MissionImpact,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val palette = citadelPalette
    val border by animateColorAsState(
        targetValue = if (selected) DawnGold.copy(alpha = 0.55f) else palette.outline,
        animationSpec = tween(240),
        label = "impactBorder"
    )
    val tint = when (impact) {
        MissionImpact.MINOR -> MaterialTheme.colorScheme.onSurfaceVariant
        MissionImpact.MODERATE -> MaterialTheme.colorScheme.secondary
        MissionImpact.MAJOR -> DawnGold
    }

    Surface(
        color = if (selected) DawnGold.copy(alpha = 0.09f) else palette.surfaceElevated.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = impact.label, role = Role.RadioButton, onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (selected) tint else tint.copy(alpha = 0.35f))
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = impact.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = impact.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun RecurrenceRow(recurrence: Recurrence, onChange: (Recurrence) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("How often")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Recurrence.entries.forEach { option ->
                val selected = option == recurrence
                Surface(
                    color = if (selected) AncientForest.copy(alpha = 0.16f) else citadelPalette.surfaceElevated.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) AncientForest.copy(alpha = 0.55f) else citadelPalette.outline
                    ),
                    modifier = Modifier.clickable(
                        role = Role.RadioButton,
                        onClick = { onChange(option) }
                    )
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuietAction(
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = tint,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
