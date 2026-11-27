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
 * It draws no text, so it hides the text styling options of [Hud] and keeps a fixed `165 x 61.89` artwork size. The
 * user still scales it through OneConfig's own HUD scale.
 */
object WatermarkHud : LbHudBase("watermark", "Watermark", Hud.Category.INFO) {

    /**
     * The logo paints two textures and no text, so it hides everything but the background it may want to sit on,
     * unlike the text elements which paint their own card and therefore hide the background switch too.
     */
    override val hiddenOptions: Set<String>
        get() = LbHudOptions.UNUSED_BY_ARTWORK

    /** The logo has no plate of its own, so OneConfig's background switch stays available for it. */
    override val paintsOwnSurface: Boolean
        get() = false

    override fun measure(): HudSize = HudSize(BASE_WIDTH, BASE_HEIGHT)

    override fun paint(canvas: LbHudCanvas) {
        canvas.stretchedIcon(BADGE, 0f, 0f, BASE_WIDTH, BASE_HEIGHT, LbHudPalette.accent(1f))
        canvas.stretchedIcon(FOREGROUND, 0f, 0f, BASE_WIDTH, BASE_HEIGHT, textColor)
    }

    private const val BADGE: String = "textures/ui/hud/watermark_badge.png"
    private const val FOREGROUND: String = "textures/ui/hud/watermark_fg.png"

    private const val BASE_WIDTH: Float = 165f
    private const val BASE_HEIGHT: Float = 61.8917f

}
