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
import org.polyfrost.oneconfig.api.hud.v1.Hud
import kotlin.math.abs

/**
 * Shared drawing helpers for the native LiquidBounce HUD elements.
 *
 * Every element is drawn with the Inter family and the fixed native colour tokens, so the helpers here only take a
 * size, a weight and an ARGB colour. The background, the border and the padding of OneConfig's generic HUD units are
 * not used: the native cards paint their own surface.
 */
abstract class LbHudBase(id: String, title: String, category: Hud.Category) : LbHud(id, title, category) {

    protected fun hudFont(size: Float, weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR): PolyFont =
        LbHudFonts.font(size, weight)

    protected fun textWidth(
        text: String,
        size: Float,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float = LbHudFonts.measure(text, size, weight)

    protected fun lineHeight(
        size: Float,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float {
        val metrics = hudFont(size, weight).metrics
        val extent = abs(metrics.ascent) + abs(metrics.descent) + metrics.leading
        return extent.coerceAtLeast(size)
    }

    /** Draws one line of [text] at [size] and returns the width that was painted. */
    protected fun drawText(
        canvas: LbHudCanvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        argb: Int,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float = canvas.text(text, x, y, LbHudFonts.font(size, weight), PolyColor(argb))

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
    }

}
