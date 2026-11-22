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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Lists every running module, one per line.
 */
object ArrayListHud : LbHudBase("arraylist", "Array List", Hud.Category.INFO) {

    @Dropdown(title = "Sort By", options = ["Length", "Alphabetical"])
    @JvmField
    var sortMode: String = "Length"

    @Switch(title = "Show Category")
    @JvmField
    var showCategory: Boolean = false

    @Color(title = "Text Color", alpha = true)
    @JvmField
    var color: PolyColor = PolyColor(0xFFFFFFFF.toInt())

    @Switch(title = "Rainbow")
    @JvmField
    var rainbow: Boolean = false

    @Slider(title = "Rainbow Speed", min = 1f, max = 20f, step = 1f)
    @JvmField
    var rainbowSpeed: Float = 6f

    @Switch(title = "Right Aligned")
    @JvmField
    var rightAligned: Boolean = false

    @Slider(title = "Line Spacing", min = 0f, max = 8f, step = 0.5f)
    @JvmField
    var lineSpacing: Float = 0f

    override fun paint(canvas: LbHudCanvas): HudSize {
        val modules = runningModules()
        if (modules.isEmpty()) {
            return HudSize(0f, 0f)
        }

        val font = canvas.font(fontSize, fontName)
        val lineHeight = canvas.lineHeight(font) + lineSpacing
        val labels = modules.map(::labelFor)

        val contentWidth = labels.maxOf { canvas.measure(it, font) }
        val contentHeight = lineHeight * labels.size

        val size = backdrop(canvas, contentWidth, contentHeight)
        val inner = content(canvas)
        val textX = if (rightAligned) contentWidth else 0f

        labels.forEachIndexed { index, label ->
            val painted = if (rainbow) canvas.rainbow(rainbowSpeed) else color
            drawLabel(inner, label, textX, index * lineHeight, painted, font)
        }

        return size
    }

    private fun runningModules(): List<ClientModule> = when (sortMode) {
        "Alphabetical" -> ModuleManager.filter(::isListed).sortedBy { it.name.lowercase() }
        else -> ModuleManager.filter(::isListed).sortedByDescending { it.name.length }
    }

    private fun isListed(module: ClientModule): Boolean = module.enabled && !module.hidden

    private fun labelFor(module: ClientModule): String =
        if (showCategory) "${module.category.tag} · ${module.name}" else module.name

}
