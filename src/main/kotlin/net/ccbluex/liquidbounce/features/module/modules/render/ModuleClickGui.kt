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
@file:OptIn(ExperimentalOneConfigUiApi::class)

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.integration.oneconfig.LbConfigTree
import net.ccbluex.liquidbounce.integration.oneconfig.LbModulesPage
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.inGame
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI

/**
 * ClickGUI module
 *
 * Pressing the bind opens the LiquidBounce entry of the OneConfig sidebar, which lists every module with all of
 * its settings. The language setting switches the language of that page, on top of following the game language.
 */
@AddonApi
object ModuleClickGui :
    ClientModule("ClickGUI", ModuleCategories.RENDER, bind = InputConstants.KEY_RSHIFT, disableActivation = true) {

    private var language by enumChoice("Language", LanguageManager.clientLanguage)
        .doNotIncludeAlways()
        .onChanged { choice ->
            if (LanguageManager.clientLanguage != choice) {
                LanguageManager.clientLanguage = choice
            }

            LbConfigTree.invalidate()
        }

    override val running get() = true

    override fun onEnabled() {
        if (!LiquidBounce.isInitialized || !inGame) {
            return
        }

        val current = LanguageManager.clientLanguage
        if (language != current) {
            language = current
        }

        OneConfigUI.open(LbModulesPage)
        super.onEnabled()
    }

}
