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
 * Draws the client name and its version.
 */
object WatermarkHud : LbHudBase("watermark", "Watermark", Hud.Category.INFO) {

    @Text(title = "Label")
    @JvmField
    var label: String = "LiquidBounce"

    @Switch(title = "Show Version")
    @JvmField
    var showVersion: Boolean = true

    @Color(title = "Accent Color", alpha = true)
    @JvmField
    var accent: PolyColor = PolyColor(0xFF57C0FF.toInt())

    @Switch(title = "Rainbow")
    @JvmField
    var rainbow: Boolean = false

    @Slider(title = "Rainbow Speed", min = 1f, max = 20f, step = 1f)
    @JvmField
    var rainbowSpeed: Float = 6f

    private fun text(): String = if (showVersion) "$label v${LiquidBounce.clientVersion}" else label

    override fun measure(): HudSize {
        val text = text()
        return HudSize(textWidth(text), lineHeight())
    }

    override fun paint(canvas: LbHudCanvas) {
        val font = hudFont()
        val color = if (rainbow) canvas.rainbow(rainbowSpeed) else accent

        drawText(canvas, text(), 0f, 0f, color, font)
    }

}
