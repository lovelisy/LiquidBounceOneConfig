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
 * Shared drawing helpers for the native LiquidBounce HUD elements.
 *
 * The font family, the size multiplier, the weight and the text colour all come from the HUD settings panel of
 * OneConfig, where the game font is the default. The native layout only decides which weight a line uses, so names
 * stay medium and labels stay semibold while the family stays under the user's control. The background, the border
 * and the padding of OneConfig's generic HUD units are not used: the native cards paint their own surface.
 */
abstract class LbHudBase(id: String, title: String, category: Hud.Category) : LbHud(id, title, category) {

    protected fun hudFont(size: Float, weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR): PolyFont =
        PolyFont(fontKey(weight), (size * textScale).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE))

    private fun fontKey(weight: LbHudFonts.Weight): String {
        if (font != Font.Poppins) {
            return LbHudCanvas.GAME_FONT
        }

        val base = if (textBold) LbHudFonts.Weight.BOLD.key else weight.key
        return if (textItalic) "$base-italic" else base
    }

    protected fun textWidth(
        text: String,
        size: Float,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float = if (text.isEmpty()) 0f else hudFont(size, weight).measureTextWidth(text)

    protected fun lineHeight(
        size: Float,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float {
        val metrics = hudFont(size, weight).metrics
        return (metrics.descent - metrics.ascent).coerceAtLeast(size)
    }

    /** Draws one line of [text] at [size] and returns the width that was painted. */
    protected fun drawText(
        canvas: LbHudCanvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        argb: Int = textColor,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float = canvas.text(text, x, y, hudFont(size, weight), PolyColor(argb))

    /** Shortens [text] with an ellipsis until it fits into [maxWidth]. */
    protected fun ellipsize(
        text: String,
        maxWidth: Float,
        size: Float,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): String {
        if (text.isEmpty() || maxWidth <= 0f) {
            return ""
        }
        if (textWidth(text, size, weight) <= maxWidth) {
            return text
        }

        val ellipsis = ELLIPSIS
        val ellipsisWidth = textWidth(ellipsis, size, weight)
        if (ellipsisWidth > maxWidth) {
            return ""
        }

        var end = text.length
        while (end > 0 && textWidth(text.substring(0, end), size, weight) + ellipsisWidth > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }

    private companion object {
        const val ELLIPSIS: String = "..."
        const val MIN_FONT_SIZE: Float = 1f
        const val MAX_FONT_SIZE: Float = 256f
    }

}
