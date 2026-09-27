package dev.atharva.citadel.system

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dev.atharva.citadel.CitadelApp
import dev.atharva.citadel.MainActivity
import dev.atharva.citadel.R
import dev.atharva.citadel.domain.DaySweep
import dev.atharva.citadel.domain.DayView
import dev.atharva.citadel.domain.WhisperCopy
import dev.atharva.citadel.domain.WhisperKind
import dev.atharva.citadel.domain.WhisperMessage
import dev.atharva.citadel.domain.WhisperRoute
import dev.atharva.citadel.domain.WhisperSchedule
import dev.atharva.citadel.domain.WhisperSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Whispers from the kingdom.
 *
 * Up to five a day — dawn and night to plan, midday, afternoon and evening to follow
 * through. They are written by [WhisperCopy], which is never allowed to use guilt, and
 * delivered under three restraints:
 *  - A new whisper replaces the last one. The shade never fills with a stack of them.
 *  - Nothing is sent while the Commander is in the app, or within half an hour of leaving.
 *  - A whisper that arrives very late (Doze can hold alarms) is dropped rather than sent
 *    at the wrong hour — "Dawn has arrived" at two in the afternoon is worse than nothing.
 *
 * Scheduling is one alarm at a time. [AlarmManager.setAndAllowWhileIdle] fires through Doze
 * without the exact-alarm permission (which Play restricts to alarm-clock and calendar apps),
 * but the system is free to deliver it anywhere within 75% of the remaining delay — so an
 * alarm set at night for dawn could arrive at noon. Instead the chain *approaches* each
 * whisper: while it is far away, a silent check-in is set a third of the way there, which
 * always lands before the whisper; once it is close, the whisper itself is set, and its
 * window is only a few minutes wide.
 */
object Whispers {

    const val CHANNEL_PLANS = "whispers.plans"
    const val CHANNEL_WATCH = "whispers.watch"
    private const val LEGACY_CHANNEL = "citadel.whispers"

    const val EXTRA_ROUTE = "dev.atharva.citadel.extra.ROUTE"

    private const val ACTION_WHISPER = "dev.atharva.citadel.action.WHISPER"
    private const val EXTRA_SLOT = "dev.atharva.citadel.extra.SLOT"
    private const val EXTRA_SCHEDULED_AT = "dev.atharva.citadel.extra.SCHEDULED_AT"

    private const val REQUEST_ALARM = 4100
    private const val NOTIFICATION_ID = 7

    /** Within this, the whisper itself is armed; its delivery window is then under ten minutes. */
    private const val APPROACH_MS = 12L * 60 * 1000
    private const val MIN_HOP_MS = 60L * 1000

    private const val QUIET_AFTER_VISIT_MS = 30L * 60 * 1000
    private const val TOO_LATE_MS = 90L * 60 * 1000
    private const val SHOWN_FOR_MS = 4L * 60 * 60 * 1000

    private const val TAG = "Whispers"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PLANS,
                "Planning the day",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "At dawn, to name the day's promises. At night, to prepare tomorrow."
                enableVibration(false)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WATCH,
                "Through the day",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Midday, afternoon and evening check-ins on the promises you set."
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }

    /** True when the system will actually show what we post. */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true
    }

    /**
     * Arms the next whisper, or disarms if whispers are off. Safe to call as often as
     * needed — on open, on every settings change, after boot, after a time-zone change.
     */
    fun reschedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(alarmIntent(context, null, 0L))

        val settings = container(context).settings.settings.value
        if (!settings.whispers) return

        val next = WhisperSchedule.next(
            now = LocalDateTime.now(),
            cadence = settings.whisperCadence,
            morningMinute = settings.dawnWhisperMinute,
            nightMinute = settings.eveningWhisperMinute
        ) ?: return

        val slotAt = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val remaining = slotAt - now

        // Far away: a silent check-in. Its latest possible delivery (1.75 × a third of the
        // way) is still before the whisper, so the approach can never overshoot.
        val (fireAt, slot) = if (remaining > APPROACH_MS) {
            (now + remaining / 3).coerceAtLeast(now + MIN_HOP_MS) to null
        } else {
            slotAt to next.slot
        }

        runCatching {
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                fireAt,
                alarmIntent(context, slot, slotAt)
            )
        }.onFailure { Log.w(TAG, "Could not arm the next whisper", it) }
    }

    /** Removes a whisper that is still showing. Called when the Commander arrives. */
    fun clearShown(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /**
     * Composes and posts the whisper for [slot]. [force] skips the timing restraints and is
     * used only by debug tooling to preview copy.
     */
    internal suspend fun deliver(
        context: Context,
        slot: WhisperSlot,
        scheduledAt: Long,
        force: Boolean = false
    ) {
        val container = container(context)
        val store = container.settings
        val settings = store.settings.value
        val now = System.currentTimeMillis()

        if (!force) {
            if (!settings.whispers || slot !in settings.whisperCadence.slots) return
            if (scheduledAt > 0L && now - scheduledAt > TOO_LATE_MS) return
            if (Presence.inForeground) return
            if (now - store.lastOpenedAtMillis < QUIET_AFTER_VISIT_MS) return
        }
        if (!canNotify(context)) return

        val todayKey = container.clock.todayKey()
        val data = DaySweep.project(container.store.read(), todayKey, now)
        val day = DayView.of(data, todayKey)
        val message = WhisperCopy.compose(
            slot = slot,
            day = day,
            alreadyCelebrated = store.lastCelebratedDayKey == todayKey
        ) ?: return

        if (message.celebrates) store.lastCelebratedDayKey = todayKey
        post(context, message)
    }

    private fun post(context: Context, message: WhisperMessage) {
        val channel = if (message.slot.kind == WhisperKind.PLAN) CHANNEL_PLANS else CHANNEL_WATCH

        // On the lock screen the kingdom speaks, but mission titles stay private.
        val publicVersion = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_whisper)
            .setColor(GOLD)
            .setContentTitle(message.title)
            .setContentText("A whisper from the Citadel.")
            .build()

        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_whisper)
            .setColor(GOLD)
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setStyle(Notification.BigTextStyle().bigText(message.text))
            .setContentIntent(openApp(context, message.route))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setTimeoutAfter(SHOWN_FOR_MS)
            .setShowWhen(true)
            .build()

        runCatching {
            context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }.onFailure { Log.w(TAG, "Could not post a whisper", it) }
    }

    private fun openApp(context: Context, route: WhisperRoute): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route.name)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_ALARM + 10 + route.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The one pending alarm. Extras do not affect identity, so cancel() always finds it. */
    private fun alarmIntent(context: Context, slot: WhisperSlot?, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, WhisperReceiver::class.java).apply {
            action = ACTION_WHISPER
            slot?.let { putExtra(EXTRA_SLOT, it.name) }
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun slotFrom(intent: Intent): WhisperSlot? =
        intent.getStringExtra(EXTRA_SLOT)?.let { name -> WhisperSlot.entries.firstOrNull { it.name == name } }

    internal fun scheduledAtFrom(intent: Intent): Long = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)

    internal fun isWhisper(intent: Intent) = intent.action == ACTION_WHISPER

    private fun container(context: Context) = (context.applicationContext as CitadelApp).container

    private val GOLD = 0xFFD4A84F.toInt()
}

/** Delivers a whisper if one is due (check-ins carry none), then arms the next step. */
class WhisperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Whispers.isWhisper(intent)) return
        val slot = Whispers.slotFrom(intent)
        val scheduledAt = Whispers.scheduledAtFrom(intent)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (slot != null) Whispers.deliver(context, slot, scheduledAt)
            } catch (error: Throwable) {
                Log.e("Whispers", "Whisper delivery failed", error)
            } finally {
                Whispers.reschedule(context)
                pending.finish()
            }
        }
    }
}

/**
 * Alarms do not survive a reboot, and a changed clock or time zone moves every whisper,
 * so all of those simply re-arm the next one.
 */
class WhisperRearmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Whispers.reschedule(context)
        }
    }
}

/** Whether the Citadel is on screen right now. Set by the activity, read by the whispers. */
object Presence {
    @Volatile
    var inForeground: Boolean = false
}
