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
package net.ccbluex.liquidbounce.integration.oneconfig

import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * Bootstraps the OneConfig integration.
 *
 * OneConfig owns the whole UI now: the module list lives on the [LiquidBounceConfig] card and
 * [LbPlayerInfoHud] lives in the OneConfig HUD system. Nothing is drawn with the old WebUI anymore.
 */
object OneConfigIntegration {

    private var config: LiquidBounceConfig? = null

    val liquidBounceConfig: LiquidBounceConfig?
        get() = config

    fun init() {
        if (config != null) {
            return
        }

        // Constructing the config submits it to OneConfig's initialization queue, which registers the
        // "LiquidBounce" card with every module on it. Initialize it now in case OneConfig already finished
        // its own startup pass, otherwise the card would only appear after a restart.
        config = LiquidBounceConfig().also { it.preload() }

        // Register the player information HUD under the same config id, so OneConfig groups it with our card.
        HudManager.register(LbPlayerInfoHud, LiquidBounceConfig.ID)
    }

}
