package dev.atharva.citadel.core.time

import android.content.Context
import java.io.File
import java.time.LocalDateTime

/**
 * Debug builds only: the hour can be pinned so every time of day can be seen and tested
 * without waiting for it. The date is always real, so the day sweep behaves normally.
 *
 *   adb shell "run-as dev.atharva.citadel sh -c 'echo 18:40 > files/debug_clock'"
 *   adb shell "run-as dev.atharva.citadel rm files/debug_clock"
 */
internal object ClockProvider {
    fun create(context: Context): CitadelClock = PinnedClock(File(context.filesDir, "debug_clock"))
}

private class PinnedClock(private val file: File) : CitadelClock {
    override fun now(): LocalDateTime {
        val real = LocalDateTime.now()
        val pinned = runCatching { if (file.exists()) file.readText().trim() else "" }.getOrDefault("")
        val parts = pinned.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
            real.withHour(hour).withMinute(minute)
        } else {
            real
        }
    }

    override fun epochMillis(): Long = System.currentTimeMillis()
}
