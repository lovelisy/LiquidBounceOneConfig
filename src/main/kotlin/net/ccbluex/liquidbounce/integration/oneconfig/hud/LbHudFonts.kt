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

import org.polyfrost.compose.render.PolyFont

/**
 * Font weight keys of the Poppins family that ships with OneConfig.
 *
 * The font family itself is chosen in the HUD settings of every element, where the game font is the default. When
 * Poppins is selected, these keys pick the face of the matching weight. OneConfig only registers `poppins`,
 * `poppins-thin`, `poppins-medium`, `poppins-bold` and `poppins-black`, so there is no key for SemiBold; the native
 * layout resolves it to the Medium cut instead of asking for a face that would fall back to the default font.
 */
object LbHudFonts {

    enum class Weight(val key: String) {
        THIN("poppins-thin"),
        REGULAR("poppins"),
        MEDIUM("poppins-medium"),
        SEMIBOLD("poppins-medium"),
        BOLD("poppins-bold"),
        BLACK("poppins-black"),
    }

    /**
     * Font of the stack count printed in the corner of an item icon.
     *
     * The size scales with the box the icon is drawn in, mirroring the way the vanilla count grows with the icon, and
     * stays legible on the smallest slot.
     */
    fun count(boxSize: Float, textScale: Float = 1f): PolyFont =
        PolyFont(Weight.BOLD.key, (boxSize * COUNT_SIZE_FACTOR * textScale).coerceIn(MIN_SIZE, MAX_SIZE))

    private const val COUNT_SIZE_FACTOR = 0.28f
    private const val MIN_SIZE = 7f
    private const val MAX_SIZE = 18f

}
