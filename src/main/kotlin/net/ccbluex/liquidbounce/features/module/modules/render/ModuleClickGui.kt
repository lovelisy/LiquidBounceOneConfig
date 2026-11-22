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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.inGame
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI

/**
 * ClickGUI module
 *
 * Pressing the bind opens the OneConfig screen, where the "LiquidBounce" card lists every module
 * grouped by its category. There is no separate LiquidBounce screen anymore.
 */
@AddonApi
object ModuleClickGui :
    ClientModule("ClickGUI", ModuleCategories.RENDER, bind = InputConstants.KEY_RSHIFT, disableActivation = true) {

    override val running get() = true

    override fun onEnabled() {
        if (!LiquidBounce.isInitialized || !inGame) {
            return
        }

        OneConfigUI.open()
        super.onEnabled()
    }

}
