/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.oneconfig.hud

import net.ccbluex.liquidbounce.utils.client.mc
import org.polyfrost.oneconfig.api.hud.v1.Hud
import kotlin.math.max

/**
 * Native taco.
 *
 * A twelve-frame taco strolls across the screen at a constant speed and wraps around after leaving the right edge.
 * The frame advances every 70 ms, matching the native animation. The element is off by default and never paints a
 * background plate.
 */
object TacoHud : LbHudSurface("taco", "Taco", Hud.Category.INFO, 0) {

    override val defaultShown: Boolean
        get() = false

    private const val FRAME_COUNT: Int = 12
    private const val FRAME_MS: Long = 70L
    private const val TACO_WIDTH: Float = 86f
    private const val TACO_HEIGHT: Float = 60f
    private const val SPEED: Float = 200f
    private const val MIN_WIDTH: Float = 120f

    private var walkX: Float = -100f

    private fun framePath(index: Int): String = "textures/ui/hud/taco/frame_$index.png"

    override fun measure(): HudSize {
        // The style scale halves the reported extent, so double the viewport width to let the taco roam the full screen.
        val width = mc.window.guiScaledWidth.toFloat() * (1f / LbHudScale.PX)
        return HudSize(max(MIN_WIDTH, width), TACO_HEIGHT)
    }

    override fun paint(canvas: LbHudCanvas) {
        walkX += canvas.deltaSeconds * SPEED
        val viewportWidth = canvas.width
        if (walkX > viewportWidth + 100f) {
            walkX = -100f
        }

        val frame = ((System.currentTimeMillis() / FRAME_MS) % FRAME_COUNT).toInt()
        val drawX = if (canvas.inEditor && (walkX < 0f || walkX > viewportWidth - TACO_WIDTH)) {
            16f
        } else {
            walkX
        }
        canvas.stretchedIcon(framePath(frame), drawX, 0f, TACO_WIDTH, TACO_HEIGHT, textColor)
    }
}
