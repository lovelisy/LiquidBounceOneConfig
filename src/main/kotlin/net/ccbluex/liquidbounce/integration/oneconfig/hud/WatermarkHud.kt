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

import net.ccbluex.liquidbounce.LiquidBounce
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Draws the client name in the corner of the screen.
 */
object WatermarkHud : LbHudBase("watermark", "Watermark", Hud.Category.INFO) {

    @Text(title = "Text")
    @JvmField
    var text: String = "LiquidBounce"

    @Switch(title = "Show Version")
    @JvmField
    var showVersion: Boolean = true

    @Color(title = "Text Color", alpha = true)
    @JvmField
    var color: PolyColor = PolyColor(0xFF57C0FF.toInt())

    @Switch(title = "Rainbow")
    @JvmField
    var rainbow: Boolean = false

    @Slider(title = "Rainbow Speed", min = 1f, max = 20f, step = 1f)
    @JvmField
    var rainbowSpeed: Float = 6f

    override fun paint(canvas: LbHudCanvas): HudSize {
        val font = canvas.font(fontSize, fontName)
        val label = if (showVersion) "$text v${LiquidBounce.clientVersion}" else text

        val contentWidth = canvas.measure(label, font)
        val contentHeight = canvas.lineHeight(font)

        val size = backdrop(canvas, contentWidth, contentHeight)
        val painted = if (rainbow) canvas.rainbow(rainbowSpeed) else color
        drawLabel(content(canvas), label, 0f, 0f, painted, font)

        return size
    }

}
