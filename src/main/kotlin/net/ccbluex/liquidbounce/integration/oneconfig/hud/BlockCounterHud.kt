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
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import java.awt.Color
import kotlin.math.max

/**
 * Native bridging block counter.
 *
 * The scaffold module reports the block it is about to place and how many of them are left, and the element prints
 * that number next to the block icon. The colour runs from red at zero to green at a full stack, so a single glance
 * tells whether bridging is about to run out of blocks.
 */
object BlockCounterHud :
    LbHudSurface("block_counter", "Block Counter", Hud.Category.INFO, LbHudPalette.surface(0.68f)) {

    @Dropdown(title = "Icon Position", options = ["None", "Left", "Right", "Top", "Bottom"])
    @JvmField
    var iconPosition: String = "Left"

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
     * Colour of the number, running from red at an empty inventory to green at a full stack.
     *
     * This mirrors the native `mapToColor` helper, which walks the hue circle up to 120 degrees, the point where the
     * hue turns green.
     */
    private fun countColor(count: Int): Int {
        val hue = (count.coerceIn(0, FULL_STACK) / FULL_STACK.toFloat()) * (MAX_HUE / 360f)
        return Color.HSBtoRGB(hue, 1f, 0.85f) or ALPHA_OPAQUE
    }

    private fun iconStack(): ItemStack? =
        currentBlock?.asItem()?.defaultInstance?.takeIf { !it.isEmpty }

    /** Extent of the plate for a given label, so the measuring pass and the paint pass agree. */
    private fun plateSize(text: String, withIcon: Boolean): HudSize {
        val textWidth = textWidth(text, VALUE_SIZE, LbHudFonts.Weight.MEDIUM)
        val textLine = lineHeight(VALUE_SIZE, LbHudFonts.Weight.MEDIUM)

        val width = when {
            !withIcon -> PADDING_X * 2f + textWidth
            verticalIcon -> PADDING_X * 2f + max(ICON_SIZE, textWidth)
            else -> PADDING_X * 2f + ICON_SIZE + GAP + textWidth
        }
        val height = when {
            !withIcon -> PADDING_Y * 2f + textLine
            verticalIcon -> PADDING_Y * 2f + ICON_SIZE + GAP + textLine
            else -> PADDING_Y * 2f + max(ICON_SIZE, textLine)
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

        canvas.fill(0f, 0f, width, height, surfaceArgb(), SURFACE_RADIUS)

        val colour = countColor(count)

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
                iconX = PADDING_X
                iconY = (height - ICON_SIZE) * 0.5f
                textX = PADDING_X + ICON_SIZE + GAP
                textY = (height - lineHeight) * 0.5f
            }

            "Right" -> {
                textX = PADDING_X
                textY = (height - lineHeight) * 0.5f
                iconX = PADDING_X + textWidth + GAP
                iconY = (height - ICON_SIZE) * 0.5f
            }

            "Top" -> {
                iconX = (width - ICON_SIZE) * 0.5f
                iconY = PADDING_Y
                textX = (width - textWidth) * 0.5f
                textY = PADDING_Y + ICON_SIZE + GAP
            }

            else -> {
                textX = (width - textWidth) * 0.5f
                textY = PADDING_Y
                iconX = (width - ICON_SIZE) * 0.5f
                iconY = PADDING_Y + lineHeight + GAP
            }
        }

        canvas.item(stack, iconX, iconY, ICON_SIZE, ICON_SIZE)
        drawText(canvas, countText, textX, textY, VALUE_SIZE, colour, LbHudFonts.Weight.MEDIUM)
    }

    private const val VALUE_SIZE: Float = 15f
    private const val ICON_SIZE: Float = 24f
    private const val PADDING_X: Float = 8f
    private const val PADDING_Y: Float = 5f
    private const val GAP: Float = 5f
    private const val SURFACE_RADIUS: Float = 5f

    private const val FULL_STACK: Int = 64
    private const val MAX_HUE: Float = 120f
    private const val ALPHA_OPAQUE: Int = 0xFF shl 24

    private const val SAMPLE_COUNT: Int = 64
    private const val SAMPLE_COUNT_WIDTH: Float = 64f
    private const val SAMPLE_COUNT_HEIGHT: Float = 34f

}
