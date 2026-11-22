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

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Colour tokens and animation helpers of the native LiquidBounce HUD.
 *
 * These are the exact values the native client derives from its `_colors.scss` tokens: the surfaces are a black tint,
 * the accent is the LiquidBounce blue and the semantic colours match the native notification and target HUD cards.
 */
object LbHudPalette {

    const val TEXT_COLOR: Int = 0xFFFFFFFF.toInt()
    const val TEXT_DIMMED_COLOR: Int = 0xFFCBD5E1.toInt()
    const val ARRAYLIST_TAG_COLOR: Int = 0xFFAAAAAA.toInt()

    const val SUCCESS_COLOR: Int = 0xFF4DAC68.toInt()
    const val ERROR_COLOR: Int = 0xFFFC4130.toInt()
    const val HOTBAR_HEALTH_COLOR: Int = 0xFFFC4130.toInt()

    const val DEFAULT_ACCENT: Int = 0xFF4677FF.toInt()
    const val DEFAULT_TINT: Int = 0xFF000000.toInt()

    /** Accent colour, scaled to [alpha]. */
    fun accent(alpha: Float = 1f): Int = withAlphaFactor(DEFAULT_ACCENT, alpha)

    /** Native surface colour, scaled to [alpha]. */
    fun surface(alpha: Float): Int = withAlphaFactor(DEFAULT_TINT, alpha)

    /** Multiplies the alpha channel of [argb] by [factor], keeping the colour channels untouched. */
    fun withAlphaFactor(argb: Int, factor: Float): Int {
        val clamped = factor.coerceIn(0f, 1f)
        val alpha = (((argb ushr 24) and 0xFF) * clamped).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or (argb and 0x00FFFFFF)
    }

    /** Frame rate independent exponential interpolation, matching the native HUD animation curve. */
    fun expLerp(current: Float, target: Float, speed: Float, deltaSeconds: Float): Float {
        if (deltaSeconds <= 0f) {
            return current
        }

        val factor = 1f - exp(-speed * deltaSeconds.coerceAtMost(0.1f))
        val next = current + (target - current) * factor
        return if (abs(target - next) < 0.0005f) target else next
    }

}
