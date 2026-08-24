package dev.atharva.citadel.system

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dev.atharva.citadel.MainActivity
import dev.atharva.citadel.R
import java.util.Calendar
import kotlin.math.abs

/**
 * Whispers from the kingdom.
 *
 * These are not reminders. They never mention what is unfinished, never count anything,
 * and never ask the Commander to come back. They mention the weather and the hour, and
 * then they stop talking.
 *
 * Deliberately scheduled with [AlarmManager.setInexactRepeating]: a whisper that lands
 * twenty minutes late is still a whisper, and this costs no exact-alarm permission and
 * almost no battery.
 */
object Whispers {

    const val CHANNEL_ID = "citadel.whispers"

    private const val ACTION_WHISPER = "dev.atharva.citadel.WHISPER"
    private const val EXTRA_KIND = "kind"
    private const val KIND_DAWN = "dawn"
    private const val KIND_EVENING = "evening"

    private const val REQUEST_DAWN = 4001
    private const val REQUEST_EVENING = 4002
    private const val NOTIFICATION_ID = 7

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Whispers from the kingdom",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Two quiet messages a day. Never about what is unfinished."
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Re-arms both daily whispers, or clears them if the Commander has them turned off. */
    fun apply(context: Context, enabled: Boolean, dawnMinute: Int, eveningMinute: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context, alarms, REQUEST_DAWN, KIND_DAWN)
        cancel(context, alarms, REQUEST_EVENING, KIND_EVENING)
        if (!enabled) return

        schedule(context, alarms, REQUEST_DAWN, KIND_DAWN, dawnMinute)
        schedule(context, alarms, REQUEST_EVENING, KIND_EVENING, eveningMinute)
    }

    private fun schedule(
        context: Context,
        alarms: AlarmManager,
        requestCode: Int,
        kind: String,
        minuteOfDay: Int
    ) {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        runCatching {
            alarms.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent(context, requestCode, kind)
            )
        }
    }

    private fun cancel(context: Context, alarms: AlarmManager, requestCode: Int, kind: String) {
        runCatching { alarms.cancel(pendingIntent(context, requestCode, kind)) }
    }

    private fun pendingIntent(context: Context, requestCode: Int, kind: String): PendingIntent {
        val intent = Intent(context, WhisperReceiver::class.java).apply {
            action = ACTION_WHISPER
            putExtra(EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun post(context: Context, kind: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val text = lineFor(kind)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_whisper)
            .setContentTitle("Citadel")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private val DAWN_LINES = listOf(
        "Dawn has arrived, Commander. The mist is coming off the meadows.",
        "The gates are open. The scouts brought back clear roads.",
        "Morning over the Citadel. The kettle is on whenever you are.",
        "First light on the eastern wall. Nothing is urgent."
    )

    private val EVENING_LINES = listOf(
        "The lanterns are lit along the wall. The scouts await tomorrow's orders.",
        "The fire is stoked for the evening. Join us when you are ready.",
        "The stars are clear over the Citadel tonight.",
        "The watch is changing. Tomorrow can be given a shape, if you like."
    )

    private fun lineFor(kind: String): String {
        val lines = if (kind == KIND_DAWN) DAWN_LINES else EVENING_LINES
        // Stable within a day, different across days.
        val seed = abs((System.currentTimeMillis() / 86_400_000L).toInt() * 31 + kind.hashCode())
        return lines[seed % lines.size]
    }
}

/** Delivers a whisper. Does no work beyond building one notification. */
class WhisperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        Whispers.post(context, kind)
    }
}

/** Alarms do not survive a reboot, so they are re-armed here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val prefs = context.getSharedPreferences("citadel.settings", Context.MODE_PRIVATE)
        Whispers.apply(
            context = context,
            enabled = prefs.getBoolean("whispers", false),
            dawnMinute = prefs.getInt("dawn_whisper", 7 * 60 + 30),
            eveningMinute = prefs.getInt("evening_whisper", 21 * 60)
        )
    }
}
