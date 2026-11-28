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
import net.minecraft.world.item.ItemStack
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Native main inventory.
 *
 * The element shows the 27 storage slots of the player inventory as a 9x3 grid, which is the part a player fills while
 * mining or fighting. The hotbar is left out because the vanilla bar already shows it.
 */
object InventoryHud : LbHudSurface("inventory", "Inventory", Hud.Category.INFO) {

    /** Storage slots of the player inventory, hotbar excluded. */
    private fun stacks(): List<ItemStack> {
        val items = mc.player?.inventory?.nonEquipmentItems
        if (items != null && items.size >= MAIN_SLOT_END) {
            return items.subList(MAIN_SLOT_START, MAIN_SLOT_END)
        }
        return EMPTY_STACKS
    }

    override fun measure(): HudSize = LbInventoryGrid.size(GRID_SLOTS, COLUMNS, GAP, textScale)

    override fun paint(canvas: LbHudCanvas) {
        LbInventoryGrid.draw(canvas, OWNER, stacks(), COLUMNS, GAP, canvas.surfaceArgb())
    }

    private const val OWNER: String = "inventory"

    private const val COLUMNS: Int = 9
    private const val GAP: Float = 8f

    private const val MAIN_SLOT_START: Int = 9
    private const val MAIN_SLOT_END: Int = 36
    private const val GRID_SLOTS: Int = 27

    private val EMPTY_STACKS: List<ItemStack> by lazy { List(GRID_SLOTS) { ItemStack.EMPTY } }

}
