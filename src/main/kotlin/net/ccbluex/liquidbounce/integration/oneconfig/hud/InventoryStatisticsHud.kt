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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.polyfrost.oneconfig.api.config.v1.annotations.ItemList
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import kotlin.math.max

/** Transparent surface, used by the elements the native client draws without a plate. */
/**
 * Native inventory statistics.
 *
 * The element counts a chosen set of items over the whole inventory and prints the total in the corner of the icon,
 * so a player can see at a glance how many emeralds or diamonds are left. The number is not the stack size but the sum
 * over every slot, which is what the native element does as well.
 */
object InventoryStatisticsHud :
    LbHudSurface("inventory_statistics", "Inventory Statistics", Hud.Category.INFO, 0) {

    /** Ids of the tracked items, resolved against the item registry every paint pass. */
    @ItemList(title = "Items")
    @JvmField
    var trackedItems: List<String> = listOf(
        "minecraft:emerald",
        "minecraft:diamond",
        "minecraft:gold_ingot",
        "minecraft:iron_ingot",
        "minecraft:copper_ingot",
    )

    /** Keeps an item in the grid while the count is zero, so a row does not vanish once it runs out. */
    @Switch(title = "Show Empty")
    @JvmField
    var showEmpty: Boolean = true

    /** Icons per row, from a single column to a full 9 wide bar. */
    @Slider(title = "Row Length", min = 1f, max = 9f, step = 1f)
    @JvmField
    var rowLength: Float = 1f

    private val columns: Int
        get() = rowLength.toInt().coerceIn(1, 9)

    /** Resolves the configured ids, dropping entries the registry does not know. */
    private fun selected(): List<Item> = trackedItems.mapNotNull { id ->
        Identifier.tryParse(id)?.let { BuiltInRegistries.ITEM.getValue(it) }
    }

    /** Totals of the configured items over the storage slots of the player inventory. */
    private fun totals(items: List<Item>): Map<Item, Int> {
        if (items.isEmpty()) {
            return emptyMap()
        }

        val wanted = items.toHashSet()
        val totals = HashMap<Item, Int>(items.size * 2)
        val slots = mc.player?.inventory?.nonEquipmentItems.orEmpty()
        for (stack in slots) {
            if (stack.isEmpty || stack.item !in wanted) {
                continue
            }
            totals[stack.item] = (totals[stack.item] ?: 0) + stack.count
        }
        return totals
    }

    /** Entries the grid prints, in configuration order. */
    private fun entries(): List<Pair<Item, Int>> {
        val selected = selected()
        if (selected.isEmpty()) {
            return emptyList()
        }

        val totals = totals(selected)
        return selected
            .map { it to (totals[it] ?: 0) }
            .filter { showEmpty || it.second > 0 }
    }

    override fun measure(): HudSize {
        val count = entries().size
        if (count == 0) {
            val fallback = HEADLESS_SIZE * textScale
            return HudSize(fallback, fallback)
        }
        return LbInventoryGrid.size(count, columns, GAP, textScale)
    }

    override fun paint(canvas: LbHudCanvas) {
        val entries = entries()
        if (entries.isEmpty()) {
            if (canvas.inEditor) {
                LbInventoryGrid.draw(canvas, OWNER, sampleStacks(), 1, GAP, canvas.surfaceArgb())
            }
            return
        }

        val stacks = ArrayList<ItemStack>(entries.size)
        val labels = HashMap<Int, String>(entries.size * 2)
        entries.forEachIndexed { index, (item, total) ->
            stacks.add(ItemStack(item, max(1, total)))
            labels[index] = total.toString()
        }

        LbInventoryGrid.draw(canvas, OWNER, stacks, columns, GAP, canvas.surfaceArgb(), labels)
    }

    private const val OWNER: String = "inventory_statistics"

    private const val GAP: Float = 2f

    /** Extent of the editor preview while nothing is tracked. */
    private const val HEADLESS_SIZE: Float = 40f

    /**
     * Sample icon drawn by the editor while nothing is tracked yet.
     *
     * Built on demand instead of being a field: the elements are initialised while the client is still starting, and
     * `ItemStack` reads the component map of the item, which the registry only binds later. A stack created in a class
     * initialiser therefore rushes the registry and crashes the integration with `Components not bound yet`.
     */
    private fun sampleStacks(): List<ItemStack> = listOf(ItemStack(Items.DIAMOND, 1))

}
