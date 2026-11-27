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

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Background controls every LiquidBounce HUD element carries itself.
 *
 * OneConfig's own background card is not used: it can only paint one flat rectangle behind the whole content area,
 * while the native elements build their surface out of several pieces, such as the split header and body of the key
 * bind list or the per row cards of the array list. Each element therefore paints its own surface and offers the
 * colour right here in its unit settings.
 *
 * The colour is a [PolyColor], which OneConfig's colour control edits with the alpha slider, the chroma cycle and the
 * gradient that every other OneConfig surface already offers.
 *
 * @param defaultColor the colour a fresh element starts with, matching the native surface alpha of that element.
 */
abstract class LbHudSurface(
    id: String,
    title: String,
    category: Hud.Category,
    defaultColor: Int,
) : LbHudBase(id, title, category) {

    /** Whether the surface is painted behind the content. */
    @Switch(title = "Show Background")
    @JvmField
    var showSurface: Boolean = true

    /** Fill colour of the surface, including its transparency. */
    @Color(title = "Background Color", alpha = true, gradient = true)
    @JvmField
    var surfaceColor: PolyColor = PolyColor(defaultColor)

    /** The surface colour in use, fully transparent while the surface is switched off. */
    protected fun surfaceArgb(): Int = if (showSurface) surfaceColor.rawArgb else 0

    /** The surface colour at [factor] of its transparency, for surfaces made of several layers. */
    protected fun surfaceArgb(factor: Float): Int = LbHudPalette.withAlphaFactor(surfaceArgb(), factor)

}
