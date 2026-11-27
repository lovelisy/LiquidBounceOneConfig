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
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.HudStyle
import org.polyfrost.oneconfig.api.hud.v1.HudStyles
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

/**
 * Declares the look every LiquidBounce HUD element starts with.
 *
 * OneConfig derives the default of a HUD unit from its own `HudStyle`, and its style panel restores that value through
 * the reset button. Registering the native design as the family default keeps the panel, the reset button and a fresh
 * configuration in agreement, instead of hiding options the panel would keep offering anyway.
 *
 * The elements paint their own card, so the style only carries what is left: the Poppins family, no frame, no shadow
 * and no padding around the content.
 */
@OptIn(ExperimentalOneConfigUiApi::class)
internal object LbHudStyles {

    /** Installs the family default, to be called before the elements are registered. */
    fun install() {
        runCatching {
            HudStyles.setDefault(CONFIG_ID, NATIVE)
        }.onFailure {
            logger.error("Unable to declare the default style of the LiquidBounce HUD elements", it)
        }
    }

    /**
     * The native design.
     *
     * The padding is pinned to zero on every side: a `HudStyle` that only sets `padding` would make OneConfig add that
     * inset to an element which lays its content out on its own.
     */
    private val NATIVE: HudStyle = HudStyle(
        font = Font.Poppins,
        textColor = PolyColor(0xFFFFFFFF.toInt()),
        shadow = false,
        background = false,
        border = false,
        borderSize = 0f,
        radius = 4f,
        padding = 0f,
        paddingTop = 0f,
        paddingBottom = 0f,
        paddingLeft = 0f,
        paddingRight = 0f,
        scale = 1f,
        textScale = 1f,
        textWeight = Weight.Regular,
        caseType = 0,
        textBold = false,
        textItalic = false,
        textUnderline = false,
        useGuiScale = true,
    )

    private const val CONFIG_ID: String = "liquidbounce"

}
