package dev.atharva.citadel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.atharva.citadel.core.time.SkyMoment

/**
 * The Citadel's theme follows the sky.
 *
 * [CitadelPalette] moves continuously through the day; the Material scheme is derived
 * from it so any standard component lands in the right light automatically. Gold stays
 * primary at every hour — see the note in Palette.kt.
 */
val LocalCitadelPalette: ProvidableCompositionLocal<CitadelPalette> =
    staticCompositionLocalOf { paletteAt(12 * 60) }

@Composable
fun CitadelTheme(
    sky: SkyMoment,
    content: @Composable () -> Unit
) {
    // Re-derived only when the light has moved enough to matter, not on every frame.
    val palette = remember(sky.minuteOfDay / 4) { paletteAt(sky.minuteOfDay) }

    val colorScheme = remember(palette) {
        darkColorScheme(
            primary = DawnGold,
            onPrimary = Color0D,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = DawnGoldBright,
            secondary = TwilightViolet,
            onSecondary = Ivory,
            tertiary = AncientForest,
            onTertiary = Ivory,
            background = palette.background,
            onBackground = Ivory,
            surface = palette.surface,
            onSurface = Ivory,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = IvoryDim,
            surfaceContainer = palette.surfaceElevated,
            surfaceContainerHigh = palette.surfaceElevated,
            outline = palette.outline,
            outlineVariant = palette.outline,
            error = DangerRed,
            onError = Ivory,
            scrim = Color0D
        )
    }

    CompositionLocalProvider(LocalCitadelPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CitadelTypography,
            shapes = CitadelShapes,
            content = content
        )
    }
}

private val Color0D = androidx.compose.ui.graphics.Color(0xFF07050A)

/** Shorthand for the ambient palette, for the places that need more than Material exposes. */
val citadelPalette: CitadelPalette
    @Composable @ReadOnlyComposable get() = LocalCitadelPalette.current
