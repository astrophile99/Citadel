package dev.atharva.citadel.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import dev.atharva.citadel.CitadelApp
import dev.atharva.citadel.MainActivity
import dev.atharva.citadel.R
import dev.atharva.citadel.core.time.DayPhase
import dev.atharva.citadel.core.time.SkyMoment
import dev.atharva.citadel.data.model.CitadelData
import dev.atharva.citadel.domain.DaySweep
import dev.atharva.citadel.domain.DayView
import dev.atharva.citadel.domain.WhisperRoute
import dev.atharva.citadel.domain.WorldState
import dev.atharva.citadel.system.Whispers
import dev.atharva.citadel.ui.scene.SceneRenderer
import dev.atharva.citadel.ui.scene.describe
import dev.atharva.citadel.ui.theme.paletteAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The Citadel on the home screen.
 *
 * The widget only ever reads. It projects today with the same day sweep the app uses, so
 * missions prepared last night appear this morning even if the app has not been opened —
 * but it never writes, so opening the app still greets the Commander properly.
 *
 * Tapping it opens the Citadel. Promises are kept inside the app, where the world can
 * answer; a checkbox on the home screen would skip the one moment that matters.
 */
class CitadelWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshInBackground(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refreshInBackground(context, intArrayOf(appWidgetId))
    }

    private fun refreshInBackground(context: Context, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val data = container(context).store.read()
                render(context, ids, data)
            } catch (error: Throwable) {
                Log.e(TAG, "Could not draw the widget", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CitadelWidget"

        /** Redraws every placed widget. Pass [data] when the caller already holds it. */
        suspend fun refreshAll(context: Context, data: CitadelData? = null) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, CitadelWidget::class.java))
            if (ids.isEmpty()) return
            runCatching {
                render(context, ids, data ?: container(context).store.read())
            }.onFailure { Log.e(TAG, "Could not refresh the widget", it) }
        }

        /** True when the launcher can place the widget for the Commander in one tap. */
        fun canRequestPin(context: Context): Boolean =
            AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true

        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context) ?: return false
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(
                ComponentName(context, CitadelWidget::class.java),
                null,
                null
            )
        }

        private fun container(context: Context) =
            (context.applicationContext as CitadelApp).container

        private fun render(context: Context, ids: IntArray, data: CitadelData) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val container = container(context)
            val clock = container.clock
            val todayKey = clock.todayKey()
            val now = clock.epochMillis()

            val projected = DaySweep.project(data, todayKey, now)
            val day = DayView.of(projected, todayKey)
            val sky = SkyMoment.at(clock.now())
            val world = WorldState.from(projected, sky, todayKey, now, ambience = false)

            ids.forEach { id ->
                val views = build(context, manager, id, day, world)
                manager.updateAppWidget(id, views)
            }
        }

        private fun build(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            day: DayView,
            world: WorldState
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_citadel)
            val size = sizeOf(context, manager, id)

            // How much the widget can say depends on how tall it is.
            val rows = when {
                size.heightDp >= 230 -> 3
                size.heightDp >= 160 -> 2
                else -> 0
            }
            val shown = day.today.take(rows)

            // Where the text begins, so the world can sit above it and dissolve beneath it.
            val textBlockDp = 14 + 24 + shown.size * 21 + 7 + 17 + 4
            val textTop = (size.heightDp - textBlockDp).coerceAtLeast(24) / size.heightDp.toFloat()
            val compact = rows == 0

            val density = context.resources.displayMetrics.density
            val scale = (MAX_BITMAP_WIDTH / (size.widthDp * density)).coerceAtMost(1f)
            val widthPx = (size.widthDp * density * scale).roundToInt().coerceAtLeast(64)
            val heightPx = (size.heightDp * density * scale).roundToInt().coerceAtLeast(32)
            val corner = context.resources.getDimension(R.dimen.widget_corner) * scale

            val bitmap = SceneRenderer.render(
                widthPx = widthPx,
                heightPx = heightPx,
                density = density * scale,
                world = world,
                horizonAt = (textTop * if (compact) 0.74f else 0.64f).coerceIn(0.18f, 0.5f),
                veilColor = paletteAt(world.sky.minuteOfDay).veil,
                zoom = if (compact) 0.66f else 0.84f,
                veilFrom = (textTop - 0.16f).coerceIn(0.12f, 0.8f),
                veilTo = (textTop + 0.12f).coerceIn(0.3f, 0.95f),
                cornerRadiusPx = corner
            )
            views.setImageViewBitmap(R.id.widget_scene, bitmap)
            views.setContentDescription(R.id.widget_scene, describe(world))

            views.setTextViewText(R.id.widget_greeting, greeting(world.sky.phase))
            views.setTextViewText(R.id.widget_status, status(day, shown.size))

            ROWS.forEachIndexed { index, row ->
                val mission = shown.getOrNull(index)
                if (mission == null) {
                    views.setViewVisibility(row.container, View.GONE)
                } else {
                    views.setViewVisibility(row.container, View.VISIBLE)
                    views.setTextViewText(row.title, mission.title)
                    views.setTextColor(row.title, if (mission.isComplete) KEPT_TEXT else OPEN_TEXT)
                    views.setImageViewResource(
                        row.mark,
                        if (mission.isComplete) R.drawable.widget_ring_kept else R.drawable.widget_ring
                    )
                    views.setContentDescription(
                        row.container,
                        "${mission.title}, ${if (mission.isComplete) "kept" else "not yet kept"}"
                    )
                }
            }

            views.setOnClickPendingIntent(android.R.id.background, openApp(context))
            return views
        }

        private fun greeting(phase: DayPhase): String = when (phase) {
            DayPhase.DAWN -> "Good morning, Commander."
            DayPhase.DAY -> "The day is yours."
            DayPhase.DUSK -> "Welcome home, Commander."
            DayPhase.NIGHT -> "The watch is set."
        }

        private fun status(day: DayView, shown: Int): String {
            val more = (day.prepared - shown).takeIf { shown > 0 && it > 0 }
            val base = when {
                day.unwritten -> "Nothing set down yet · tap to begin"
                day.allKept -> "Every lantern is lit"
                else -> "${day.kept} of ${day.prepared} lanterns lit"
            }
            return if (more != null && !day.allKept) "$base · $more more inside" else base
        }

        private fun openApp(context: Context): PendingIntent {
            // The widget shows today, so it opens onto today — wherever the app was left.
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(Whispers.EXTRA_ROUTE, WhisperRoute.HEARTH.name)
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private data class WidgetSize(val widthDp: Int, val heightDp: Int)

        /** The size the launcher actually gave us, in the orientation we are in. */
        private fun sizeOf(context: Context, manager: AppWidgetManager, id: Int): WidgetSize {
            val options = manager.getAppWidgetOptions(id)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val width = if (portrait) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            }
            val height = if (portrait) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            }
            return WidgetSize(
                widthDp = width.takeIf { it > 0 } ?: 250,
                heightDp = height.takeIf { it > 0 } ?: 110
            )
        }

        private class Row(val container: Int, val mark: Int, val title: Int)

        private val ROWS = listOf(
            Row(R.id.widget_row_1, R.id.widget_mark_1, R.id.widget_title_1),
            Row(R.id.widget_row_2, R.id.widget_mark_2, R.id.widget_title_2),
            Row(R.id.widget_row_3, R.id.widget_mark_3, R.id.widget_title_3)
        )

        private val OPEN_TEXT = 0xFFF4F1EA.toInt()
        private val KEPT_TEXT = 0x99B7BDC8.toInt()
        private const val REQUEST_OPEN = 5100

        /** Keeps the RemoteViews bitmap well inside every launcher's memory budget. */
        private const val MAX_BITMAP_WIDTH = 960f
    }
}
