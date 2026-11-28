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
 * client. A held key fills with the accent colour: the native client draws an inset shadow whose spread grows to half
 * the cell, which `UiDraw.fillCssBox` turns into a border that thickens until the ring closes over the whole plate. That
 * ring is what [LbHudCanvas.fillRing] reproduces, so the key stays lit for as long as it is held instead of flashing
 * once and going dark.
 */
object KeystrokesHud : LbHudSurface("keystrokes", "Keystrokes", Hud.Category.INFO) {

    /** Fill progress of every key, so releasing a key fades the plate back instead of clearing it at once. */
    private val animProgress: FloatArray = FloatArray(KEY_COUNT)

    private fun keys(): Array<KeyMapping> {
        val options = mc.options
        return arrayOf(options.keyUp, options.keyLeft, options.keyDown, options.keyRight, options.keyJump)
    }

    /** Plates of the five keys, as `x`, `y`, `width` and `height` in the content area. */
    private fun plates(cell: Float, gap: Float, totalWidth: Float): Array<FloatArray> {
        val step = cell + gap
        return arrayOf(
            floatArrayOf(step, 0f, cell, cell),
            floatArrayOf(0f, step, cell, cell),
            floatArrayOf(step, step, cell, cell),
            floatArrayOf(step * 2f, step, cell, cell),
            floatArrayOf(0f, step * 2f, totalWidth, cell),
        )
    }

    /** One key cell of the native design; the style scale converts it to GUI units. */
    private fun cellSize(): Float = CELL_SIZE * textScale

    private fun gap(): Float = GAP * textScale

    /** Label size; [hudFont] applies the text scale on its own. */
    private fun labelFont(): Float = LABEL_SIZE

    private fun totalWidth(): Float = cellSize() * 3f + gap() * 2f

    override fun measure(): HudSize {
        val total = totalWidth()
        return HudSize(total, total)
    }

    override fun paint(canvas: LbHudCanvas) {
        val mapping = keys()
        val cell = cellSize()
        val spacing = gap()
        val total = totalWidth()
        val rects = plates(cell, spacing, total)
        val radius = SURFACE_RADIUS * textScale
        val labelFont = labelFont()

        mapping.forEachIndexed { index, key ->
            // A held key reports true for as long as it is down; the editor never reports a press so the preview stays
            // readable. The progress is clamped to one immediately on press, so a frame that only repaints once still
            // leaves the key fully lit while it is held.
            val pressed = !HudManager.isEditing && key.isDown
            animProgress[index] = if (pressed) {
                1f
            } else {
                LbHudPalette.expLerp(animProgress[index], 0f, 16f, canvas.deltaSeconds)
            }

            val rect = rects[index]
            val x = rect[0]
            val y = rect[1]
            val width = rect[2]
            val height = rect[3]

            canvas.fill(x, y, width, height, surfaceArgb(), radius)

            val progress = animProgress[index]
            if (progress > 0.01f) {
                // Native look: an inset ring that thickens towards the centre while the key is held.
                canvas.fillRing(x, y, width, height, radius, height * 0.5f * progress, accentArgb())
            }

            val text = key.translatedKeyMessage.string.uppercase()
            val labelWidth = textWidth(text, labelFont, LbHudFonts.Weight.MEDIUM)
            val labelLine = lineHeight(labelFont, LbHudFonts.Weight.MEDIUM)
            drawText(
                canvas,
                text,
                x + (width - labelWidth) * 0.5f,
                y + (height - labelLine) * 0.5f,
                labelFont,
                textColor,
                LbHudFonts.Weight.MEDIUM,
            )
        }
    }

    private const val KEY_COUNT: Int = 5

    /** Cell edge, gap, label size and corner radius of the native key grid. */
    private const val CELL_SIZE: Float = 50f
    private const val GAP: Float = 5f
    private const val LABEL_SIZE: Float = 14f
    private const val SURFACE_RADIUS: Float = 5f

}
