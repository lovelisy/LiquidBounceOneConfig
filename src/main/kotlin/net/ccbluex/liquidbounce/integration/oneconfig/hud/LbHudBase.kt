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
 * OneConfig, where Poppins is the default. The native layout only decides which weight a line uses; picking a weight
 * other than `Regular` in the panel overrides it for every line, so the choice is always visible. The generic
 * background, border and shadow of OneConfig's HUD units are not used: the native cards paint their own surface.
 */
abstract class LbHudBase(id: String, title: String, category: Hud.Category) : LbHud(id, title, category) {

    protected fun hudFont(size: Float, weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR): PolyFont =
        PolyFont(fontKey(resolvedWeight(weight)), (size * textScale).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE))

    /**
     * Font built from a composition snapshot, so a font/text-scale change in the style panel repaints immediately.
     * [measure] keeps using [hudFont] because size changes are reported through update() instead.
     */
    protected fun hudFont(style: LbHudStyle, size: Float, weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR): PolyFont =
        PolyFont(
            fontKey(style, resolvedWeight(style, weight)),
            (size * style.textScale).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
        )

    private fun resolvedWeight(native: LbHudFonts.Weight): LbHudFonts.Weight = resolvedWeight(
        LbHudStyle(
            textColor, showBackground, bgColor, bgRadius, textScale, font, textBold, textItalic, textWeight
        ),
        native,
    )

    private fun resolvedWeight(style: LbHudStyle, native: LbHudFonts.Weight): LbHudFonts.Weight {
        if (style.textBold) {
            return LbHudFonts.Weight.BOLD
        }

        return when (style.textWeight.name) {
            "Thin" -> LbHudFonts.Weight.THIN
            "Medium" -> LbHudFonts.Weight.MEDIUM
            "Bold" -> LbHudFonts.Weight.BOLD
            "Black" -> LbHudFonts.Weight.BLACK
            else -> native
        }
    }

    private fun fontKey(weight: LbHudFonts.Weight): String = fontKey(
        LbHudStyle(
            textColor, showBackground, bgColor, bgRadius, textScale, font, textBold, textItalic, textWeight
        ),
        weight,
    )

    private fun fontKey(style: LbHudStyle, weight: LbHudFonts.Weight): String {
        if (style.font != Font.Poppins) {
            return LbHudCanvas.GAME_FONT
        }
        if (!style.textItalic) {
            return weight.key
        }
        return "${weight.key}-italic"
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
        val extent = metrics.descent - metrics.ascent + metrics.leading
        return extent.coerceAtLeast(size)
    }

    /** Draws one line of [text] at [size] and returns the width that was painted. */
    protected fun drawText(
        canvas: LbHudCanvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        argb: Int = canvas.style.textColor,
        weight: LbHudFonts.Weight = LbHudFonts.Weight.REGULAR,
    ): Float = canvas.text(text, x, y, hudFont(canvas.style, size, weight), PolyColor(argb))

    /**
     * Secondary text colour.
     *
     * It is the text colour of the style panel with reduced opacity instead of a fixed grey, so changing the colour in
     * the panel moves every label of the element, not just the primary ones. The native design expresses the same
     * hierarchy with `text-muted` and `text-dimmed` tokens, which are also a tint of the primary text.
     */
    protected fun dimTextArgb(factor: Float = DIM_TEXT_FACTOR): Int =
        LbHudPalette.withAlphaFactor(textColor, factor)

    /** Tertiary text colour, for the tag of the array list and other faint annotations. */
    protected fun faintTextArgb(factor: Float = FAINT_TEXT_FACTOR): Int =
        LbHudPalette.withAlphaFactor(textColor, factor)

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

    /** Splits `KillAura` into `Kill Aura`, matching the native client's camel case word splitting. */
    protected fun displayName(name: String): String {
        if (name.isEmpty() || name.contains(' ')) {
            return name
        }

        val words = SPACED_WORD_REGEX.findAll(name).map { it.value }.toList()
        return if (words.isEmpty()) name else words.joinToString(" ")
    }

    private companion object {
        const val ELLIPSIS: String = "..."
        const val MIN_FONT_SIZE: Float = 1f
        const val MAX_FONT_SIZE: Float = 256f
        const val DIM_TEXT_FACTOR: Float = 0.78f
        const val FAINT_TEXT_FACTOR: Float = 0.67f
        val SPACED_WORD_REGEX: Regex = Regex("[A-Z]?[a-z]+|[A-Z]+[0-9]+|[0-9]+|[A-Z]+(?![a-z])")
    }

}
