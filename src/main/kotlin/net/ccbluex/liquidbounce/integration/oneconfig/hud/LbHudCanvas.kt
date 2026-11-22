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

import net.minecraft.network.chat.Component
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyShadow
import org.polyfrost.compose.render.RenderContext

/**
 * Drawing surface handed to a LiquidBounce HUD element.
 *
 * Wraps OneConfig's immediate mode [RenderContext] so HUD code never touches a font loader, a glyph atlas or a shader
 * of its own: text is laid out by OneConfig's [PolyFont], shapes by its renderer.
 *
 * @param context OneConfig render context, already translated to the origin of the content area.
 * @param width width of the content area, in the same units as the drawing calls.
 * @param height height of the content area.
 * @param deltaSeconds seconds elapsed since the previous frame.
 * @param inEditor whether the OneConfig HUD editor is currently open.
 */
class LbHudCanvas internal constructor(
    @JvmField internal val context: RenderContext,
    @JvmField val width: Float,
    @JvmField val height: Float,
    @JvmField val deltaSeconds: Float = 0f,
    @JvmField val inEditor: Boolean = false,
) {

    fun save() = context.save()

    fun restore() = context.restore()

    fun translate(x: Float, y: Float) = context.translate(x, y)

    fun scale(x: Float, y: Float) = context.scale(x, y)

    fun clip(x: Float, y: Float, w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        context.clipRect(x, y, x + w, y + h)
    }

    fun fill(x: Float, y: Float, w: Float, h: Float, color: PolyColor, radius: Float = 0f) {
        if (w <= 0f || h <= 0f || color.alpha == 0) return
        context.rect(x, y, w, h, color, radius)
    }

    fun fill(x: Float, y: Float, w: Float, h: Float, argb: Int, radius: Float = 0f) =
        fill(x, y, w, h, PolyColor(argb), radius)

    fun stroke(
        x: Float, y: Float, w: Float, h: Float,
        color: PolyColor,
        radius: Float = 0f,
        thickness: Float = 1f,
    ) {
        if (w <= 0f || h <= 0f || thickness <= 0f || color.alpha == 0) return
        context.rectStroke(x, y, w, h, color, radius, thickness)
    }

    fun stroke(
        x: Float, y: Float, w: Float, h: Float,
        argb: Int,
        radius: Float = 0f,
        thickness: Float = 1f,
    ) = stroke(x, y, w, h, PolyColor(argb), radius, thickness)

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, color: PolyColor, thickness: Float = 1f) {
        if (thickness <= 0f || color.alpha == 0) return
        context.line(x0, y0, x1, y1, color, thickness)
    }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, argb: Int, thickness: Float = 1f) =
        line(x0, y0, x1, y1, PolyColor(argb), thickness)

    fun shadow(
        x: Float, y: Float, w: Float, h: Float,
        color: PolyColor,
        radius: Float = 0f,
        blur: Float = 6f,
    ) {
        if (w <= 0f || h <= 0f || color.alpha == 0 || blur <= 0f) return
        context.shadow(x, y, w, h, radius, PolyShadow(color, blur, 0f, 0f, 0f))
    }

    fun shadow(
        x: Float, y: Float, w: Float, h: Float,
        argb: Int,
        radius: Float = 0f,
        blur: Float = 6f,
    ) = shadow(x, y, w, h, PolyColor(argb), radius, blur)

    fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        context.image(bitmap, x, y, w, h, FilterQuality.Medium)
    }

    fun image(
        bitmap: PolyBitmap,
        u0: Float, v0: Float, u1: Float, v1: Float,
        x: Float, y: Float, w: Float, h: Float,
        tint: PolyColor = PolyColor(0xFFFFFFFF.toInt()),
    ) {
        if (w <= 0f || h <= 0f) return
        context.image(bitmap, x, y, w, h, u0, v0, u1, v1, tint, FilterQuality.Medium)
    }

    fun text(
        text: String,
        x: Float,
        y: Float,
        font: PolyFont,
        color: PolyColor,
        align: PolyAlign = PolyAlign.TopLeft,
    ): Float {
        if (text.isEmpty() || color.alpha == 0) return 0f
        val w = measure(text, font)
        val dx = alignedX(x, w, align)
        val dy = alignedY(y, font.size, align)

        if (isGameFont(font)) {
            context.mcText(text, dx, dy, color.argb, false, gameFontScale(font))
        } else {
            context.text(text, dx, dy, color, font)
        }

        return w
    }

    fun text(
        text: String,
        x: Float,
        y: Float,
        font: PolyFont,
        argb: Int,
        align: PolyAlign = PolyAlign.TopLeft,
    ): Float {
        if ((argb ushr 24) == 0) return 0f
        return text(text, x, y, font, PolyColor(argb), align)
    }

    /**
     * Draws [text] with a one pixel drop shadow.
     *
     * The game font has its own shadow pass, which is what the vanilla interface uses; every other font gets the same
     * text painted once at an offset in the shadow colour before the real text is drawn on top.
     */
    fun textShadow(
        text: String,
        x: Float,
        y: Float,
        font: PolyFont,
        argb: Int,
        align: PolyAlign = PolyAlign.TopLeft,
    ): Float {
        if (text.isEmpty() || (argb ushr 24) == 0) return 0f
        val w = measure(text, font)
        val dx = alignedX(x, w, align)
        val dy = alignedY(y, font.size, align)

        if (isGameFont(font)) {
            context.mcText(text, dx, dy, argb, true, gameFontScale(font))
            return w
        }

        context.text(text, dx + SHADOW_OFFSET, dy + SHADOW_OFFSET, PolyColor(SHADOW_ARGB), font)
        context.text(text, dx, dy, PolyColor(argb), font)
        return w
    }

    /** Draws [text] with an outline in [outline], used where a shadow is not readable enough. */
    fun textOutline(
        text: String,
        x: Float,
        y: Float,
        font: PolyFont,
        color: PolyColor,
        outline: Int,
        thickness: Float = 1f,
    ): Float {
        if (text.isEmpty()) return 0f
        val w = measure(text, font)
        val dx = alignedX(x, w, PolyAlign.TopLeft)
        val outlineColor = if ((outline ushr 24) == 0) PolyColor(0xFF000000.toInt()) else PolyColor(outline)

        if (isGameFont(font)) {
            context.mcTextOutline(text, dx, y, color.argb, outlineColor.argb, gameFontScale(font))
            return w
        }

        context.textOutline(text, dx, y, outlineColor, font, thickness)
        context.text(text, dx, y, color, font)
        return w
    }

    fun textComponent(
        component: Component,
        x: Float,
        y: Float,
        font: PolyFont,
        argb: Int,
        shadow: Boolean = true,
    ): Float {
        val text = component.string
        if (text.isEmpty() || (argb ushr 24) == 0) return 0f
        return if (shadow) textShadow(text, x, y, font, argb) else text(text, x, y, font, argb)
    }

    /** Width of [text] in [font], usable with or without a render context. */
    fun measure(text: String, font: PolyFont): Float =
        if (text.isEmpty()) 0f else font.measureTextWidth(text)

    fun measure(component: Component, font: PolyFont): Float = measure(component.string, font)

    /** Line height of [font], based on the metrics the font engine reports. */
    fun lineHeight(font: PolyFont): Float {
        val metrics = font.metrics
        val extent = kotlin.math.abs(metrics.ascent) + kotlin.math.abs(metrics.descent) + metrics.leading
        return extent.coerceAtLeast(font.size)
    }

    /** A colour cycling through the hue wheel, used by the rainbow options. */
    fun rainbow(speed: Float): PolyColor {
        val cycle = (System.currentTimeMillis() % 100_000L) / 100_000f * speed
        val hue = (cycle % 1f) * 360f
        return PolyColor(hsvToArgb(hue, 0.65f, 1f))
    }

    fun font(size: Float, name: String = GAME_FONT): PolyFont = PolyFont(name, size)

    private fun isGameFont(font: PolyFont): Boolean =
        font.name == GAME_FONT || font.name == GAME_FONT_BOLD

    /** The game font renders on an eight pixel grid, so its size is expressed as a multiple of that grid. */
    private fun gameFontScale(font: PolyFont): Float = font.size / GAME_FONT_BASE_SIZE

    private fun alignedX(x: Float, width: Float, align: PolyAlign): Float = when (align) {
        PolyAlign.TopRight, PolyAlign.Right, PolyAlign.BottomRight -> x - width
        PolyAlign.Top, PolyAlign.Center, PolyAlign.Bottom -> x - width * 0.5f
        else -> x
    }

    private fun alignedY(y: Float, height: Float, align: PolyAlign): Float = when (align) {
        PolyAlign.BottomLeft, PolyAlign.Bottom, PolyAlign.BottomRight -> y - height
        PolyAlign.Left, PolyAlign.Center, PolyAlign.Right -> y - height * 0.5f
        else -> y
    }

    private fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val sector = (hue / 60f)
        val index = sector.toInt() % 6
        val fraction = sector - sector.toInt()

        val p = value * (1f - saturation)
        val q = value * (1f - saturation * fraction)
        val t = value * (1f - saturation * (1f - fraction))

        val (r, g, b) = when (index) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }

        return (0xFF shl 24) or
            ((r * 255f).toInt() shl 16) or
            ((g * 255f).toInt() shl 8) or
            (b * 255f).toInt()
    }

    companion object {
        /** The font key PolyCompose maps onto the game's own font, so text keeps the vanilla look and glyph coverage. */
        const val GAME_FONT: String = "polycompose:minecraft"

        /** Bold variant of [GAME_FONT]. */
        const val GAME_FONT_BOLD: String = "polycompose:minecraft-bold"

        /** Size of one game font glyph cell, in logical units. */
        const val GAME_FONT_BASE_SIZE: Float = 8f

        /** Smallest font size an element is allowed to ask for, so a stray option value cannot vanish the text. */
        const val MIN_FONT_SIZE: Float = 4f

        /** Offset and colour of the drop shadow used by [textShadow] for fonts without a shadow pass of their own. */
        private const val SHADOW_OFFSET: Float = 1f
        private const val SHADOW_ARGB: Int = 0x80000000.toInt()
    }

}
