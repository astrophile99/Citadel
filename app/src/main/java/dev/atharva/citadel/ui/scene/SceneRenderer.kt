package dev.atharva.citadel.ui.scene

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.atharva.citadel.domain.WorldState

/**
 * Draws the Citadel into a bitmap, outside of any composition.
 *
 * This is how the widget and the store artwork get the *same* world as the app: the exact
 * sky, land and weather painters, run once on an offscreen canvas instead of every frame
 * on screen. Nothing here is a second illustration that could drift from the first.
 */
object SceneRenderer {

    /**
     * @param horizonAt where the horizon should sit, as a fraction of the output height.
     *   The scene is laid out on a virtual phone-shaped canvas and cropped, so the land
     *   keeps its proportions at any output size.
     * @param zoom below 1 pulls the camera back so more of the land fits a short frame.
     *   The Citadel stays anchored near the same point of the output either way.
     * @param veilFrom/veilTo the dissolve into [veilColor] behind any text, as fractions of
     *   the output height. Pass `veilFrom >= 1f` for no veil.
     */
    fun render(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        world: WorldState,
        horizonAt: Float,
        veilColor: Color,
        zoom: Float = 1f,
        veilFrom: Float = 1f,
        veilTo: Float = 1f,
        cornerRadiusPx: Float = 0f
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val image = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(image)
        val drawScope = CanvasDrawScope()
        val densityObj = Density(density)

        // The scene believes it is on a tall phone; we only keep the band we need.
        val virtualWidth = width / zoom.coerceIn(0.4f, 1f)
        val virtualHeight = virtualWidth * VIRTUAL_ASPECT
        val horizonY = virtualHeight * SCENE_HORIZON
        val offsetX = ANCHOR_X * (virtualWidth - width)
        val offsetY = horizonY - horizonAt * height

        val colors = SceneColors.of(world.sky)
        val ambients = dominantAmbients(world)

        canvas.save()
        canvas.translate(-offsetX, -offsetY)
        drawScope.draw(densityObj, LayoutDirection.Ltr, canvas, Size(virtualWidth, virtualHeight)) {
            drawSky(
                sky = world.sky,
                colors = colors,
                horizonY = horizonY,
                stars = StarField(count = 110),
                scratch = SkyScratch(),
                time = STILL_FRAME,
                ambients = ambients,
                reveal = 1f
            )
            drawLand(
                world = world,
                colors = colors,
                horizonY = horizonY,
                scratch = LandScratch(),
                time = STILL_FRAME,
                ambients = ambients,
                reveal = 1f
            )
            if (Ambient.MIST in ambients) {
                drawMist(
                    colors = colors,
                    horizonY = horizonY,
                    density = world.wildness.coerceIn(0f, 1f),
                    time = STILL_FRAME,
                    reveal = 1f
                )
            }
        }
        canvas.restore()

        drawScope.draw(densityObj, LayoutDirection.Ltr, canvas, Size(width.toFloat(), height.toFloat())) {
            if (veilFrom < 1f) {
                drawVeil(veilColor = veilColor, startFraction = veilFrom, endFraction = veilTo)
            }
            if (cornerRadiusPx > 0f) {
                // Clear the corners with an anti-aliased path. A clip would leave jagged
                // edges on a software canvas, and launchers before Android 12 will not
                // round a widget's image for us.
                val corners = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }
                drawPath(corners, Color.Black, blendMode = BlendMode.Clear)
            }
        }

        return image.asAndroidBitmap()
    }

    /** Roughly a modern phone: the proportions every painter was tuned against. */
    private const val VIRTUAL_ASPECT = 2.2f

    /** The point of the scene that stays put when zooming out — just left of the keep. */
    private const val ANCHOR_X = 0.62f
}
