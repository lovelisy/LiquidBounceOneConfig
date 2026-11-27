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

import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Native LiquidBounce watermark.
 *
 * The badge and the wordmark are two layered textures of the native client. The badge is tinted with the accent colour
 * and the wordmark keeps the text colour, so the logo follows the theme of every other HUD element.
 *
 * It draws no text, so it drops the text card of OneConfig's style panel. The background colour, the visibility and
 * the radius come from the background card of the panel like on every other element.
 */
object WatermarkHud : LbHudSurface("watermark", "Watermark", Hud.Category.INFO, LbHudPalette.surface(0.68f)) {

    override val defaultShown: Boolean
        get() = true

    /** The logo paints two textures and no text, so only the text card of the style panel is dropped. */
    override val hiddenPanelSections: Set<String>
        get() = setOf(LbHudPanels.TEXT)

    override fun measure(): HudSize = HudSize(BASE_WIDTH, BASE_HEIGHT)

    override fun paint(canvas: LbHudCanvas) {
        canvas.fill(0f, 0f, BASE_WIDTH, BASE_HEIGHT, surfaceArgb(), surfaceRadius())
        canvas.stretchedIcon(BADGE, 0f, 0f, BASE_WIDTH, BASE_HEIGHT, accentArgb())
        canvas.stretchedIcon(FOREGROUND, 0f, 0f, BASE_WIDTH, BASE_HEIGHT, textColor)
    }

    private const val BADGE: String = "textures/ui/hud/watermark_badge.png"
    private const val FOREGROUND: String = "textures/ui/hud/watermark_fg.png"

    private const val BASE_WIDTH: Float = 165f
    private const val BASE_HEIGHT: Float = 61.8917f

}
