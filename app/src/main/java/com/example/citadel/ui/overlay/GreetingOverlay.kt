package com.example.citadel.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.citadel.ui.scene.ArrivalState
import com.example.citadel.ui.theme.TimeOfDay

@Composable
fun GreetingOverlay(
    timeOfDay: TimeOfDay,
    activeMessageIndex: Int,
    arrivalState: ArrivalState,
    onCompanionCycled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dawnMessages = listOf(
        "The scout's kettle is boiling. Warm your hands, Commander.",
        "The morning dew is heavy. Today holds new promises.",
        "A fresh start is a quiet gift. Let us take it one step at a time.",
        "The hearth fire is ready for today's march."
    )
    val dayMessages = listOf(
        "The roads are clear. Keep steady, Commander.",
        "A focused mind is like a deep well. Draw from it slowly.",
        "The wind blows from the east. The walls stand strong.",
        "The kingdom is at peace under your watch."
    )
    val duskMessages = listOf(
        "The sun dips below the ridge. The fire is stoked.",
        "The march was long. Sit, rest your boots, Commander.",
        "The light fades, but the hearth is warm. Let the day rest.",
        "The watch is taking their posts at the gates."
    )
    val nightMessages = listOf(
        "The stars are quiet guardians. Rest well, Commander.",
        "The watch is set. Tomorrow begins at dawn.",
        "Sleep is the best fortification. The Citadel is safe tonight.",
        "The embers glow softly. Rest easy."
    )

    val companionMessages = when (timeOfDay) {
        TimeOfDay.DAWN -> dawnMessages
        TimeOfDay.DAY -> dayMessages
        TimeOfDay.DUSK -> duskMessages
        TimeOfDay.NIGHT -> nightMessages
    }

    val currentWhisper = companionMessages[activeMessageIndex % companionMessages.size]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = arrivalState.greetingAlpha
            },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Greeting Title & Subtext
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = getGreetingText(timeOfDay),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = getGreetingSubtext(timeOfDay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Guardian Whisper Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCompanionCycled
                )
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✦",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$currentWhisper\"",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— The Guardian",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

private fun getGreetingText(timeOfDay: TimeOfDay): String {
    return when (timeOfDay) {
        TimeOfDay.DAWN -> "Good morning, Commander."
        TimeOfDay.DAY -> "Daylight watches, Commander."
        TimeOfDay.DUSK -> "Welcome home, Commander."
        TimeOfDay.NIGHT -> "Rest easy tonight, Commander."
    }
}

private fun getGreetingSubtext(timeOfDay: TimeOfDay): String {
    return when (timeOfDay) {
        TimeOfDay.DAWN -> "The scouts are ready. Today holds new promises."
        TimeOfDay.DAY -> "The walls are strong. Let us proceed with today's watch."
        TimeOfDay.DUSK -> "The day's march is behind us. Step to the campfire."
        TimeOfDay.NIGHT -> "The watch is set. Tomorrow begins at dawn."
    }
}
