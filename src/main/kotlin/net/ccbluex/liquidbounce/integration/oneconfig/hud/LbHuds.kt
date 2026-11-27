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

package net.ccbluex.liquidbounce.integration.oneconfig.hud

import net.ccbluex.liquidbounce.utils.client.logger
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

/**
 * Hands every LiquidBounce HUD element to OneConfig's HUD registry.
 *
 * Each element becomes its own unit in OneConfig's HUD editor, so it can be placed, scaled and toggled on its own. The
 * element itself only declares its options and its paint pass.
 */
object LbHuds {

    private val elements: List<LbHud> = listOf(
        TargetHud,
        ArrayListHud,
        NotificationsHud,
        KeyBindsHud,
        WatermarkHud,
    )

    /** Whether the HUD elements should be visible. OneConfig already hides them outside of a world. */
    private val enabledSupplier: () -> Boolean = { true }

    /** OneConfig queues the registration until its own HUD registry is ready, which is when the client boots. */
    fun init() {
        LbHudStyles.install()

        elements.forEach { element ->
            runCatching {
                HudManager.register(element, CONFIG_ID, CATEGORY_ICON, enabledSupplier)
            }.onFailure {
                logger.error("Unable to register the '${element.title}' HUD element", it)
            }
        }
    }

    /** Saves every pending HUD setting, called while the client shuts down. */
    fun flush() = LbHudStore.flushAll()

    /**
     * Config id shared by every element, the way Evergreen groups all of its HUD units: OneConfig stores the card
     * icon once per config id, so the whole LiquidBounce family shares a single icon entry.
     */
    private const val CONFIG_ID: String = "liquidbounce"

    /** Card icon every LiquidBounce HUD unit shows in OneConfig's HUD browser. */
    private const val CATEGORY_ICON: String = "/assets/liquidbounce/lb_logo_preview.png"

}
