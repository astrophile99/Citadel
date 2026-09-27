package dev.atharva.citadel.core.time

import android.content.Context

/** Release builds keep real time, always. */
internal object ClockProvider {
    fun create(@Suppress("UNUSED_PARAMETER") context: Context): CitadelClock = SystemClock
}
