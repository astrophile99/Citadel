package dev.atharva.citadel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ---- fixed identity ----------------------------------------------------------------
// These never change with the hour. Gold is the only interactive accent, at every time
// of day, because a violet button on an obsidian background fails contrast at exactly
// the hour someone is most likely to be reading in the dark.

val DawnGold = Color(0xFFD4A84F)
val DawnGoldBright = Color(0xFFF0C674)
val AncientForest = Color(0xFF4E9068)
val TwilightViolet = Color(0xFF6C63A8)
val EmberOrange = Color(0xFFC9762F)
val DangerRed = Color(0xFFB54B4B)

val Ivory = Color(0xFFF4F1EA)
val IvoryDim = Color(0xFFB7BDC8)
val Muted = Color(0xFF6B7280)

/**
 * The colours of one instant.
 *
 * Rather than four fixed themes that snap at fixed hours, the Citadel keeps a handful of
 * anchor palettes and moves continuously between them. Nobody notices the change while
 * it happens, which is the point — the light is simply different at seven than at four.
 */
@Immutable
data class CitadelPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    /** The wash pulled up behind the mission list so the world dissolves into it. */
    val veil: Color
)

private class PaletteAnchor(
    val minute: Int,
    val palette: CitadelPalette
)

private val ANCHORS = listOf(
    // Deep night
    PaletteAnchor(
        3 * 60,
        CitadelPalette(
            background = Color(0xFF07050A),
            surface = Color(0xFF120F1A),
            surfaceElevated = Color(0xFF1B1726),
            outline = Color(0xFF2A2438),
            veil = Color(0xFF07050A)
        )
    ),
    // Dawn
    PaletteAnchor(
        6 * 60 + 40,
        CitadelPalette(
            background = Color(0xFF0D1420),
            surface = Color(0xFF16202F),
            surfaceElevated = Color(0xFF1F2B3D),
            outline = Color(0xFF2E3B4F),
            veil = Color(0xFF0D1420)
        )
    ),
    // Full day
    PaletteAnchor(
        12 * 60 + 30,
        CitadelPalette(
            background = Color(0xFF0D1117),
            surface = Color(0xFF18212D),
            surfaceElevated = Color(0xFF222C39),
            outline = Color(0xFF303B4A),
            veil = Color(0xFF0D1117)
        )
    ),
    // Dusk
    PaletteAnchor(
        18 * 60 + 40,
        CitadelPalette(
            background = Color(0xFF141010),
            surface = Color(0xFF221A19),
            surfaceElevated = Color(0xFF2E2422),
            outline = Color(0xFF3D2F2C),
            veil = Color(0xFF141010)
        )
    ),
    // Night falling
    PaletteAnchor(
        21 * 60 + 30,
        CitadelPalette(
            background = Color(0xFF07050A),
            surface = Color(0xFF120F1A),
            surfaceElevated = Color(0xFF1B1726),
            outline = Color(0xFF2A2438),
            veil = Color(0xFF07050A)
        )
    )
)

private const val MINUTES_PER_DAY = 1440

/** The palette for a given minute of the day, interpolated between anchors. */
fun paletteAt(minuteOfDay: Int): CitadelPalette {
    val m = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)

    val next = ANCHORS.firstOrNull { it.minute > m }
    val previous = ANCHORS.lastOrNull { it.minute <= m }

    // Before the first anchor or after the last one, wrap around midnight.
    if (previous == null) {
        val last = ANCHORS.last()
        val first = ANCHORS.first()
        val span = (first.minute + MINUTES_PER_DAY - last.minute).toFloat()
        val travelled = (m + MINUTES_PER_DAY - last.minute).toFloat()
        return lerpPalette(last.palette, first.palette, ease(travelled / span))
    }
    if (next == null) {
        val last = ANCHORS.last()
        val first = ANCHORS.first()
        val span = (first.minute + MINUTES_PER_DAY - last.minute).toFloat()
        val travelled = (m - last.minute).toFloat()
        return lerpPalette(last.palette, first.palette, ease(travelled / span))
    }

    val span = (next.minute - previous.minute).toFloat()
    val travelled = (m - previous.minute).toFloat()
    return lerpPalette(previous.palette, next.palette, ease(travelled / span))
}

private fun lerpPalette(a: CitadelPalette, b: CitadelPalette, t: Float) = CitadelPalette(
    background = lerp(a.background, b.background, t),
    surface = lerp(a.surface, b.surface, t),
    surfaceElevated = lerp(a.surfaceElevated, b.surfaceElevated, t),
    outline = lerp(a.outline, b.outline, t),
    veil = lerp(a.veil, b.veil, t)
)

private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
