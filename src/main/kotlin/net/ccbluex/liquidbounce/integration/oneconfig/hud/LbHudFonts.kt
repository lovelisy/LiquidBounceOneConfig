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
 * Font weight keys of the Poppins family that ships with OneConfig.
 *
 * The font family itself is chosen in the HUD settings of every element, where the game font is the default. When
 * Poppins is selected, these keys pick the face of the matching weight; the native LiquidBounce layout keeps using
 * its own weight for every line, so names stay medium and labels stay semibold.
 */
object LbHudFonts {

    enum class Weight(val key: String) {
        REGULAR("poppins"),
        MEDIUM("poppins-medium"),
        SEMIBOLD("poppins-semibold"),
        BOLD("poppins-bold"),
    }

}
