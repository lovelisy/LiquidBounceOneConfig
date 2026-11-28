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

import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.DependsOn
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import kotlin.math.max

/**
 * Native bridging block counter.
 *
 * The scaffold module reports the block it is about to place and how many of them are left, and the element prints
 * that number next to the block icon.
 *
 * The number follows the text colour of the style panel, like every other label of the client. [colorByCount] keeps
 * the native warning on top of that: while it is on, the number blends from [lowCountColor] at zero blocks to
 * [highCountColor] at a full stack, so a single glance tells whether bridging is about to run out of blocks. The two
 * endpoints default to the hues the native `mapToColor` helper produces and are only shown while the switch is on.
 * Turning it off leaves the panel colour in charge.
 */
object BlockCounterHud :
    LbHudSurface("block_counter", "Block Counter", Hud.Category.INFO) {

    override val defaultShown: Boolean
        get() = true

    @Dropdown(title = "Icon Position", options = ["None", "Left", "Right", "Top", "Bottom"])
    @JvmField
    var iconPosition: String = "Left"

    @Switch(title = "Highlight Color")
    @JvmField
    var colorByCount: Boolean = false

    @Color(title = "Low Count Color", alpha = true, gradient = true)
    @DependsOn("colorByCount")
    @JvmField
    var lowCountColor: PolyColor = PolyColor(LOW_COUNT_DEFAULT)

    @Color(title = "High Count Color", alpha = true, gradient = true)
    @DependsOn("colorByCount")
    @JvmField
    var highCountColor: PolyColor = PolyColor(HIGH_COUNT_DEFAULT)

    private var currentBlock: Block? = null

    private var currentCount: Int? = null

    @Suppress("unused")
    private val blockCountHandler = handler<BlockCountChangeEvent> { event ->
        currentBlock = event.nextBlock
        currentCount = event.count
    }

    /** Count shown in the editor while the scaffold is not running, so the element can be placed and scaled. */
    private fun shownCount(canvas: LbHudCanvas): Int? =
        currentCount ?: if (canvas.inEditor) SAMPLE_COUNT else null

    private val showsIcon: Boolean
        get() = iconPosition != "None" && iconStack() != null

    private val verticalIcon: Boolean
        get() = iconPosition == "Top" || iconPosition == "Bottom"

    /**
     * Colour of the number.
     *
     * With [colorByCount] on this blends from [lowCountColor] at zero blocks to [highCountColor] at a full stack, the
     * warning colour the native element paints with its fixed red-to-green ramp. Otherwise the text colour of the
     * style panel is used, so the colour control of the HUD settings governs it like it governs every other label.
     */
    private fun countArgb(canvas: LbHudCanvas, count: Int): Int {
        if (!colorByCount) {
            return canvas.textColor
        }

        val ratio = count.coerceIn(0, FULL_STACK) / FULL_STACK.toFloat()
        return LbHudPalette.lerpArgb(lowCountColor.rawArgb, highCountColor.rawArgb, ratio)
    }

    private fun iconStack(): ItemStack? =
        currentBlock?.asItem()?.defaultInstance?.takeIf { !it.isEmpty }

    /** Extent of the plate for a given label, so the measuring pass and the paint pass agree. */
    private fun plateSize(text: String, withIcon: Boolean): HudSize {
        val textWidth = textWidth(text, VALUE_SIZE, LbHudFonts.Weight.MEDIUM)
        val textLine = lineHeight(VALUE_SIZE, LbHudFonts.Weight.MEDIUM)

        val width = when {
            !withIcon -> paddingX() * 2f + textWidth
            verticalIcon -> paddingX() * 2f + max(iconSize(), textWidth)
            else -> paddingX() * 2f + iconSize() + gap() + textWidth
        }
        val height = when {
            !withIcon -> paddingY() * 2f + textLine
            verticalIcon -> paddingY() * 2f + iconSize() + gap() + textLine
            else -> paddingY() * 2f + max(iconSize(), textLine)
        }

        return HudSize(width, height)
    }

    override fun measure(): HudSize {
        val count = currentCount ?: if (HudManager.isEditing) SAMPLE_COUNT else null
            ?: return HudSize(SAMPLE_COUNT_WIDTH, SAMPLE_COUNT_HEIGHT)

        return plateSize(count.toString(), showsIcon)
    }

    override fun paint(canvas: LbHudCanvas) {
        val count = shownCount(canvas) ?: return
        val countText = count.toString()
        val stack = iconStack()
        val withIcon = stack != null && iconPosition != "None"
        val plate = plateSize(countText, withIcon)

        val textWidth = textWidth(countText, VALUE_SIZE, LbHudFonts.Weight.MEDIUM)
        val lineHeight = lineHeight(VALUE_SIZE, LbHudFonts.Weight.MEDIUM)
        val width = plate.width
        val height = plate.height
        val icon = iconSize()

        canvas.fill(0f, 0f, width, height, canvas.surfaceArgb(), canvas.surfaceRadius())

        val colour = countArgb(canvas, count)

        if (!withIcon || stack == null) {
            drawText(
                canvas,
                countText,
                (width - textWidth) * 0.5f,
                (height - lineHeight) * 0.5f,
                VALUE_SIZE,
                colour,
                LbHudFonts.Weight.MEDIUM,
            )
            return
        }

        val iconX: Float
        val iconY: Float
        val textX: Float
        val textY: Float
        when (iconPosition) {
            "Left" -> {
                iconX = paddingX()
                iconY = (height - icon) * 0.5f
                textX = paddingX() + icon + gap()
                textY = (height - lineHeight) * 0.5f
            }

            "Right" -> {
                textX = paddingX()
                textY = (height - lineHeight) * 0.5f
                iconX = paddingX() + textWidth + gap()
                iconY = (height - icon) * 0.5f
            }

            "Top" -> {
                iconX = (width - icon) * 0.5f
                iconY = paddingY()
                textX = (width - textWidth) * 0.5f
                textY = paddingY() + icon + gap()
            }

            else -> {
                textX = (width - textWidth) * 0.5f
                textY = paddingY()
                iconX = (width - icon) * 0.5f
                iconY = paddingY() + lineHeight + gap()
            }
        }

        canvas.item(stack, SLOT, iconX, iconY, icon, icon)
        drawText(canvas, countText, textX, textY, VALUE_SIZE, colour, LbHudFonts.Weight.MEDIUM)
    }

    private fun iconSize(): Float = ICON_SIZE * textScale

    private fun paddingX(): Float = PADDING_X * textScale

    private fun paddingY(): Float = PADDING_Y * textScale

    private fun gap(): Float = GAP * textScale

    private const val SLOT: String = "block_counter"

    private const val VALUE_SIZE: Float = 15f
    private const val ICON_SIZE: Float = 24f
    private const val PADDING_X: Float = 8f
    private const val PADDING_Y: Float = 5f
    private const val GAP: Float = 5f

    private const val FULL_STACK: Int = 64

    /** Endpoints of the native `mapToColor` ramp: `HSBtoRGB(0, 1, 0.85)` and `HSBtoRGB(120°, 1, 0.85)`. */
    private const val LOW_COUNT_DEFAULT: Int = 0xFFD90000.toInt()
    private const val HIGH_COUNT_DEFAULT: Int = 0xFF00D900.toInt()

    private const val SAMPLE_COUNT: Int = 64
    private const val SAMPLE_COUNT_WIDTH: Float = 64f
    private const val SAMPLE_COUNT_HEIGHT: Float = 34f

}
