package com.example.citadel.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.motion.SceneScrollController
import com.example.citadel.ui.motion.rememberSceneScrollController
import com.example.citadel.ui.overlay.GreetingOverlay
import com.example.citadel.ui.overlay.Mission
import com.example.citadel.ui.overlay.MissionDifficulty
import com.example.citadel.ui.overlay.MissionOverlay
import com.example.citadel.ui.scene.ArrivalState
import com.example.citadel.ui.scene.KingdomScene
import com.example.citadel.ui.scene.SceneState
import com.example.citadel.ui.scene.rememberArrivalState
import com.example.citadel.ui.theme.TimeOfDay
import com.example.citadel.ui.theme.getCurrentTimeOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentTab by remember { mutableStateOf(0) }
    
    // Initial sample promises
    var missions by remember {
        mutableStateOf(
            listOf(
                Mission(1, "Study 30 minutes of Kotlin basics", MissionDifficulty.MODERATE),
                Mission(2, "Drink fresh water at midday", MissionDifficulty.MINOR),
                Mission(3, "Complete the Citadel V1 architectural design", MissionDifficulty.MAJOR)
            )
        )
    }

    val completedCount = missions.count { it.isCompleted }
    val totalCount = missions.size
    val campfireIntensity by animateFloatAsState(
        targetValue = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat(),
        label = "CampfireIntensity"
    )

    // Debug override for time of day (toggling sky cycles DAWN -> DAY -> DUSK -> NIGHT)
    var debugTimeOfDayOverride by remember { mutableStateOf<TimeOfDay?>(null) }
    val timeOfDay = debugTimeOfDayOverride ?: getCurrentTimeOfDay()

    // Guardian message cycle index
    var activeMessageIndex by remember { mutableStateOf(0) }
    LaunchedEffect(timeOfDay) {
        activeMessageIndex = 0
    }

    // 7-Stage 5.0-Second Cinematic Arrival Sequence Driver
    val arrivalState by rememberArrivalState(durationMs = 5000)

    // Persistent Parallax & Darkening Scroll Controller
    val scrollController = rememberSceneScrollController()

    val sceneState = SceneState(
        timeOfDay = timeOfDay,
        campfireIntensity = campfireIntensity,
        completedCount = completedCount,
        sceneAlpha = scrollController.sceneAlpha
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Hearth") },
                    label = { Text("Hearth", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Chronicles") },
                    label = { Text("Chronicles", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Sanctuary") },
                    label = { Text("Sanctuary", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ActiveHearthScene(
                    sceneState = sceneState,
                    arrivalState = arrivalState,
                    scrollController = scrollController,
                    missions = missions,
                    activeMessageIndex = activeMessageIndex,
                    onTimeOfDayCycled = { debugTimeOfDayOverride = it },
                    onCompanionCycled = { activeMessageIndex = (activeMessageIndex + 1) },
                    onToggleMission = { missionId ->
                        missions = missions.map {
                            if (it.id == missionId) it.copy(isCompleted = !it.isCompleted) else it
                        }
                    }
                )
                1 -> ChroniclePlaceholder()
                2 -> SanctuaryPlaceholder()
            }
        }
    }
}

@Composable
fun ActiveHearthScene(
    sceneState: SceneState,
    arrivalState: ArrivalState,
    scrollController: SceneScrollController,
    missions: List<Mission>,
    activeMessageIndex: Int,
    onTimeOfDayCycled: (TimeOfDay) -> Unit,
    onCompanionCycled: () -> Unit,
    onToggleMission: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 0: Kingdom Scene (World comes first)
        KingdomScene(
            sceneState = sceneState,
            arrivalState = arrivalState,
            onTimeOfDayCycled = onTimeOfDayCycled,
            onCompanionCycled = onCompanionCycled,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = scrollController.parallaxOffsetPx
                }
        )

        // Layer 0.5: Atmospheric Dark Scrim (Darks background as user scrolls down into journal)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrollController.backgroundScrimAlpha))
        )

        // Layer 1: Foreground Scrollable Interface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollController.scrollState)
                .padding(horizontal = 24.dp)
        ) {
            // Transparent landing spacer to display world on arrival
            Spacer(modifier = Modifier.height(380.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Greeting & Companion Overlay (emerging in Stage 6)
                GreetingOverlay(
                    timeOfDay = sceneState.timeOfDay,
                    activeMessageIndex = activeMessageIndex,
                    arrivalState = arrivalState,
                    onCompanionCycled = onCompanionCycled
                )

                // Promises Journal Overlay (emerging in Stage 7)
                MissionOverlay(
                    missions = missions,
                    arrivalState = arrivalState,
                    onToggleMission = onToggleMission
                )
            }

            // Bottom Spacer for scrolling comfort
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ChroniclePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Chronicles",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The records of past seasons are quiet.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SanctuaryPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sanctuary Settings",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The archives rest here. Rest easy.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
