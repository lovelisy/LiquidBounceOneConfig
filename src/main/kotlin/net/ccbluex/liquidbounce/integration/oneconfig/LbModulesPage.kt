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

package net.ccbluex.liquidbounce.integration.oneconfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage
import org.polyfrost.oneconfig.api.ui.v1.page.PageGroup
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.internal.ui.screens.ConfigScreen

private const val PAGE_ID = "liquidbounce:modules"

/**
 * The LiquidBounce entry in the OneConfig sidebar.
 *
 * It renders the LiquidBounce settings with OneConfig's own config screen, so every module and every module
 * setting is available right inside OneConfig instead of in a separate LiquidBounce screen.
 */
object LbModulesPage : OneConfigPage(
    PAGE_ID,
    PageText.literal("LiquidBounce"),
    "/assets/liquidbounce/lb_logo_preview.png",
    PageGroup.MODS_AND_OPTIONS,
) {

    override val order: Int
        get() = 50

    @Composable
    override fun Content() {
        val revision = LbConfigTree.revision
        val tree = remember(revision) { LbConfigTree.build() }

        ConfigScreen(
            tree = tree,
            pageKey = PAGE_ID,
        )
    }

}
