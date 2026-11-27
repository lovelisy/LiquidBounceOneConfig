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

/**
 * Converts the geometry of the native HUD into the units OneConfig draws in.
 *
 * The native client describes its HUD in a CSS viewport of `guiScaledWidth * 2` by `guiScaledHeight * 2` and scales that
 * viewport by `0.5` while blitting it, so one design unit of the native sources equals half a Minecraft GUI unit.
 *
 * The elements whose extent comes straight from those numbers use [px]: an item slot is `32` design units and a key cap
 * is `50`, so copying them into a OneConfig canvas drew both at twice their native size, which is why the inventory, the
 * statistics grid, the armour column and the key display used to take up half of the screen.
 *
 * Only boxes, paddings, gaps and radii go through [px]. Font sizes do not: they are read in Minecraft GUI units and are
 * multiplied by the text scale of the style panel inside `hudFont`, so converting them again would shrink the labels
 * twice. Every label of every element therefore stays the size the style panel says, and only the plates shrink.
 */
internal object LbHudScale {

    /** One design unit of the native HUD, expressed in Minecraft GUI units. */
    const val PX: Float = 0.5f

    /** Converts a length of the native design into Minecraft GUI units. */
    fun px(value: Float): Float = value * PX

}
