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
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Lists every enabled module, one per line.
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

    private fun modules(): List<ClientModule> {
        val enabled = ModuleManager.filter { module -> module.enabled && !module.hidden }
        return when (sortMode) {
            "Alphabetical" -> enabled.sortedBy { it.name.lowercase() }
            else -> enabled.sortedByDescending { it.name.length }
        }
    }

    private fun labels(): List<String> = modules().map { module ->
        val tag = module.category.tag
        if (!showCategory || tag == null) module.name else "$tag · ${module.name}"
    }

    override fun measure(): HudSize {
        val font = hudFont()
        val labels = labels()
        val width = labels.maxOfOrNull { textWidth(it, font) } ?: textWidth(PLACEHOLDER, font)
        val height = lineHeight(font) * labels.size.coerceAtLeast(1)
        return HudSize(width, height)
    }

    override fun paint(canvas: LbHudCanvas) {
        val font = hudFont()
        val step = lineHeight(font) + lineSpacing
        val labels = labels()
        val painted = if (rainbow) canvas.rainbow(rainbowSpeed) else color
        val contentWidth = labels.maxOfOrNull { canvas.measure(it, font) } ?: 0f

        labels.forEachIndexed { index, label ->
            val x = if (rightAligned) contentWidth - canvas.measure(label, font) else 0f
            drawLabel(canvas, label, x, index * step, painted, font)
        }
    }

    private fun drawLabel(
        canvas: LbHudCanvas,
        label: String,
        x: Float,
        y: Float,
        color: PolyColor,
        font: PolyFont,
    ) {
        drawText(canvas, label, x, y, color, font)
    }

    private const val PLACEHOLDER = "Array List"

}
