package com.example.citadel.ui.scene

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.citadel.ui.theme.TimeOfDay

@Composable
fun KingdomScene(
    sceneState: SceneState,
    arrivalState: ArrivalState,
    onTimeOfDayCycled: (TimeOfDay) -> Unit,
    onCompanionCycled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "KingdomSceneAtmosphere")

    // Cloud animations
    val cloud1Progress by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Cloud1"
    )

    val cloud2Progress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(150000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Cloud2"
    )

    // Twinkling stars animations
    val starAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Star1"
    )
    val starAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Star2"
    )
    val starAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Star3"
    )

    // Sun / Moon / Lantern breathing animations
    val sunGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunGlow"
    )

    val lanternAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LanternAlpha"
    )

    // Campfire flicker & sparks animations
    val campfireFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CampfireFlicker"
    )

    val sparkProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spark1"
    )
    val sparkProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spark2"
    )
    val sparkProgress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spark3"
    )

    // Chimney smoke animations
    val smokeProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Smoke1"
    )
    val smokeProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Smoke2"
    )

    // Fog/Mist animations
    val fogProgress1 by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(70000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FogProgress1"
    )
    val fogProgress2 by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FogProgress2"
    )
    val fogAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FogAlpha"
    )

    // Bird wings flap animation (Dawn only)
    val birdFlap by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BirdFlap"
    )

    // Dawn bird landing animated fractions
    val bird1RestingFraction by animateFloatAsState(
        targetValue = if (sceneState.timeOfDay == TimeOfDay.DAWN && sceneState.completedCount >= 1) 1.0f else 0.0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "Bird1Resting"
    )
    val bird2RestingFraction by animateFloatAsState(
        targetValue = if (sceneState.timeOfDay == TimeOfDay.DAWN && sceneState.completedCount >= 2) 1.0f else 0.0f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "Bird2Resting"
    )
    val bird3RestingFraction by animateFloatAsState(
        targetValue = if (sceneState.timeOfDay == TimeOfDay.DAWN && sceneState.completedCount >= 3) 1.0f else 0.0f,
        animationSpec = tween(1600, easing = FastOutSlowInEasing),
        label = "Bird3Resting"
    )

    // Persistent shape paths
    val mountainPath = remember { Path() }
    val hillPath1 = remember { Path() }
    val hillPath2 = remember { Path() }
    val foregroundHillPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp)
            .clip(RoundedCornerShape(20.dp))
            .graphicsLayer {
                scaleX = arrivalState.cameraScale
                scaleY = arrivalState.cameraScale
                translationY = arrivalState.cameraOffsetYDp.dp.toPx()
                alpha = sceneState.sceneAlpha * arrivalState.kingdomVisibility
            }
            .pointerInput(sceneState.timeOfDay) {
                detectTapGestures { offset ->
                    if (offset.y < size.height * 0.45f) {
                        val nextTime = when (sceneState.timeOfDay) {
                            TimeOfDay.DAWN -> TimeOfDay.DAY
                            TimeOfDay.DAY -> TimeOfDay.DUSK
                            TimeOfDay.DUSK -> TimeOfDay.NIGHT
                            TimeOfDay.NIGHT -> TimeOfDay.DAWN
                        }
                        onTimeOfDayCycled(nextTime)
                    } else {
                        onCompanionCycled()
                    }
                }
            }
    ) {
        // Layer 1: Sky, Stars, Sun/Moon
        drawSkyLayer(
            timeOfDay = sceneState.timeOfDay,
            starAlpha1 = starAlpha1,
            starAlpha2 = starAlpha2,
            starAlpha3 = starAlpha3,
            sunGlowScale = sunGlowScale
        )

        // Layer 2: Drifting Clouds & Arrival Parting
        drawCloudLayer(
            timeOfDay = sceneState.timeOfDay,
            cloud1Progress = cloud1Progress,
            cloud2Progress = cloud2Progress,
            cloudSeparation = arrivalState.cloudSeparation
        )

        // Layer 3: Kingdom Silhouette, Keep, Smoke, Windows, Lanterns, Birds, Companion
        drawKingdomLayer(
            sceneState = sceneState,
            primaryColor = primaryColor,
            smokeProgress1 = smokeProgress1,
            smokeProgress2 = smokeProgress2,
            lanternAlpha = lanternAlpha,
            birdFlap = birdFlap,
            bird1RestingFraction = bird1RestingFraction,
            bird2RestingFraction = bird2RestingFraction,
            bird3RestingFraction = bird3RestingFraction,
            mountainPath = mountainPath,
            hillPath1 = hillPath1,
            hillPath2 = hillPath2,
            foregroundHillPath = foregroundHillPath
        )

        // Layer 4: Atmosphere (Campfire Glow, Sparks, Valley Fog)
        drawAtmosphereLayer(
            sceneState = sceneState,
            arrivalState = arrivalState,
            campfireFlicker = campfireFlicker,
            sparkProgress1 = sparkProgress1,
            sparkProgress2 = sparkProgress2,
            sparkProgress3 = sparkProgress3,
            fogProgress1 = fogProgress1,
            fogProgress2 = fogProgress2,
            fogAlpha = fogAlpha
        )
    }
}
