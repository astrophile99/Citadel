package dev.atharva.citadel.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.Log
import androidx.core.graphics.createBitmap
import dev.atharva.citadel.R
import dev.atharva.citadel.core.time.SkyMoment
import dev.atharva.citadel.domain.WhisperSlot
import dev.atharva.citadel.domain.WorldState
import dev.atharva.citadel.system.Whispers
import dev.atharva.citadel.ui.scene.SceneRenderer
import dev.atharva.citadel.ui.theme.paletteAt
import dev.atharva.citadel.widget.CitadelWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Developer tools, compiled into debug builds only.
 *
 *   adb shell am broadcast -a dev.atharva.citadel.debug.TOOLS -p dev.atharva.citadel --es cmd whisper --es slot DAWN
 *   adb shell am broadcast -a dev.atharva.citadel.debug.TOOLS -p dev.atharva.citadel --es cmd widget
 *   adb shell am broadcast -a dev.atharva.citadel.debug.TOOLS -p dev.atharva.citadel --es cmd art
 *
 * `art` writes the Play Store icon and feature graphic to files/store/, rendered by the same
 * painters the app draws with.
 */
class DebugToolsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.getStringExtra("cmd")) {
                    "whisper" -> {
                        val slot = WhisperSlot.entries.firstOrNull { it.name == intent.getStringExtra("slot") }
                            ?: WhisperSlot.DAWN
                        Whispers.deliver(context, slot, scheduledAt = 0L, force = true)
                    }
                    "widget" -> CitadelWidget.refreshAll(context)
                    "reschedule" -> Whispers.reschedule(context)
                    "art" -> renderStoreArt(context)
                    else -> Log.w(TAG, "Unknown command")
                }
                Log.i(TAG, "Done: ${intent.getStringExtra("cmd")}")
            } catch (error: Throwable) {
                Log.e(TAG, "Tool failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private fun renderStoreArt(context: Context) {
        val out = File(context.filesDir, "store").apply { mkdirs() }

        // Feature graphic, 1024 x 500: the Citadel at sunset, fully lit, a month of keeping.
        val dusk = SkyMoment.at(18 * 60 + 32)
        val world = WorldState(
            sky = dusk,
            keptToday = 5,
            preparedToday = 5,
            lightFraction = 0.95f,
            lanternsLit = WorldState.WALL_LANTERNS,
            villageWarmth = 1f,
            ambience = false
        )
        val feature = SceneRenderer.render(
            widthPx = 1024,
            heightPx = 500,
            density = 2f,
            world = world,
            horizonAt = 0.66f,
            veilColor = paletteAt(dusk.minuteOfDay).veil,
            zoom = 0.92f
        )
        Canvas(feature).apply {
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                textSize = 104f
                color = 0xFFF4F1EA.toInt()
                letterSpacing = -0.01f
            }
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textSize = 30f
                color = 0xE6E9E1D2.toInt()
            }
            drawText("Citadel", 66f, 176f, title)
            drawText("Keep the promises you make to yourself.", 70f, 226f, line)
        }
        save(feature, File(out, "featureGraphic.png"))

        // Play icon, 512 x 512: the adaptive icon's visible area, drawn at full bleed.
        val adaptive = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val icon = createBitmap(512, 512)
        Canvas(icon).apply {
            // Layers are 108 units with the inner 72 visible; scale so those 72 fill 512.
            val inset = 128
            adaptive.background?.setBounds(-inset, -inset, 512 + inset, 512 + inset)
            adaptive.background?.draw(this)
            adaptive.foreground?.setBounds(-inset, -inset, 512 + inset, 512 + inset)
            adaptive.foreground?.draw(this)
        }
        save(icon, File(out, "icon.png"))
    }

    private fun save(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i(TAG, "Wrote ${file.absolutePath}")
    }

    private companion object {
        const val TAG = "CitadelTools"
    }
}
