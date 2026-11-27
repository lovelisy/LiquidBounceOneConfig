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
 * Only the semantic colours of the native design live here. The text colours are not listed: every element derives its
 * primary text from `Hud.textColor` and its secondary text from that same colour at a lower alpha, so the colour control
 * of the style panel governs all of them, and the accent of an element comes from its own `accentColor` unit option.
 */
object LbHudPalette {

    const val SUCCESS_COLOR: Int = 0xFF4DAC68.toInt()
    const val ERROR_COLOR: Int = 0xFFFC4130.toInt()
    const val HOTBAR_HEALTH_COLOR: Int = 0xFFFC4130.toInt()

    const val DEFAULT_ACCENT: Int = 0xFF4677FF.toInt()
    const val DEFAULT_TINT: Int = 0xFF000000.toInt()

    /** Native surface colour, scaled to [alpha]. */
    fun surface(alpha: Float): Int = withAlphaFactor(DEFAULT_TINT, alpha)

    /** Multiplies the alpha channel of [argb] by [factor], keeping the colour channels untouched. */
    fun withAlphaFactor(argb: Int, factor: Float): Int {
        val clamped = factor.coerceIn(0f, 1f)
        val alpha = (((argb ushr 24) and 0xFF) * clamped).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or (argb and 0x00FFFFFF)
    }

    /** Blends two ARGB colours channel by channel at [ratio] of the way from [from] to [to]. */
    fun lerpArgb(from: Int, to: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)

        fun channel(a: Int, b: Int): Int = (a + (b - a) * t).roundToInt().coerceIn(0, 255)

        val alpha = channel(from ushr 24, to ushr 24)
        val red = channel((from shr 16) and 0xFF, (to shr 16) and 0xFF)
        val green = channel((from shr 8) and 0xFF, (to shr 8) and 0xFF)
        val blue = channel(from and 0xFF, to and 0xFF)
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
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
