package dev.atharva.citadel.ui.sanctuary

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.atharva.citadel.data.CitadelSettings
import dev.atharva.citadel.data.model.KingdomState
import dev.atharva.citadel.domain.WhisperCadence
import dev.atharva.citadel.domain.WhisperSchedule
import dev.atharva.citadel.system.Whispers
import dev.atharva.citadel.ui.components.Hairline
import dev.atharva.citadel.ui.components.QuietButton
import dev.atharva.citadel.ui.components.SectionLabel
import dev.atharva.citadel.ui.components.StatusBarFade
import dev.atharva.citadel.ui.components.readableWidth
import dev.atharva.citadel.ui.nav.BarClearance
import dev.atharva.citadel.ui.scene.rememberSystemWantsStillness
import dev.atharva.citadel.ui.theme.AncientForest
import dev.atharva.citadel.ui.theme.DawnGold
import dev.atharva.citadel.ui.theme.citadelPalette
import dev.atharva.citadel.widget.CitadelWidget
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Sanctuary.
 *
 * Settings, the guide, and the only place in the Citadel where numbers are shown at all —
 * presented as a record of what has been built rather than a performance dashboard. There
 * is no streak here, and there never will be.
 */
@Composable
fun SanctuaryScreen(
    settings: CitadelSettings,
    kingdom: KingdomState,
    onUpdate: ((CitadelSettings) -> CitadelSettings) -> Unit,
    onReplayTour: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = citadelPalette
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val systemStillness = rememberSystemWantsStillness()

    // Re-checked on every return to the screen: the Commander may have changed it in system settings.
    var canNotify by remember { mutableStateOf(Whispers.canNotify(context)) }
    var showBlockedHint by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canNotify = Whispers.canNotify(context)
        if (canNotify) showBlockedHint = false
    }

    var showPrivacy by remember { mutableStateOf(false) }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canNotify = Whispers.canNotify(context)
        if (granted) {
            onUpdate { it.copy(whispers = true) }
        } else {
            // Ask once. After that, point to where the choice lives — never nag.
            showBlockedHint = true
        }
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
                .readableWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(36.dp))
            Text(
                text = "Sanctuary",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(30.dp))

            KingdomRecord(kingdom)

            // ---- the world ----
            Section("The world")
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

            // ---- whispers ----
            Section("Whispers")
            ToggleRow(
                title = "Messages from the kingdom",
                detail = "Up to five a day — to plan at dawn, follow through at midday, afternoon and " +
                    "evening, and prepare tomorrow at night. Never guilt, never streaks.",
                checked = settings.whispers && canNotify,
                onChange = { on ->
                    when {
                        !on -> onUpdate { it.copy(whispers = false) }
                        canNotify -> onUpdate { it.copy(whispers = true) }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !showBlockedHint ->
                            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> showBlockedHint = true
                    }
                }
            )

            AnimatedVisibility(visible = showBlockedHint && !canNotify) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Note("Notifications are turned off for Citadel in your system settings.")
                    QuietButton(
                        text = "Open notification settings",
                        onClick = { openNotificationSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = settings.whispers && canNotify) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How often",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhisperCadence.entries.forEach { cadence ->
                            Choice(
                                label = cadence.label,
                                selected = cadence == settings.whisperCadence,
                                onClick = { onUpdate { it.copy(whisperCadence = cadence) } }
                            )
                        }
                    }
                    Note(settings.whisperCadence.detail)
                    Spacer(Modifier.height(2.dp))
                    TimeRow(
                        label = "Dawn whisper",
                        minute = settings.dawnWhisperMinute,
                        range = WhisperSchedule.MORNING_RANGE,
                        onChange = { m -> onUpdate { it.copy(dawnWhisperMinute = m) } }
                    )
                    TimeRow(
                        label = "Night whisper",
                        minute = settings.eveningWhisperMinute,
                        range = WhisperSchedule.NIGHT_RANGE,
                        onChange = { m -> onUpdate { it.copy(eveningWhisperMinute = m) } }
                    )
                    Note("Today they arrive at " + scheduleLine(settings) + ".")
                }
            }

            // ---- ritual ----
            Section("The evening ritual")
            CounterRow(
                label = "Promises suggested for tomorrow",
                value = settings.suggestedMissions,
                range = 1..8,
                onChange = { n -> onUpdate { it.copy(suggestedMissions = n) } }
            )
            Spacer(Modifier.height(8.dp))
            Note("A suggestion, never a requirement. The Citadel does not check.")

            // ---- home screen ----
            Section("On your home screen")
            Text(
                text = "A small window into the kingdom: today's light, and the promises you set.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            if (remember { CitadelWidget.canRequestPin(context) }) {
                QuietButton(
                    text = "Place the Citadel widget",
                    onClick = { CitadelWidget.requestPin(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Note("Long-press your home screen, choose Widgets, and find Citadel.")
            }

            // ---- guide & about ----
            Section("Guide")
            LinkRow("How Citadel works", onClick = onReplayTour)
            LinkRow("Privacy", onClick = { showPrivacy = true })

            Spacer(Modifier.height(30.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Everything the Citadel remembers is stored on this device only. " +
                    "There is no account, no sync and no analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Citadel " + versionName(context),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(BarClearance))
        }

        StatusBarFade()
    }

    if (showPrivacy) {
        PrivacySheet(onDismiss = { showPrivacy = false })
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(34.dp))
    SectionLabel(title)
    Spacer(Modifier.height(12.dp))
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
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
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
            // The row owns the click, so the switch is announced once rather than twice.
            onCheckedChange = null,
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
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = citadelPalette
    Surface(
        color = if (selected) DawnGold.copy(alpha = 0.12f) else palette.surfaceElevated.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) DawnGold.copy(alpha = 0.55f) else palette.outline),
        modifier = Modifier.clickable(role = Role.RadioButton, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = DawnGold.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, range: IntRange, onChange: (Int) -> Unit) {
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
            display = clock(minute),
            onLess = { onChange((minute - 30).coerceIn(range)) },
            onMore = { onChange((minute + 30).coerceIn(range)) },
            lessEnabled = minute - 30 >= range.first,
            moreEnabled = minute + 30 <= range.last,
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
            lessEnabled = value > range.first,
            moreEnabled = value < range.last,
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
    lessEnabled: Boolean,
    moreEnabled: Boolean,
    lessLabel: String,
    moreLabel: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton("–", lessLabel, lessEnabled, onLess)
        Text(
            text = display,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepButton("+", moreLabel, moreEnabled, onMore)
    }
}

@Composable
private fun StepButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(citadelPalette.surfaceElevated.copy(alpha = if (enabled) 0.7f else 0.3f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = AncientForest.copy(alpha = if (enabled) 0.95f else 0.35f)
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySheet(onDismiss: () -> Unit) {
    val palette = citadelPalette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Note("Effective ${PrivacyPolicy.EFFECTIVE}")
            PrivacyPolicy.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun clock(minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minute / 60, minute % 60)

private fun scheduleLine(settings: CitadelSettings): String =
    settings.whisperCadence.slots.joinToString(" · ") { slot ->
        clock(WhisperSchedule.minuteOf(slot, settings.dawnWhisperMinute, settings.eveningWhisperMinute))
    }

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}.getOrDefault("")

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
