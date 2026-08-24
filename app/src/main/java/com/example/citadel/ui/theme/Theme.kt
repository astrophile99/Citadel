package com.example.citadel.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import java.util.Calendar

enum class TimeOfDay {
    DAWN,  // 6:00 AM - 11:59 AM
    DAY,   // 12:00 PM - 5:59 PM
    DUSK,  // 6:00 PM - 8:59 PM
    NIGHT  // 9:00 PM - 5:59 AM
}

fun getCurrentTimeOfDay(): TimeOfDay {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..11 -> TimeOfDay.DAWN
        in 12..17 -> TimeOfDay.DAY
        in 18..20 -> TimeOfDay.DUSK
        else -> TimeOfDay.NIGHT
    }
}

private val DawnColorScheme = darkColorScheme(
    primary = DawnGold,
    secondary = TwilightViolet,
    tertiary = AncientForest,
    background = DawnBackground,
    surface = DawnSurface,
    surfaceVariant = DawnSurfaceElevated,
    onPrimary = DawnBackground,
    onSecondary = IvoryText,
    onTertiary = IvoryText,
    onBackground = IvoryText,
    onSurface = IvoryText,
    onSurfaceVariant = SecondaryGray,
    outline = MutedBorders,
    error = DangerRed,
    onError = IvoryText
)

private val DayColorScheme = darkColorScheme(
    primary = DawnGold,
    secondary = TwilightViolet,
    tertiary = AncientForest,
    background = DayBackground,
    surface = DaySurface,
    surfaceVariant = DaySurfaceElevated,
    onPrimary = DayBackground,
    onSecondary = IvoryText,
    onTertiary = IvoryText,
    onBackground = IvoryText,
    onSurface = IvoryText,
    onSurfaceVariant = SecondaryGray,
    outline = MutedBorders,
    error = DangerRed,
    onError = IvoryText
)

private val DuskColorScheme = darkColorScheme(
    primary = DawnGold,
    secondary = TwilightViolet,
    tertiary = AncientForest,
    background = DuskBackground,
    surface = DuskSurface,
    surfaceVariant = DuskSurfaceElevated,
    onPrimary = DuskBackground,
    onSecondary = IvoryText,
    onTertiary = IvoryText,
    onBackground = IvoryText,
    onSurface = IvoryText,
    onSurfaceVariant = SecondaryGray,
    outline = MutedBorders,
    error = DangerRed,
    onError = IvoryText
)

private val NightColorScheme = darkColorScheme(
    primary = DawnGold,
    secondary = TwilightViolet,
    tertiary = AncientForest,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceElevated,
    onPrimary = NightBackground,
    onSecondary = IvoryText,
    onTertiary = IvoryText,
    onBackground = IvoryText,
    onSurface = IvoryText,
    onSurfaceVariant = SecondaryGray,
    outline = MutedBorders,
    error = DangerRed,
    onError = IvoryText
)

@Composable
fun CitadelTheme(
    timeOfDay: TimeOfDay = getCurrentTimeOfDay(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (timeOfDay) {
        TimeOfDay.DAWN -> DawnColorScheme
        TimeOfDay.DAY -> DayColorScheme
        TimeOfDay.DUSK -> DuskColorScheme
        TimeOfDay.NIGHT -> NightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}