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

import net.ccbluex.liquidbounce.utils.client.logger
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyFont

/**
 * Registers the Inter family used by the native LiquidBounce HUD elements and hands out [PolyFont] instances.
 *
 * The native HUD draws every line with Inter, so the four weights bundled with the client are registered into
 * OneConfig's font engine under LiquidBounce owned keys. The registration is lazy: the font engine is only ready once
 * the client booted, which is after this object is created.
 */
object LbHudFonts {

    /** One bundled Inter weight, its font engine key and the classpath resource it is read from. */
    enum class Weight(val key: String, val resource: String) {
        REGULAR("lb-inter-regular", "assets/liquidbounce/fonts/Inter-Regular.ttf"),
        MEDIUM("lb-inter-medium", "assets/liquidbounce/fonts/Inter-Medium.ttf"),
        SEMIBOLD("lb-inter-semibold", "assets/liquidbounce/fonts/Inter-SemiBold.ttf"),
        BOLD("lb-inter-bold", "assets/liquidbounce/fonts/Inter-Bold.ttf"),
    }

    @Volatile
    private var registered: Boolean = false

    @Synchronized
    private fun ensureRegistered() {
        if (registered) {
            return
        }

        val classLoader = LbHudFonts::class.java.classLoader
        val loaded = Weight.entries.count { weight ->
            runCatching {
                FontManager.loadFromResource(weight.resource, weight.key, classLoader, false)
            }.getOrDefault(false)
        }

        registered = loaded == Weight.entries.size
        if (!registered) {
            logger.warn(
                "Unable to register all LiquidBounce HUD fonts, only {} of {} Inter weights are available",
                loaded,
                Weight.entries.size,
            )
        }
    }

    /** Inter at [size] logical pixels, in [weight]. */
    fun font(size: Float, weight: Weight = Weight.REGULAR): PolyFont {
        ensureRegistered()
        return PolyFont(weight.key, size)
    }

    /** Width of [text] in [weight] at [size], without needing a render context. */
    fun measure(text: String, size: Float, weight: Weight = Weight.REGULAR): Float =
        if (text.isEmpty()) 0f else font(size, weight).measureTextWidth(text)

}
