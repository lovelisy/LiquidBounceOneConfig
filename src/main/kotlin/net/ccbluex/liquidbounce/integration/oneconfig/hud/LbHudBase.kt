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

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Option set shared by every LiquidBounce HUD element.
 *
 * The properties are plain annotated fields, so OneConfig reflects them into the element's settings panel and
 * [LbHudStore] persists their values into `LiquidBounce/configs/hud`.
 */
abstract class LbHudBase(id: String, title: String, category: Hud.Category) : LbHud(id, title, category) {

    @Switch(title = "Show Background")
    @JvmField
    var background: Boolean = true

    @Color(title = "Background Color", alpha = true)
    @JvmField
    var backgroundColor: PolyColor = PolyColor(0x80000000.toInt())

    @Slider(title = "Corner Radius", min = 0f, max = 12f, step = 0.5f)
    @JvmField
    var cornerRadius: Float = 3f

    @Slider(title = "Padding", min = 0f, max = 8f, step = 0.5f)
    @JvmField
    var padding: Float = 2f

    @Text(title = "Font")
    @JvmField
    var fontName: String = LbHudCanvas.DEFAULT_FONT

    @Slider(title = "Font Size", min = 6f, max = 32f, step = 0.5f)
    @JvmField
    var fontSize: Float = LbHudCanvas.VANILLA_FONT_SIZE

    @Switch(title = "Text Shadow")
    @JvmField
    var textShadow: Boolean = true

    /** Sizes the frame around [contentWidth] by [contentHeight] plus the padding, then paints the background. */
    protected fun backdrop(canvas: LbHudCanvas, contentWidth: Float, contentHeight: Float): HudSize {
        val width = contentWidth + padding * 2f
        val height = contentHeight + padding * 2f

        if (background) {
            canvas.fill(0f, 0f, width, height, backgroundColor, cornerRadius)
        }

        return HudSize(width, height)
    }

    /** Shifts the canvas to the inner box, so the element can draw at `(0, 0)` ignoring the padding. */
    protected fun content(canvas: LbHudCanvas): LbHudCanvas {
        if (padding > 0f) {
            canvas.translate(padding, padding)
        }

        return canvas
    }

    /** Draws [label] with the element's font, honouring the shadow switch. */
    protected fun drawLabel(
        canvas: LbHudCanvas,
        label: String,
        x: Float,
        y: Float,
        color: PolyColor,
        font: org.polyfrost.compose.render.PolyFont,
    ) {
        if (textShadow) {
            canvas.textShadow(label, x, y, font, color.argb)
        } else {
            canvas.text(label, x, y, font, color)
        }
    }

}
