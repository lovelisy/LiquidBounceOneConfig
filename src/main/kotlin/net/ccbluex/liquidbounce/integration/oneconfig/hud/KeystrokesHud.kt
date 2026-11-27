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
import net.minecraft.client.KeyMapping
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * Native keystroke display.
 *
 * The four movement keys sit in a cross with the jump key as a wide bar below them, which is the layout of the native
 * client. A pressed key fills with the accent colour, animated so the plate follows the key instead of snapping.
 */
object KeystrokesHud : LbHudSurface("keystrokes", "Keystrokes", Hud.Category.INFO, LbHudPalette.surface(0.68f)) {

    /** Fill progress of every key, so releasing a key fades the plate back instead of clearing it at once. */
    private val animProgress: FloatArray = FloatArray(KEY_COUNT)

    private fun keys(): Array<KeyMapping> {
        val options = mc.options
        return arrayOf(options.keyUp, options.keyLeft, options.keyDown, options.keyRight, options.keyJump)
    }

    /** Plates of the five keys, as `x`, `y`, `width` and `height` in the content area. */
    private fun plates(): Array<FloatArray> {
        val step = CELL_SIZE + GAP
        return arrayOf(
            floatArrayOf(step, 0f, CELL_SIZE, CELL_SIZE),
            floatArrayOf(0f, step, CELL_SIZE, CELL_SIZE),
            floatArrayOf(step, step, CELL_SIZE, CELL_SIZE),
            floatArrayOf(step * 2f, step, CELL_SIZE, CELL_SIZE),
            floatArrayOf(0f, step * 2f, TOTAL_WIDTH, CELL_SIZE),
        )
    }

    override fun measure(): HudSize = HudSize(TOTAL_WIDTH, TOTAL_HEIGHT)

    override fun paint(canvas: LbHudCanvas) {
        val mapping = keys()
        val rects = plates()

        mapping.forEachIndexed { index, key ->
            val pressed = !HudManager.isEditing && key.isDown
            animProgress[index] = LbHudPalette.expLerp(animProgress[index], if (pressed) 1f else 0f, 16f, canvas.deltaSeconds)

            val rect = rects[index]
            val x = rect[0]
            val y = rect[1]
            val width = rect[2]
            val height = rect[3]

            canvas.fill(x, y, width, height, surfaceArgb(), SURFACE_RADIUS)

            val progress = animProgress[index]
            if (progress > 0.01f) {
                val inset = (height * 0.5f) * progress
                canvas.fill(
                    x + inset,
                    y + inset,
                    width - inset * 2f,
                    height - inset * 2f,
                    LbHudPalette.accent(progress),
                    (SURFACE_RADIUS - inset * 0.5f).coerceAtLeast(0f),
                )
            }

            val label = key.translatedKeyMessage.string.uppercase()
            val labelWidth = textWidth(label, LABEL_SIZE, LbHudFonts.Weight.MEDIUM)
            val labelLine = lineHeight(LABEL_SIZE, LbHudFonts.Weight.MEDIUM)
            drawText(
                canvas,
                label,
                x + (width - labelWidth) * 0.5f,
                y + (height - labelLine) * 0.5f,
                LABEL_SIZE,
                textColor,
                LbHudFonts.Weight.MEDIUM,
            )
        }
    }

    private const val KEY_COUNT: Int = 5
    private const val CELL_SIZE: Float = 50f
    private const val GAP: Float = 5f
    private const val TOTAL_WIDTH: Float = CELL_SIZE * 3f + GAP * 2f
    private const val TOTAL_HEIGHT: Float = CELL_SIZE * 3f + GAP * 2f
    private const val LABEL_SIZE: Float = 14f
    private const val SURFACE_RADIUS: Float = 5f

}
