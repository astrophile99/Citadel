package dev.atharva.citadel.ui.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import dev.atharva.citadel.core.time.DayPhase
import dev.atharva.citadel.domain.WorldState
import kotlin.math.PI
import kotlin.math.sin

/** Every path the land needs, allocated once and rewound each frame. */
class LandScratch {
    val far = Path()
    val near = Path()
    val ridge = Path()
    val foreground = Path()
    val roof = Path()
    val gate = Path()
    val banner = Path()
    val figure = Path()
}

private val LanternGold = Color(0xFFFFC96B)
private val WindowWarm = Color(0xFFFFD98A)
private val WindowCold = Color(0xFF1A1D28)
private val FireCore = Color(0xFFFFB84D)
private val FireDeep = Color(0xFFD2641F)

/**
 * The Citadel and the ground it stands on.
 *
 * Everything here is silhouette plus warmth: the shapes are near-black, and the only
 * colour in the whole landscape is light that the Commander has earned. That is the
 * entire reward system, drawn.
 */
fun DrawScope.drawLand(
    world: WorldState,
    colors: SceneColors,
    horizonY: Float,
    scratch: LandScratch,
    time: Float,
    ambients: Set<Ambient>,
    reveal: Float
) {
    val w = size.width
    val h = size.height
    if (reveal <= 0.01f) return

    val alpha = reveal
    val u = w * 0.026f

    // ---- distant range -------------------------------------------------------------
    val farBase = horizonY + h * 0.012f
    scratch.far.reset()
    scratch.far.moveTo(0f, farBase)
    scratch.far.lineTo(w * 0.06f, horizonY - h * 0.055f)
    scratch.far.lineTo(w * 0.17f, horizonY - h * 0.020f)
    scratch.far.lineTo(w * 0.28f, horizonY - h * 0.092f)
    scratch.far.lineTo(w * 0.39f, horizonY - h * 0.030f)
    scratch.far.lineTo(w * 0.52f, horizonY - h * 0.070f)
    scratch.far.lineTo(w * 0.66f, horizonY - h * 0.026f)
    scratch.far.lineTo(w * 0.80f, horizonY - h * 0.082f)
    scratch.far.lineTo(w * 0.92f, horizonY - h * 0.028f)
    scratch.far.lineTo(w, horizonY - h * 0.052f)
    scratch.far.lineTo(w, farBase)
    scratch.far.close()
    drawPath(scratch.far, colors.mountainFar.copy(alpha = 0.75f * alpha))

    // ---- nearer range --------------------------------------------------------------
    // Reaches well below the ridge, which is drawn over it. A shallower base left a sliver
    // of bare ground-colour between the two — a ruler-straight line across the valley.
    val nearBase = horizonY + h * 0.16f
    scratch.near.reset()
    scratch.near.moveTo(0f, nearBase)
    scratch.near.lineTo(0f, horizonY + h * 0.004f)
    scratch.near.lineTo(w * 0.14f, horizonY - h * 0.028f)
    scratch.near.lineTo(w * 0.31f, horizonY + h * 0.006f)
    scratch.near.lineTo(w * 0.46f, horizonY - h * 0.040f)
    scratch.near.lineTo(w * 0.61f, horizonY - h * 0.004f)
    scratch.near.lineTo(w * 0.79f, horizonY - h * 0.034f)
    scratch.near.lineTo(w, horizonY + h * 0.002f)
    scratch.near.lineTo(w, nearBase)
    scratch.near.close()
    drawPath(scratch.near, colors.mountainNear.copy(alpha = alpha))

    // ---- the ridge the Citadel stands on -------------------------------------------
    val ridgeY = horizonY + h * 0.052f
    scratch.ridge.reset()
    scratch.ridge.moveTo(0f, h)
    scratch.ridge.lineTo(0f, ridgeY + h * 0.030f)
    scratch.ridge.quadraticTo(w * 0.30f, ridgeY - h * 0.014f, w * 0.58f, ridgeY)
    scratch.ridge.quadraticTo(w * 0.82f, ridgeY + h * 0.012f, w, ridgeY - h * 0.006f)
    scratch.ridge.lineTo(w, h)
    scratch.ridge.close()
    drawPath(scratch.ridge, colors.ridge.copy(alpha = alpha))

    // ---- the village, then the keep above it ---------------------------------------
    drawVillage(world, colors, w, ridgeY, u, alpha, time, ambients, scratch)
    drawCitadel(world, colors, w, ridgeY, u, alpha, time, scratch)

    // ---- foreground the camp sits on -----------------------------------------------
    val camp = ridgeY + h * 0.062f
    scratch.foreground.reset()
    scratch.foreground.moveTo(0f, h)
    scratch.foreground.lineTo(0f, camp + h * 0.016f)
    scratch.foreground.quadraticTo(w * 0.26f, camp - h * 0.020f, w * 0.62f, camp + h * 0.028f)
    scratch.foreground.quadraticTo(w * 0.84f, camp + h * 0.050f, w, camp + h * 0.030f)
    scratch.foreground.lineTo(w, h)
    scratch.foreground.close()
    drawPath(scratch.foreground, colors.foreground.copy(alpha = alpha))

    drawCamp(world, colors, w, camp, ridgeY, u, alpha, time, ambients, scratch)

    if (Ambient.BIRDS in ambients && world.hasBirds) {
        drawBirds(w, horizonY, time, alpha)
    }
}

// ---- the keep -----------------------------------------------------------------------

private fun DrawScope.drawCitadel(
    world: WorldState,
    colors: SceneColors,
    w: Float,
    ridgeY: Float,
    u: Float,
    alpha: Float,
    time: Float,
    scratch: LandScratch
) {
    val stone = colors.stone.copy(alpha = alpha)
    val cx = w * 0.68f
    val base = ridgeY + u * 0.15f

    // Curtain wall
    val wallLeft = cx - u * 5.6f
    val wallRight = cx + u * 4.6f
    val wallTop = base - u * 2.1f
    drawRect(stone, Offset(wallLeft, wallTop), Size(wallRight - wallLeft, base - wallTop))

    // Battlements along the top of the wall
    val merlon = u * 0.42f
    var mx = wallLeft
    while (mx < wallRight - merlon) {
        drawRect(stone, Offset(mx, wallTop - u * 0.38f), Size(merlon, u * 0.38f))
        mx += merlon * 2.1f
    }

    // Watchtower at the far end of the wall
    val towerX = wallLeft - u * 0.2f
    val towerW = u * 1.35f
    val towerH = u * 3.6f
    drawRect(stone, Offset(towerX, base - towerH), Size(towerW, towerH))
    scratch.roof.reset()
    scratch.roof.moveTo(towerX - u * 0.30f, base - towerH)
    scratch.roof.lineTo(towerX + towerW / 2f, base - towerH - u * 1.25f)
    scratch.roof.lineTo(towerX + towerW + u * 0.30f, base - towerH)
    scratch.roof.close()
    drawPath(scratch.roof, stone)

    // The keep
    val keepX = cx + u * 0.6f
    val keepW = u * 2.5f
    val keepH = u * 5.8f
    val keepTop = base - keepH
    drawRect(stone, Offset(keepX, keepTop), Size(keepW, keepH))
    scratch.roof.reset()
    scratch.roof.moveTo(keepX - u * 0.42f, keepTop)
    scratch.roof.lineTo(keepX + keepW / 2f, keepTop - u * 1.7f)
    scratch.roof.lineTo(keepX + keepW + u * 0.42f, keepTop)
    scratch.roof.close()
    drawPath(scratch.roof, stone)

    // The banner only flies once the Citadel has been kept for a while.
    if (world.villageWarmth > 0.05f) {
        val poleX = keepX + keepW / 2f
        val poleTop = keepTop - u * 3.2f
        drawLine(
            color = stone,
            start = Offset(poleX, keepTop - u * 1.7f),
            end = Offset(poleX, poleTop),
            strokeWidth = u * 0.10f
        )
        val wave = sin(time * 1.6f) * u * 0.16f
        scratch.banner.reset()
        scratch.banner.moveTo(poleX, poleTop)
        scratch.banner.lineTo(poleX + u * 1.25f, poleTop + u * 0.30f + wave)
        scratch.banner.lineTo(poleX + u * 1.05f, poleTop + u * 0.62f)
        scratch.banner.lineTo(poleX, poleTop + u * 0.92f)
        scratch.banner.close()
        drawPath(
            scratch.banner,
            LanternGold.copy(alpha = (0.30f + 0.45f * world.villageWarmth) * alpha)
        )
    }

    // Gatehouse
    val gateHouseX = cx - u * 2.4f
    val gateHouseW = u * 2.2f
    val gateHouseH = u * 3.0f
    drawRect(stone, Offset(gateHouseX, base - gateHouseH), Size(gateHouseW, gateHouseH))
    var gm = gateHouseX
    while (gm < gateHouseX + gateHouseW - merlon) {
        drawRect(stone, Offset(gm, base - gateHouseH - u * 0.34f), Size(merlon, u * 0.34f))
        gm += merlon * 2.1f
    }

    // The gate itself
    val gateW = u * 0.95f
    val gateH = u * 1.5f
    val gateX = gateHouseX + gateHouseW / 2f - gateW / 2f
    scratch.gate.reset()
    scratch.gate.moveTo(gateX, base)
    scratch.gate.lineTo(gateX, base - gateH + gateW / 2f)
    scratch.gate.quadraticTo(
        gateX + gateW / 2f, base - gateH - gateW * 0.16f,
        gateX + gateW, base - gateH + gateW / 2f
    )
    scratch.gate.lineTo(gateX + gateW, base)
    scratch.gate.close()
    // The gate glows faintly from inside once anyone is home.
    val gateGlow = (0.10f + 0.55f * world.lightFraction) * alpha
    drawPath(scratch.gate, Color(0xFF07060A).copy(alpha = alpha))
    drawPath(scratch.gate, WindowWarm.copy(alpha = gateGlow * 0.35f))

    // ---- the lights ----
    // Windows warm in step with the day's progress; lanterns light one per promise kept.
    val lit = world.lightFraction
    drawWindow(keepX + keepW * 0.28f, keepTop + u * 1.0f, u, lit > 0.15f, alpha, time, 0.0f)
    drawWindow(keepX + keepW * 0.28f, keepTop + u * 2.6f, u, lit > 0.55f, alpha, time, 0.4f)
    drawWindow(towerX + towerW * 0.30f, base - towerH + u * 0.9f, u, lit > 0.35f, alpha, time, 0.8f)

    // Wall lanterns: the clearest, most literal statement the world makes.
    val lanternSlots = WorldState.WALL_LANTERNS
    val spacing = (wallRight - wallLeft - u * 0.8f) / (lanternSlots - 1)
    for (i in 0 until lanternSlots) {
        val lx = wallLeft + u * 0.4f + spacing * i
        val ly = wallTop - u * 0.10f
        val on = i < world.lanternsLit
        drawLantern(lx, ly, u, on, alpha, time, i * 0.37f)
    }
}

private fun DrawScope.drawWindow(
    x: Float,
    y: Float,
    u: Float,
    lit: Boolean,
    alpha: Float,
    time: Float,
    phase: Float
) {
    val w = u * 0.42f
    val h = u * 0.62f
    if (lit) {
        val flicker = 0.88f + 0.12f * breathe(time, 5.5f, phase)
        drawCircle(
            brush = Brush.radialGradient(
                0f to WindowWarm.copy(alpha = 0.26f * alpha * flicker),
                1f to Color.Transparent,
                center = Offset(x + w / 2f, y + h / 2f),
                radius = u * 2.0f
            ),
            radius = u * 2.0f,
            center = Offset(x + w / 2f, y + h / 2f)
        )
        drawRect(WindowWarm.copy(alpha = alpha * flicker), Offset(x, y), Size(w, h))
    } else {
        drawRect(WindowCold.copy(alpha = alpha * 0.85f), Offset(x, y), Size(w, h))
    }
}

private fun DrawScope.drawLantern(
    x: Float,
    y: Float,
    u: Float,
    lit: Boolean,
    alpha: Float,
    time: Float,
    phase: Float
) {
    val hookTop = y - u * 0.55f
    drawLine(
        color = Color(0xFF0A0910).copy(alpha = alpha),
        start = Offset(x, hookTop),
        end = Offset(x, y),
        strokeWidth = u * 0.07f
    )
    if (!lit) {
        drawCircle(Color(0xFF14121C).copy(alpha = alpha), u * 0.16f, Offset(x, y))
        return
    }
    val pulse = 0.82f + 0.18f * breathe(time, 4.2f, phase)
    drawCircle(
        brush = Brush.radialGradient(
            0f to LanternGold.copy(alpha = 0.42f * alpha * pulse),
            0.4f to LanternGold.copy(alpha = 0.14f * alpha * pulse),
            1f to Color.Transparent,
            center = Offset(x, y),
            radius = u * 1.9f
        ),
        radius = u * 1.9f,
        center = Offset(x, y)
    )
    drawCircle(LanternGold.copy(alpha = alpha * pulse), u * 0.19f, Offset(x, y))
}

// ---- the village --------------------------------------------------------------------

private fun DrawScope.drawVillage(
    world: WorldState,
    colors: SceneColors,
    w: Float,
    ridgeY: Float,
    u: Float,
    alpha: Float,
    time: Float,
    ambients: Set<Ambient>,
    scratch: LandScratch
) {
    val stone = colors.stone.copy(alpha = alpha)
    val homes = listOf(
        w * 0.30f to u * 1.35f,
        w * 0.39f to u * 1.05f,
        w * 0.47f to u * 1.20f
    )

    homes.forEachIndexed { index, (x, height) ->
        val bw = u * 1.5f
        val base = ridgeY + u * 0.55f + index * u * 0.10f
        drawRect(stone, Offset(x, base - height), Size(bw, height))
        drawRoof(scratch.roof, x, base - height, bw, u * 0.62f, stone)

        // A window per home, warming with long-term consistency rather than today's list.
        val homeLit = world.villageWarmth > (index * 0.28f) || world.lightFraction > 0.6f
        drawWindow(x + bw * 0.32f, base - height + u * 0.42f, u * 0.85f, homeLit, alpha, time, index * 0.6f)

        // Chimney
        val chimneyX = x + bw * 0.72f
        val chimneyTop = base - height - u * 0.75f
        drawRect(stone, Offset(chimneyX, chimneyTop), Size(u * 0.26f, u * 0.75f))

        if (Ambient.SMOKE in ambients && world.lightFraction > 0f) {
            drawSmoke(chimneyX + u * 0.13f, chimneyTop, u, time, index * 1.7f, alpha * world.lightFraction)
        }
    }
}

private fun DrawScope.drawRoof(
    path: Path,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color
) {
    path.reset()
    path.moveTo(x - width * 0.14f, y)
    path.lineTo(x + width / 2f, y - height)
    path.lineTo(x + width * 1.14f, y)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSmoke(
    x: Float,
    y: Float,
    u: Float,
    time: Float,
    phase: Float,
    alpha: Float
) {
    // Three puffs on the same path, staggered — reads as a continuous curl.
    for (i in 0 until 3) {
        val t = ((time * 0.16f) + phase + i * 0.333f) % 1f
        val rise = u * 3.4f * t
        val sway = sin((t * 3.1f + phase) * PI.toFloat()) * u * 0.5f
        val radius = u * 0.16f + u * 0.42f * t
        val a = (1f - t) * 0.16f * alpha
        if (a <= 0.005f) continue
        // Soft falloff, like every other kind of air in the Citadel.
        softPuff(Offset(x + sway, y - rise), radius * 1.7f, Color(0xFFC9CCD6), a * 1.5f)
    }
}

// ---- the camp in the foreground ------------------------------------------------------

private fun DrawScope.drawCamp(
    world: WorldState,
    colors: SceneColors,
    w: Float,
    campY: Float,
    ridgeY: Float,
    u: Float,
    alpha: Float,
    time: Float,
    ambients: Set<Ambient>,
    scratch: LandScratch
) {
    val fireX = w * 0.22f
    val fireY = campY - u * 0.10f
    val heat = world.lightFraction

    // Firewood
    val wood = Color(0xFF241611).copy(alpha = alpha)
    drawLine(wood, Offset(fireX - u * 0.7f, fireY + u * 0.2f), Offset(fireX + u * 0.7f, fireY - u * 0.15f), u * 0.16f)
    drawLine(wood, Offset(fireX - u * 0.6f, fireY - u * 0.15f), Offset(fireX + u * 0.6f, fireY + u * 0.2f), u * 0.16f)

    // The fire itself. Low embers when nothing has been kept; a real flame when it has.
    val flicker = 0.90f + 0.10f * sin(time * 6.1f) + 0.04f * sin(time * 11.3f)
    val glowRadius = u * (1.6f + heat * 3.4f) * flicker
    drawCircle(
        brush = Brush.radialGradient(
            0f to FireCore.copy(alpha = (0.30f + 0.34f * heat) * alpha),
            0.35f to FireDeep.copy(alpha = (0.13f + 0.17f * heat) * alpha),
            1f to Color.Transparent,
            center = Offset(fireX, fireY),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(fireX, fireY)
    )
    drawCircle(
        color = if (heat > 0f) FireCore.copy(alpha = 0.92f * alpha) else FireDeep.copy(alpha = 0.55f * alpha),
        radius = u * (0.22f + heat * 0.24f) * flicker,
        center = Offset(fireX, fireY - u * 0.08f)
    )

    // Embers rising. Only when the fire has something to burn, and only as a dominant ambient.
    if (Ambient.EMBERS in ambients && heat > 0.05f) {
        for (i in 0 until 5) {
            val t = ((time * 0.22f) + i * 0.2f) % 1f
            val rise = u * 4.5f * t
            val sway = sin((t * 2.4f + i) * PI.toFloat()) * u * 0.55f
            val a = (1f - t) * heat * 0.9f * alpha
            if (a <= 0.01f) continue
            drawCircle(
                FireCore.copy(alpha = a),
                u * 0.075f * (1f - t * 0.4f),
                Offset(fireX + sway, fireY - rise)
            )
        }
    }

    drawGuardian(world, colors, w, campY, ridgeY, fireX, fireY, u, alpha, time, scratch)

    // The hound arrives after a month of protected days and then simply lives here.
    if (world.hasHound) {
        val hx = fireX + u * 2.3f
        val hy = campY + u * 0.10f
        val body = colors.stone.copy(alpha = alpha)
        drawRoundRect(
            color = body,
            topLeft = Offset(hx, hy - u * 0.34f),
            size = Size(u * 1.05f, u * 0.34f),
            cornerRadius = CornerRadius(u * 0.17f, u * 0.17f)
        )
        drawCircle(body, u * 0.20f, Offset(hx + u * 1.02f, hy - u * 0.40f))
        // A slow breath, because a sleeping dog is the calmest thing a screen can show.
        val breath = breathe(time, 4.0f) * u * 0.03f
        drawCircle(body, u * 0.07f, Offset(hx + u * 1.20f, hy - u * 0.34f + breath))
    }

    // Sprouts and flowers: the slow reward for weeks rather than for today.
    if (world.hasSprouts) {
        val green = Color(0xFF3F7D58).copy(alpha = alpha * (0.4f + 0.6f * world.villageWarmth))
        val spots = listOf(0.42f, 0.50f, 0.57f, 0.64f, 0.72f, 0.79f)
        spots.forEachIndexed { i, fx ->
            if (i > world.villageWarmth * spots.size) return@forEachIndexed
            val sx = w * fx
            val sy = campY + u * (0.55f + (i % 3) * 0.22f)
            val sway = sin(time * 0.5f + i) * u * 0.04f
            drawLine(green, Offset(sx, sy), Offset(sx + sway, sy - u * 0.34f), u * 0.06f)
            if (world.hasFlowers && i % 2 == 0) {
                drawCircle(
                    Color(0xFFE9E4D6).copy(alpha = alpha * 0.75f),
                    u * 0.08f,
                    Offset(sx + sway, sy - u * 0.38f)
                )
            }
        }
    }
}

/**
 * The Guardian is part of the landscape, not a widget on top of it.
 * Where they stand says more than anything they could be made to say.
 */
private fun DrawScope.drawGuardian(
    world: WorldState,
    colors: SceneColors,
    w: Float,
    campY: Float,
    ridgeY: Float,
    fireX: Float,
    fireY: Float,
    u: Float,
    alpha: Float,
    time: Float,
    scratch: LandScratch
) {
    // Against the sky, the Guardian is a true silhouette. Down in the camp, the ground is
    // already near-black, so the figure is lifted towards the haze colour just enough to read.
    val skyInk = Color(0xFF08070D).copy(alpha = alpha * 0.94f)
    val ink = androidx.compose.ui.graphics.lerp(colors.foreground, colors.mistTint, 0.42f)
        .copy(alpha = alpha * 0.95f)
    val sway = sin(time * 0.35f) * u * 0.015f

    // Where the wall runs, for the two watches that are kept from up there.
    val wallBase = ridgeY + u * 0.15f
    val wallTop = wallBase - u * 2.1f

    when (world.sky.phase) {
        // Morning: sitting on the bench with something hot.
        DayPhase.DAWN -> {
            val bx = fireX + u * 1.6f
            val by = campY + u * 0.05f
            drawLine(ink, Offset(bx - u * 0.75f, by), Offset(bx + u * 0.75f, by), u * 0.11f)
            drawLine(ink, Offset(bx - u * 0.55f, by), Offset(bx - u * 0.55f, by + u * 0.36f), u * 0.08f)
            drawLine(ink, Offset(bx + u * 0.55f, by), Offset(bx + u * 0.55f, by + u * 0.36f), u * 0.08f)
            // Seated figure
            drawRoundRect(
                ink,
                Offset(bx - u * 0.28f, by - u * 0.86f + sway),
                Size(u * 0.56f, u * 0.86f),
                CornerRadius(u * 0.22f, u * 0.22f)
            )
            drawCircle(ink, u * 0.20f, Offset(bx, by - u * 1.05f + sway))
            // The mug.
            drawCircle(
                LanternGold.copy(alpha = alpha * 0.7f),
                u * 0.07f,
                Offset(bx + u * 0.34f, by - u * 0.52f + sway)
            )
        }

        // Midday: down on the road below the gate, watching it. On the ground the figure
        // uses the lifted ink, which is the only thing that separates them from the ridge.
        DayPhase.DAY -> {
            val gx = w * 0.56f
            // On the ridge itself rather than down in the veil, where the figure was
            // getting swallowed by the dissolve into the mission list.
            val gy = ridgeY + u * 1.1f
            drawRoundRect(
                ink,
                Offset(gx - u * 0.17f, gy - u * 1.20f + sway),
                Size(u * 0.34f, u * 1.20f),
                CornerRadius(u * 0.16f, u * 0.16f)
            )
            drawCircle(ink, u * 0.20f, Offset(gx, gy - u * 1.38f + sway))
            // A staff, planted. The whole pose is "nothing is happening, which is correct".
            drawLine(
                ink,
                Offset(gx + u * 0.30f, gy - u * 1.55f),
                Offset(gx + u * 0.30f, gy),
                u * 0.06f
            )
        }

        // Evening: crouched, feeding the fire.
        DayPhase.DUSK -> {
            val cxp = fireX - u * 1.25f
            val cyp = campY + u * 0.02f
            scratch.figure.reset()
            scratch.figure.moveTo(cxp - u * 0.30f, cyp)
            scratch.figure.lineTo(cxp + u * 0.34f, cyp - u * 0.74f + sway)
            scratch.figure.lineTo(cxp + u * 0.52f, cyp - u * 0.40f)
            scratch.figure.lineTo(cxp + u * 0.30f, cyp)
            scratch.figure.close()
            drawPath(scratch.figure, ink)
            drawCircle(ink, u * 0.19f, Offset(cxp + u * 0.34f, cyp - u * 0.92f + sway))
            drawLine(
                ink,
                Offset(cxp + u * 0.48f, cyp - u * 0.62f),
                Offset(fireX - u * 0.35f, fireY + u * 0.05f),
                u * 0.06f
            )
        }

        // Night: up on the parapet with a lantern, watching the sky.
        DayPhase.NIGHT -> {
            val px = w * 0.68f - u * 3.9f
            val py = wallTop - u * 0.30f
            val pulse = 0.80f + 0.20f * breathe(time, 5.0f)

            // The lantern's spill is drawn *behind* the figure. A black silhouette on a
            // black mountain is not a silhouette, it is an absence — the light has to be
            // behind them for the shape to exist at all.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to LanternGold.copy(alpha = 0.30f * alpha * pulse),
                    0.45f to LanternGold.copy(alpha = 0.13f * alpha * pulse),
                    1f to Color.Transparent,
                    center = Offset(px + u * 0.10f, py - u * 0.75f),
                    radius = u * 3.1f
                ),
                radius = u * 3.1f,
                center = Offset(px + u * 0.10f, py - u * 0.75f)
            )

            drawRoundRect(
                skyInk,
                Offset(px - u * 0.16f, py - u * 1.15f + sway),
                Size(u * 0.32f, u * 1.15f),
                CornerRadius(u * 0.15f, u * 0.15f)
            )
            drawCircle(skyInk, u * 0.19f, Offset(px, py - u * 1.32f + sway))
            val lanternY = py - u * 0.62f
            drawLine(
                skyInk,
                Offset(px + u * 0.30f, py - u * 1.10f),
                Offset(px + u * 0.30f, lanternY),
                u * 0.05f
            )
            drawCircle(LanternGold.copy(alpha = alpha * pulse), u * 0.13f, Offset(px + u * 0.30f, lanternY))
        }
    }
}

private fun DrawScope.drawBirds(w: Float, horizonY: Float, time: Float, alpha: Float) {
    val ink = Color(0xFF0B0A12).copy(alpha = alpha * 0.55f)
    for (i in 0 until 3) {
        val t = ((time * 0.035f) + i * 0.16f) % 1.25f
        if (t > 1f) continue
        val x = w * (-0.1f + 1.2f * t)
        val y = horizonY * (0.30f + 0.10f * sin((t * 2f + i) * PI.toFloat()))
        val span = w * 0.011f
        // Wingbeat, slow enough to read as a bird rather than a strobe.
        val flap = sin(time * 7.5f + i * 1.9f) * span * 0.5f
        drawLine(ink, Offset(x, y), Offset(x - span, y - flap), 1.6f, cap = StrokeCap.Round)
        drawLine(ink, Offset(x, y), Offset(x + span, y - flap), 1.6f, cap = StrokeCap.Round)
    }
}
