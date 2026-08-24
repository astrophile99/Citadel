package dev.atharva.citadel.ui.sanctuary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.atharva.citadel.data.CitadelSettings
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.ui.components.Hairline
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.nav.BarClearance
import dev.atharva.citadel.ui.scene.rememberSystemWantsStillness
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Sanctuary.
 *
 * Settings, and the only place in the Citadel where numbers are shown at all — presented
 * as a record of what has been built rather than a performance dashboard. There is no
 * streak here, and there never will be.
 */
@Composable
fun SanctuaryScreen(
    settings: CitadelSettings,
    kingdom: KingdomState,
    onUpdate: ((CitadelSettings) -> CitadelSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val systemStillness = rememberSystemWantsStillness()

    val notificationsGranted = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If the Commander says no, the Citadel says nothing further about it, ever.
        onUpdate { it.copy(whispers = granted) }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(36.dp))
            Text(
                text = "Sanctuary",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(30.dp))

            KingdomRecord(kingdom)

            Spacer(Modifier.height(34.dp))
            SectionLabel("The world")
            Spacer(Modifier.height(12.dp))

            ToggleRow(
                title = "Ambience",
                detail = "Drifting cloud, mist, firelight. Turning this off makes the world still, not empty.",
                checked = settings.ambience && !systemStillness,
                enabled = !systemStillness,
                onChange = { on -> onUpdate { it.copy(ambience = on) } }
            )

            if (systemStillness) {
                Spacer(Modifier.height(10.dp))
                Note("Your system has animations turned off, so the Citadel is holding still. That setting wins.")
            }

            Spacer(Modifier.height(10.dp))
            ToggleRow(
                title = "Arrive every time",
                detail = "By default the world only assembles itself once a day.",
                checked = settings.alwaysArrive,
                onChange = { on -> onUpdate { it.copy(alwaysArrive = on) } }
            )

            Spacer(Modifier.height(34.dp))
            SectionLabel("Whispers")
            Spacer(Modifier.height(12.dp))

            ToggleRow(
                title = "Messages from the kingdom",
                detail = "Two a day at most. Never about what is unfinished.",
                checked = settings.whispers,
                onChange = { on ->
                    if (on && !notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onUpdate { it.copy(whispers = on) }
                    }
                }
            )

            if (settings.whispers) {
                Spacer(Modifier.height(14.dp))
                TimeRow(
                    label = "Morning",
                    minute = settings.dawnWhisperMinute,
                    onChange = { m -> onUpdate { it.copy(dawnWhisperMinute = m) } }
                )
                Spacer(Modifier.height(8.dp))
                TimeRow(
                    label = "Evening",
                    minute = settings.eveningWhisperMinute,
                    onChange = { m -> onUpdate { it.copy(eveningWhisperMinute = m) } }
                )
            }

            Spacer(Modifier.height(34.dp))
            SectionLabel("The evening ritual")
            Spacer(Modifier.height(12.dp))
            CounterRow(
                label = "Promises suggested for tomorrow",
                value = settings.suggestedMissions,
                range = 1..8,
                onChange = { n -> onUpdate { it.copy(suggestedMissions = n) } }
            )
            Spacer(Modifier.height(8.dp))
            Note("A suggestion, never a requirement. The Citadel does not check.")

            Spacer(Modifier.height(38.dp))
            Hairline()
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Everything the Citadel remembers is stored on this device only. " +
                    "There is no account, no sync and no analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            )

            Spacer(Modifier.height(BarClearance))
        }
    }
}

/**
 * What has been built. Totals only — a total cannot be broken, which is the entire
 * difference between this and a streak.
 */
@Composable
private fun KingdomRecord(kingdom: KingdomState) {
    val palette = citadelPalette
    val founded = remember(kingdom.foundedDayKey) {
        runCatching {
            LocalDate.parse(kingdom.foundedDayKey)
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
        }.getOrDefault("today")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(palette.surfaceElevated.copy(alpha = 0.45f))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Citadel has stood since $founded.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
            Record(kingdom.daysProtected.toString(), "days protected")
            Record(kingdom.totalKept.toString(), "promises kept")
        }
    }
}

@Composable
private fun Record(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = DawnGold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = citadelPalette.background,
                checkedTrackColor = DawnGold,
                checkedBorderColor = DawnGold,
                uncheckedThumbColor = citadelPalette.outline,
                uncheckedTrackColor = citadelPalette.surfaceElevated,
                uncheckedBorderColor = citadelPalette.outline
            )
        )
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onChange: (Int) -> Unit) {
    val text = remember(minute) {
        String.format(Locale.getDefault(), "%02d:%02d", minute / 60, minute % 60)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Stepper(
            display = text,
            onLess = { onChange(((minute - 30) + 1440) % 1440) },
            onMore = { onChange((minute + 30) % 1440) },
            lessLabel = "$label, half an hour earlier",
            moreLabel = "$label, half an hour later"
        )
    }
}

@Composable
private fun CounterRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Stepper(
            display = value.toString(),
            onLess = { onChange((value - 1).coerceIn(range)) },
            onMore = { onChange((value + 1).coerceIn(range)) },
            lessLabel = "One fewer",
            moreLabel = "One more"
        )
    }
}

@Composable
private fun Stepper(
    display: String,
    onLess: () -> Unit,
    onMore: () -> Unit,
    lessLabel: String,
    moreLabel: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton("–", lessLabel, onLess)
        Text(
            text = display,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepButton("+", moreLabel, onMore)
    }
}

@Composable
private fun StepButton(glyph: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(citadelPalette.surfaceElevated.copy(alpha = 0.7f))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = AncientForest.copy(alpha = 0.95f)
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}
