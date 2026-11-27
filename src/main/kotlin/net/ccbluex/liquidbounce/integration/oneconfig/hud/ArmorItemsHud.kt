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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.hud.v1.Hud

/** Transparent surface, used by the elements the native client draws without a plate. */
private const val TRANSPARENT: Int = 0

/**
 * Native armour display.
 *
 * The four armour slots follow the native layout: a vertical column by default and a single row on request. The plate
 * starts out fully transparent, because the native element draws the icons directly onto the screen without a card
 * behind them. The colour control lets a player add one anyway.
 */
object ArmorItemsHud :
    LbHudSurface("armor_items", "Armor Items", Hud.Category.INFO, TRANSPARENT) {

    @Dropdown(title = "Layout", options = ["Vertical", "Horizontal"])
    @JvmField
    var layout: String = "Vertical"

    private val columns: Int
        get() = if (layout == "Horizontal") SLOT_COUNT else 1

    private fun stacks(): List<ItemStack> {
        val player = mc.player ?: return EMPTY_STACKS
        return listOf(
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET),
        )
    }

    override fun measure(): HudSize = LbInventoryGrid.size(SLOT_COUNT, columns, GAP)

    override fun paint(canvas: LbHudCanvas) {
        LbInventoryGrid.draw(canvas, stacks(), columns, GAP, surfaceArgb())
    }

    private const val SLOT_COUNT: Int = 4
    private const val GAP: Float = 2f

    private val EMPTY_STACKS: List<ItemStack> = List(SLOT_COUNT) { ItemStack.EMPTY }

}
