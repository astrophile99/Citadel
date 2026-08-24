package dev.atharva.citadel.ui.chronicle

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.data.model.ChronicleEntry
import dev.atharva.citadel.data.model.Mission
import dev.atharva.citadel.ui.components.QuietState
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.nav.BarClearance
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The Chronicle.
 *
 * This is where a normal productivity app keeps a page called Archived Tasks, with an
 * overdue count at the top. The Citadel keeps a record of days instead — what was kept,
 * and what is still waiting, in the same voice and without a number between them.
 *
 * Nothing here can be lost, and anything still waiting can be picked up again in one tap.
 */
@Composable
fun ChronicleScreen(
    entries: List<ChronicleEntry>,
    waiting: List<Mission>,
    onContinue: (Mission) -> Unit,
    onOpenMission: (Mission) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to palette.background,
                    0.5f to palette.surface,
                    1f to palette.background
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(36.dp))
                Text(
                    text = "The Chronicle",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Everything the Citadel remembers. Nothing here is a debt.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                )
                Spacer(Modifier.height(30.dp))
            }

            if (waiting.isNotEmpty()) {
                item {
                    SectionLabel("Still waiting")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Promises you have not finished with yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(waiting, key = { it.id }) { mission ->
                    WaitingRow(
                        mission = mission,
                        onContinue = { onContinue(mission) },
                        onOpen = { onOpenMission(mission) }
                    )
                }
                item { Spacer(Modifier.height(26.dp)) }
            }

            if (entries.isEmpty() && waiting.isEmpty()) {
                item {
                    Spacer(Modifier.height(40.dp))
                    QuietState(
                        line = "The Chronicle has not been opened yet.",
                        detail = "It fills itself, a day at a time."
                    )
                }
            }

            if (entries.isNotEmpty()) {
                item {
                    SectionLabel("Days")
                    Spacer(Modifier.height(10.dp))
                }
                items(entries, key = { it.dayKey }) { entry ->
                    DayEntry(entry)
                }
            }

            item { Spacer(Modifier.height(BarClearance)) }
        }
    }
}

@Composable
private fun WaitingRow(
    mission: Mission,
    onContinue: () -> Unit,
    onOpen: () -> Unit
) {
    val palette = citadelPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(palette.surfaceElevated.copy(alpha = 0.5f))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                text = friendlyDay(mission.dayKey),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
        // Never "restore" and never "recover". The Commander simply carries on with it.
        Text(
            text = "Continue",
            style = MaterialTheme.typography.labelLarge,
            color = DawnGold,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Continue ${mission.title} today",
                    onClick = onContinue
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun DayEntry(entry: ChronicleEntry) {
    val palette = citadelPalette
    val heading = remember(entry.dayKey) { friendlyDay(entry.dayKey) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(palette.surfaceElevated.copy(alpha = 0.4f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Lanterns, not numbers.
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(entry.keptCount.coerceAtMost(7)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(DawnGold.copy(alpha = 0.85f))
                    )
                }
            }
        }

        Text(
            text = entry.note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        if (entry.kept.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.kept.forEach { title ->
                    RecordLine(title, AncientForest)
                }
            }
        }
        if (entry.waiting.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.waiting.forEach { title ->
                    RecordLine(title, palette.outline)
                }
            }
        }
    }
}

@Composable
private fun RecordLine(title: String, dot: androidx.compose.ui.graphics.Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
        )
    }
}

/** "Yesterday", "Tuesday", or "14 March" — never a raw ISO date. */
private fun friendlyDay(dayKey: String): String = runCatching {
    val date = LocalDate.parse(dayKey)
    val today = LocalDate.now()
    when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.isAfter(today.minusDays(7))) {
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else {
            date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
        }
    }
}.getOrDefault(dayKey)
