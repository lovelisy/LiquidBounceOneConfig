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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.ChatFormatting
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.page.PageText

private const val PLAYER_INFO_HUD_ID = "liquidbounce.playerinfo"

/** Own section in OneConfig's HUD library, so LiquidBounce HUDs are not mixed with unrelated ones. */
private val LB_HUD_CATEGORY: Hud.Category =
    Hud.Category.create("liquidbounce.hud", PageText.literal("LiquidBounce"))

/**
 * Player information HUD, rendered and positioned by OneConfig.
 *
 * It shows up in OneConfig's HUD editor like any other OneConfig HUD, so it can be moved, scaled,
 * restyled and toggled there. The LiquidBounce `HUD` module still acts as the master switch.
 */
object LbPlayerInfoHud : TextHud(PLAYER_INFO_HUD_ID, "Player Info", LB_HUD_CATEGORY, "") {

    override fun update(): Boolean {
        if (!ModuleHud.enabled) {
            return false
        }
        return super.update()
    }

    public override fun getText(): String? {
        val player = mc.player ?: return null

        val pos = player.blockPosition()
        val health = player.health
        val maxHealth = player.maxHealth
        val healthColor = when {
            health <= maxHealth * 0.25F -> ChatFormatting.RED
            health <= maxHealth * 0.5F -> ChatFormatting.GOLD
            else -> ChatFormatting.GREEN
        }

        return listOf(
            "${ChatFormatting.AQUA}${ChatFormatting.BOLD}${player.name.string}",
            "${ChatFormatting.GRAY}XYZ: ${ChatFormatting.WHITE}${pos.x}, ${pos.y}, ${pos.z}",
            "${ChatFormatting.GRAY}HP: $healthColor${health.toInt()}/${maxHealth.toInt()}",
            "${ChatFormatting.GRAY}Ping: ${ChatFormatting.YELLOW}${player.ping}ms",
        ).joinToString("\n")
    }

}
