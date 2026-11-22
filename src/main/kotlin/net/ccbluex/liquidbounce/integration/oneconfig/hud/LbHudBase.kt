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
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Common helper layer for LiquidBounce HUD elements.
 *
 * The visual options, meaning the background, the border, the padding, the font, the text colour and the shadow, are
 * provided by [Hud] and edited in the element's own settings panel, so they are not declared again here. What this class
 * adds is the plumbing every element needs: turning the shared font option into a [PolyFont] and drawing a line of text
 * with the shared colour and shadow settings.
 */
abstract class LbHudBase(id: String, title: String, category: Hud.Category) : LbHud(id, title, category) {

    /** The font the element should draw with, built from the font options OneConfig exposes for this element. */
    protected fun hudFont(): PolyFont {
        val size = FONT_BASE_SIZE * textScale.coerceAtLeast(0f)
        return when (font) {
            Font.Poppins -> PolyFont(POPPINS_FONT, size)
            Font.Minecraft -> PolyFont(LbHudCanvas.GAME_FONT, size)
        }
    }

    /** The colour the element should draw its text with. */
    protected fun hudTextColor(): PolyColor = PolyColor(textColor)

    /** Width of [text] in [font], available before the element is drawn. */
    protected fun textWidth(text: String, font: PolyFont = hudFont()): Float =
        if (text.isEmpty()) 0f else font.measureTextWidth(text)
    /** Line height of [font], based on the metrics the font engine reports. */
    protected fun lineHeight(font: PolyFont = hudFont()): Float {
        val metrics = font.metrics
        val extent = kotlin.math.abs(metrics.ascent) + kotlin.math.abs(metrics.descent) + metrics.leading
        return extent.coerceAtLeast(font.size)
    }

    /** Draws one line of [text], honouring the element's shadow switch. */
    protected fun drawText(
        canvas: LbHudCanvas,
        text: String,
        x: Float,
        y: Float,
        color: PolyColor = hudTextColor(),
        font: PolyFont = hudFont(),
    ) {
        if (showShadow) {
            canvas.textShadow(text, x, y, font, color.argb)
        } else {
            canvas.text(text, x, y, font, color)
        }
    }

    private companion object {
        /** Font key PolyCompose maps onto the OneConfig Poppins family. */
        const val POPPINS_FONT: String = "poppins"

        /** Size one font option unit maps onto, matching the scale OneConfig uses for its own HUD text. */
        const val FONT_BASE_SIZE: Float = 8f
    }

}
