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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
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
import dev.atharva.citadel.data.model.MissionStatus
import dev.atharva.citadel.data.model.Recurrence
import dev.atharva.citadel.ui.components.GoldButton
import dev.atharva.citadel.ui.components.Hairline
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette

/** What the sheet is being opened for. */
sealed interface PrepareTarget {
    /** A new task, for the given day. */
    data class New(val dayKey: String, val forTomorrow: Boolean = false) : PrepareTarget

    /** An existing task, opened from its options button or a long-press. */
    data class Existing(val mission: Mission) : PrepareTarget
}

/**
 * Adding or editing a task.
 *
 * One field, three sizes, one switch. No due times, no subtasks, no tags, no priority
 * matrix — every one of those would be a place to spend the evening arranging your life
 * instead of living it. An existing task also gets its three plain actions: done, skip, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareSheet(
    target: PrepareTarget,
    onDismiss: () -> Unit,
    onCommit: (title: String, impact: MissionImpact, recurrence: Recurrence) -> Unit,
    canMoveToTomorrow: Boolean = true,
    isTomorrow: Boolean = false,
    onToggleDone: (Mission, Boolean) -> Unit = { _, _ -> },
    onDoToday: (Mission) -> Unit = {},
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
    var confirmDelete by remember(target) { mutableStateOf(false) }

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
                    existing != null -> "Edit task"
                    (target as? PrepareTarget.New)?.forTomorrow == true -> "New task for tomorrow"
                    else -> "New task"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (existing != null) {
                TaskActions(
                    mission = existing,
                    isTomorrow = isTomorrow,
                    onToggleDone = onToggleDone,
                    onDoToday = onDoToday,
                    onSkip = onSetAside,
                    onDelete = { confirmDelete = true }
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                placeholder = {
                    Text(
                        "What do you want to get done?",
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
                SectionLabel("How big is it?")
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
                text = if (existing != null) "Save changes" else "Add task",
                onClick = { onCommit(title, impact, recurrence) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            if (existing != null && canMoveToTomorrow) {
                Spacer(Modifier.height(2.dp))
                Hairline()
                QuietAction("Move to tomorrow") { onContinueTomorrow(existing) }
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = palette.surfaceElevated,
            title = { Text("Delete this task?") },
            text = {
                Text("\u201c${existing.title}\u201d will be removed for good. To keep it for later, use Skip instead.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onRelease(existing)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

/**
 * The three things people most want to do with a task, in plain words, side by side.
 *
 * Skip is not failure: a skipped task waits in History and can be brought back any time.
 * A task already waiting there offers "Do today" in place of Done and Skip.
 */
@Composable
private fun TaskActions(
    mission: Mission,
    isTomorrow: Boolean,
    onToggleDone: (Mission, Boolean) -> Unit,
    onDoToday: (Mission) -> Unit,
    onSkip: (Mission) -> Unit,
    onDelete: () -> Unit
) {
    val resting = mission.status == MissionStatus.RESTING
    val plain = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                resting -> ActionChip("Do today", Icons.Filled.Refresh, DawnGold) { onDoToday(mission) }
                isTomorrow -> Unit
                mission.isComplete -> ActionChip("Not done", Icons.Filled.Refresh, plain) {
                    onToggleDone(mission, false)
                }
                else -> ActionChip("Done", Icons.Filled.Check, AncientForest) { onToggleDone(mission, true) }
            }
            if (!resting && !mission.isComplete) {
                ActionChip("Skip", Icons.AutoMirrored.Filled.ArrowForward, plain) { onSkip(mission) }
            }
            ActionChip("Delete", Icons.Filled.Delete, MaterialTheme.colorScheme.error, onClick = onDelete)
        }
        Text(
            text = if (resting) {
                "This task is waiting in History. Bring it back to today, or delete it."
            } else {
                "Skipped tasks wait in History, so you can do them another day."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RowScope.ActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        color = tint.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        modifier = Modifier
            .weight(1f)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = tint)
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
