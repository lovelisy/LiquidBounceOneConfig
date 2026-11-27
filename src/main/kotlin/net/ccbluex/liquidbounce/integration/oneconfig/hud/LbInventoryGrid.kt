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
 */
internal object LbInventoryGrid {

    /** Extent a grid of [count] stacks occupies with [columns] columns and the given [gap]. */
    fun size(count: Int, columns: Int, gap: Float): HudSize {
        val cols = max(1, columns)
        val rows = ceil(max(1, count).toFloat() / cols.toFloat()).toInt().coerceAtLeast(1)

        val width = PLATE_PADDING * 2f + cols * CELL_SIZE + max(0, cols - 1) * gap
        val height = PLATE_PADDING * 2f + rows * CELL_SIZE + max(0, rows - 1) * gap
        return HudSize(width, height)
    }

    /**
     * Paints [stacks] into a grid.
     *
     * Every stack is drawn at [CELL_SIZE] and keeps its own decorations, so the item count and the durability bar show
     * up exactly like they do in the vanilla inventory. [countLabels] overrides the printed number per index, which the
     * statistics element uses to show a total across the whole inventory.
     */
    fun draw(
        canvas: LbHudCanvas,
        stacks: List<ItemStack>,
        columns: Int,
        gap: Float,
        surface: Int,
        countLabels: Map<Int, String> = emptyMap(),
    ) {
        val cols = max(1, columns)
        val extent = size(stacks.size, cols, gap)

        if ((surface ushr 24) != 0) {
            canvas.fill(0f, 0f, extent.width, extent.height, surface, PLATE_RADIUS)
        }

        stacks.forEachIndexed { index, stack ->
            if (stack.isEmpty) {
                return@forEachIndexed
            }

            val column = index % cols
            val row = index / cols
            val x = PLATE_PADDING + column * (CELL_SIZE + gap)
            val y = PLATE_PADDING + row * (CELL_SIZE + gap)

            canvas.itemWithDecorations(stack, x, y, CELL_SIZE, CELL_SIZE, countLabels[index])
        }
    }

    /** Size of one item slot, matching the native grid. */
    private const val CELL_SIZE: Float = 32f

    private const val PLATE_PADDING: Float = 4f
    private const val PLATE_RADIUS: Float = 5f

}
