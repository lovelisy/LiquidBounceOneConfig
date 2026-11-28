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

import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws the item grids the inventory elements share.
 *
 * One icon is [CELL_SIZE] wide, the plate is padded by [PLATE_PADDING] on every side and the cells are separated by the
 * gap the caller asks for, which is what makes the 9x3 inventory wider than the 2x2 crafting grid.
 *
 * The plate is only painted when [surface] is not fully transparent, so an element without a background stays free of
 * a plate instead of drawing one in the transparent colour.
 *
 * Every slot is reported to [LbItemIcons] with a key of `[owner]:[index]`, so the icon handles are reused across frames
 * instead of being recreated and closed on every paint pass.
 */
internal object LbInventoryGrid {

    /** Extent a grid of [count] stacks occupies with [columns] columns and the given [gap]. */
    fun size(count: Int, columns: Int, gap: Float, textScale: Float = 1f): HudSize {
        val cols = max(1, columns)
        val rows = ceil(max(1, count).toFloat() / cols.toFloat()).toInt().coerceAtLeast(1)

        val cell = cellSize(textScale)
        val padding = padding(textScale)
        val spacing = spacing(gap, textScale)

        val width = padding * 2f + cols * cell + max(0, cols - 1) * spacing
        val height = padding * 2f + rows * cell + max(0, rows - 1) * spacing
        return HudSize(width, height)
    }

    /**
     * Paints [stacks] into a grid.
     *
     * Every stack keeps its own decorations, so the item count and the durability bar show up exactly like they do in
     * the vanilla inventory. [countLabels] overrides the printed number per index, which the statistics element uses to
     * show a total across the whole inventory.
     */
    fun draw(
        canvas: LbHudCanvas,
        owner: String,
        stacks: List<ItemStack>,
        columns: Int,
        gap: Float,
        surface: Int,
        countLabels: Map<Int, String> = emptyMap(),
    ) {
        val cols = max(1, columns)
        val scale = canvas.textScale
        val extent = size(stacks.size, cols, gap, scale)
        val cell = cellSize(scale)
        val padding = padding(scale)
        val spacing = spacing(gap, scale)

        if ((surface ushr 24) != 0) {
            canvas.fill(0f, 0f, extent.width, extent.height, surface, radius(scale))
        }

        stacks.forEachIndexed { index, stack ->
            if (stack.isEmpty) {
                return@forEachIndexed
            }

            val column = index % cols
            val row = index / cols
            val x = padding + column * (cell + spacing)
            val y = padding + row * (cell + spacing)

            canvas.itemWithDecorations(stack, "$owner:$index", x, y, cell, cell, countLabels[index])
        }
    }

    /** Removes the icon handles of an element that no longer draws its grid. */
    fun release(owner: String) = LbItemIcons.release(owner)

    private fun cellSize(textScale: Float): Float = CELL_SIZE * textScale

    private fun padding(textScale: Float): Float = PLATE_PADDING * textScale

    private fun radius(textScale: Float): Float = PLATE_RADIUS * textScale

    private fun spacing(gap: Float, textScale: Float): Float = gap * textScale

    /** Size of one item slot, in the design units of the native grid. */
    private const val CELL_SIZE: Float = 32f

    private const val PLATE_PADDING: Float = 4f
    private const val PLATE_RADIUS: Float = 5f

}

