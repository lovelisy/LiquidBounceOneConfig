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

import net.minecraft.world.item.ItemStack
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.RenderContext
import kotlin.math.min

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
    /** Size multiplier of the HUD text, so the item plates follow the same scale as the labels. */
    @JvmField val textScale: Float = 1f,
) {

    fun save() = context.save()

    fun restore() = context.restore()

    fun translate(x: Float, y: Float) = context.translate(x, y)

    fun scale(x: Float, y: Float) = context.scale(x, y)

    /** Runs [block] with its whole output scaled to [factor] opacity. */
    fun alpha(factor: Float, block: () -> Unit) {
        if (factor <= 0f) return
        if (factor >= 1f) {
            block()
            return
        }

        context.saveLayerAlpha(factor)
        try {
            block()
        } finally {
            context.restore()
        }
    }

    fun fill(x: Float, y: Float, w: Float, h: Float, color: PolyColor, radius: Float = 0f) {
        if (w <= 0f || h <= 0f || color.alpha == 0) return
        context.rect(x, y, w, h, color, radius)
    }

    fun fill(x: Float, y: Float, w: Float, h: Float, argb: Int, radius: Float = 0f) =
        fill(x, y, w, h, PolyColor(argb), radius)

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
            context.text(text, dx, dy - font.metrics.ascent, color, font)
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

    /** Width of [text] in [font], usable with or without a render context. */
    fun measure(text: String, font: PolyFont): Float =
        if (text.isEmpty()) 0f else font.measureTextWidth(text)

    /**
     * Fills a rectangle with an independent corner radius on every corner.
     *
     * The native HUD uses this for the accent bar of the array list and for the health bar of the target HUD, where
     * only one side is rounded.
     */
    fun fillCssBox(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
        argb: Int,
    ) {
        if (w <= 0f || h <= 0f || (argb ushr 24) == 0) return

        val maxRadius = min(w, h) * 0.5f
        val tl = topLeft.coerceIn(0f, maxRadius)
        val tr = topRight.coerceIn(0f, maxRadius)
        val br = bottomRight.coerceIn(0f, maxRadius)
        val bl = bottomLeft.coerceIn(0f, maxRadius)

        if (tl == 0f && tr == 0f && br == 0f && bl == 0f) {
            context.rect(x, y, w, h, PolyColor(argb), 0f)
            return
        }

        val path = PolyPath()
        path.moveTo(x + tl, y)
        path.lineTo(x + w - tr, y)
        if (tr > 0f) path.tangentArcTo(x + w, y, x + w, y + tr, tr)
        path.lineTo(x + w, y + h - br)
        if (br > 0f) path.tangentArcTo(x + w, y + h, x + w - br, y + h, br)
        path.lineTo(x + bl, y + h)
        if (bl > 0f) path.tangentArcTo(x, y + h, x, y + h - bl, bl)
        path.lineTo(x, y + tl)
        if (tl > 0f) path.tangentArcTo(x, y, x + tl, y, tl)
        path.close()

        context.fillPath(path, PolyColor(argb))
    }

    /**
     * Fills a rounded ring, the shape the native key display grows while a key is held.
     *
     * The ring is the area between the outer rounded rectangle and the same rectangle inset by [thickness]. It is built
     * as one even-odd path so the hole stays transparent and the plate behind it keeps showing, which a filled
     * rectangle shrinking towards the centre could not do.
     */
    fun fillRing(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        thickness: Float,
        argb: Int,
    ) {
        if (w <= 0f || h <= 0f || (argb ushr 24) == 0) return

        val maxRadius = min(w, h) * 0.5f
        val outerRadius = radius.coerceIn(0f, maxRadius)
        val inset = thickness.coerceIn(0f, maxRadius)
        if (inset <= 0f) return

        val innerW = w - inset * 2f
        val innerH = h - inset * 2f
        if (innerW <= 0f || innerH <= 0f) {
            context.rect(x, y, w, h, PolyColor(argb), outerRadius)
            return
        }

        val innerRadius = (outerRadius - inset).coerceIn(0f, min(innerW, innerH) * 0.5f)

        val path = PolyPath()
        appendRoundedRect(path, x, y, w, h, outerRadius)
        // The hole is traced the other way round, so it stays a hole for a renderer that fills by winding rule, and the
        // even-odd flag covers the ones that fill by crossing count.
        appendRoundedRectReversed(path, x + inset, y + inset, innerW, innerH, innerRadius)
        path.evenOdd = true

        context.fillPath(path, PolyColor(argb))
    }

    /** Appends one closed rounded rectangle to [path], traced clockwise. */
    private fun appendRoundedRect(
        path: PolyPath,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
    ) {
        val maxRadius = min(w, h) * 0.5f
        val r = radius.coerceIn(0f, maxRadius)

        path.moveTo(x + r, y)
        path.lineTo(x + w - r, y)
        if (r > 0f) path.tangentArcTo(x + w, y, x + w, y + r, r)
        path.lineTo(x + w, y + h - r)
        if (r > 0f) path.tangentArcTo(x + w, y + h, x + w - r, y + h, r)
        path.lineTo(x + r, y + h)
        if (r > 0f) path.tangentArcTo(x, y + h, x, y + h - r, r)
        path.lineTo(x, y + r)
        if (r > 0f) path.tangentArcTo(x, y, x + r, y, r)
        path.close()
    }

    /** Appends one closed rounded rectangle to [path], traced counter-clockwise so it punches a hole. */
    private fun appendRoundedRectReversed(
        path: PolyPath,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
    ) {
        val maxRadius = min(w, h) * 0.5f
        val r = radius.coerceIn(0f, maxRadius)

        path.moveTo(x + r, y + h)
        path.lineTo(x + w - r, y + h)
        if (r > 0f) path.tangentArcTo(x + w, y + h, x + w, y + h - r, r)
        path.lineTo(x + w, y + r)
        if (r > 0f) path.tangentArcTo(x + w, y, x + w - r, y, r)
        path.lineTo(x + r, y)
        if (r > 0f) path.tangentArcTo(x, y, x, y + r, r)
        path.lineTo(x, y + h - r)
        if (r > 0f) path.tangentArcTo(x, y + h, x + r, y + h, r)
        path.close()
    }

    /**
     * Draws an item stack with its three dimensional model, scaled into the given box.
     *
     * [slot] identifies the position the stack is drawn at and must stay the same while the element lives, for example
     * `inventory:14`. OneConfig samples the icons from an atlas rendered on a frame hook, so the handle has to be reused
     * across frames; see [LbItemIcons].
     */
    fun item(stack: ItemStack, slot: String, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f) {
        LbItemIcons.draw(context, slot, stack, x, y, w, h, alpha)
    }

    /**
     * Draws an item stack together with its decorations.
     *
     * The stack count is printed through OneConfig's text engine in the corner the vanilla inventory uses, so the
     * numbers of the inventory grids follow the same font as every other label of the client instead of the game font.
     * The durability bar and the cooldown veil stay native. [countLabel] overrides the number, so an element can print
     * a total that differs from the stack size, such as the number of matching items spread over the whole inventory;
     * an empty label leaves the corner blank.
     */
    fun itemWithDecorations(
        stack: ItemStack,
        slot: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        countLabel: String? = null,
    ) {
        if (stack.isEmpty) {
            return
        }

        LbItemIcons.draw(context, slot, stack, x, y, w, h, 1f, countLabel, ownCount = true)

        val label = countLabel ?: stack.count.let { if (it <= 1) null else it.toString() } ?: return
        if (label.isEmpty()) {
            return
        }

        val font = LbHudFonts.count(h, textScale)
        val textWidth = measure(label, font)
        val pad = h * COUNT_PAD_FACTOR
        context.text(
            label,
            x + w - textWidth - pad,
            y + h - font.metrics.descent - pad,
            PolyColor(-1),
            font,
        )
    }

    /** Draws the icon at the classpath [path] aspect fitted and centred inside the given box. */
    fun icon(path: String, x: Float, y: Float, boxW: Float, boxH: Float, argb: Int = -1) {
        if (boxW <= 0f || boxH <= 0f || (argb ushr 24) == 0) return

        val bitmap = LbHudTextures.get(path) ?: return
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val drawW: Float
        val drawH: Float
        if (boxW / boxH > aspect) {
            drawH = boxH
            drawW = boxH * aspect
        } else {
            drawW = boxW
            drawH = boxW / aspect
        }

        val centerX = x + boxW * 0.5f
        val centerY = y + boxH * 0.5f
        context.image(
            bitmap,
            centerX - drawW * 0.5f,
            centerY - drawH * 0.5f,
            drawW,
            drawH,
            0f,
            0f,
            1f,
            1f,
            PolyColor(argb),
            FilterQuality.Medium,
        )
    }

    /** Draws the classpath [path] stretched to the whole box, for artwork that must not keep its aspect ratio. */
    fun stretchedIcon(path: String, x: Float, y: Float, w: Float, h: Float, argb: Int = -1) {
        if (w <= 0f || h <= 0f || (argb ushr 24) == 0) return

        val bitmap = LbHudTextures.get(path) ?: return
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        context.image(bitmap, x, y, w, h, 0f, 0f, 1f, 1f, PolyColor(argb), FilterQuality.Medium)
    }

    /**
     * Draws a rounded cut-out of [bitmap].
     *
     * The texture coordinates are expressed in pixels of the bitmap and normalized internally, because the renderer
     * samples the whole bitmap with the unit square. [quality] defaults to smooth filtering for the downscaled icons;
     * pixel art such as the player face has to pass [FilterQuality.None] to stay crisp.
     */
    fun roundedTexture(
        bitmap: PolyBitmap,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        u0: Float,
        v0: Float,
        u1: Float,
        v1: Float,
        argb: Int = -1,
        quality: FilterQuality = FilterQuality.Medium,
    ) {
        if (w <= 0f || h <= 0f || (argb ushr 24) == 0) return
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        val uMax = bitmap.width.toFloat()
        val vMax = bitmap.height.toFloat()
        context.save()
        try {
            context.clipRRect(x, y, w, h, radius.coerceIn(0f, min(w, h) * 0.5f))
            context.image(
                bitmap,
                x,
                y,
                w,
                h,
                u0 / uMax,
                v0 / vMax,
                u1 / uMax,
                v1 / vMax,
                PolyColor(argb),
                quality,
            )
        } finally {
            context.restore()
        }
    }

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

    companion object {
        /** The font key PolyCompose maps onto the game's own font, so text keeps the vanilla look and glyph coverage. */
        const val GAME_FONT: String = "polycompose:minecraft"

        /** Bold variant of [GAME_FONT]. */
        const val GAME_FONT_BOLD: String = "polycompose:minecraft-bold"

        /** Size of one game font glyph cell, in logical units. */
        const val GAME_FONT_BASE_SIZE: Float = 8f

        /** Inset of the stack count from the corner of the icon, as a share of the icon box. */
        private const val COUNT_PAD_FACTOR: Float = 0.08f
    }

}
