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

import org.polyfrost.compose.render.PolyFont

/**
 * Hands out [PolyFont] instances for the native LiquidBounce HUD elements.
 *
 * The native HUD draws every line with Inter, so the four weights of PolyCompose's own Inter family are used directly;
 * they are always present because PolyCompose registers them on startup. Strings that contain glyphs outside of Inter,
 * which is every CJK ideograph, fall back to OneConfig's bundled unifont.
 */
object LbHudFonts {

    /**
     * Font engine keys of PolyCompose's own Inter family. The mod ships the same four TTF files and registers them
     * on startup, so they are always present and the client does not have to bundle them again.
     */
    enum class Weight(val key: String) {
        REGULAR("inter"),
        MEDIUM("inter-medium"),
        SEMIBOLD("inter-semibold"),
        BOLD("inter-bold"),
    }

    /**
     * Font engine key of OneConfig's bundled unifont, used whenever a string contains glyphs that Inter does not
     * carry, which is every CJK ideograph. The stb font engine has no glyph level fallback, so callers have to
     * switch fonts themselves.
     */
    private const val UNIFONT_KEY = "unifont"

    /** Inter at [size] logical pixels, in [weight]. */
    fun font(size: Float, weight: Weight = Weight.REGULAR): PolyFont = PolyFont(weight.key, size)

    /**
     * Picks the font for [text]: Inter whenever the string is fully covered by it, otherwise OneConfig's unifont,
     * which carries every CJK glyph. The choice is per string, matching how the native HUD components pass whole
     * lines of text.
     */
    fun fontFor(text: String, size: Float, weight: Weight = Weight.REGULAR): PolyFont =
        if (needsCjkFallback(text)) PolyFont(UNIFONT_KEY, size) else font(size, weight)

    /** Width of [text] in the font that [fontFor] would pick, at [size]. */
    fun measureFor(text: String, size: Float, weight: Weight = Weight.REGULAR): Float =
        if (text.isEmpty()) 0f else fontFor(text, size, weight).measureTextWidth(text)

    /** Whether [text] contains at least one glyph outside the Latin, punctuation and symbol ranges of Inter. */
    fun needsCjkFallback(text: String): Boolean = text.any { it.code >= 0x2E80 }

    /** Width of [text] in [weight] at [size], without needing a render context. */
    fun measure(text: String, size: Float, weight: Weight = Weight.REGULAR): Float =
        if (text.isEmpty()) 0f else font(size, weight).measureTextWidth(text)

}
