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

import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.polyfrost.compose.minecraft.render.blaze.BlazeRenderer
import org.polyfrost.compose.minecraft.render.blaze.GlyphAtlas
import org.polyfrost.compose.minecraft.render.blaze.StackingGuiRenderState
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud

/**
 * Base class of every LiquidBounce HUD element.
 *
 * OneConfig owns the placement, the scale, the toggle key and the per element settings panel; the element only has to
 * report its natural size and paint itself into an [LbHudCanvas].
 *
 * The paint pass goes through PolyCompose's immediate mode renderer, so text and shapes use OneConfig's font engine
 * instead of a font of its own. A frame is recorded into a fresh [BlazeRenderer], the recorded operations are handed to
 * the client's GUI render state and replayed by [GuiGraphicsExtractor].
 */
abstract class LbHud(
    id: String,
    title: String,
    category: Hud.Category,
) : LegacyHud(id, title, category) {

    private var lastFrameNanos: Long = 0L

    private var measuredWidth: Float = 0f
    private var measuredHeight: Float = 0f

    /**
     * Fallback size used before the first paint pass.
     *
     * OneConfig asks for the minimum size *before* it renders the element for the first time, so a zero size would
     * make the very first frame invisible and misplaced until the next frame.
     */
    protected open fun defaultSize(): HudSize = HudSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)

    final override fun getWidth(): Float = measuredWidth.takeIf { it > 0f } ?: defaultSize().width

    final override fun getHeight(): Float = measuredHeight.takeIf { it > 0f } ?: defaultSize().height

    /** Paints the element at HUD local origin `(0, 0)` and returns the size it occupied. */
    protected abstract fun paint(canvas: LbHudCanvas): HudSize

    /** OneConfig polls the element on a timer, but the element is redrawn every frame anyway. */
    override fun update(): Boolean = true

    /** HUD content follows the world state, so a cached frame would go stale right away. */
    override fun getAlwaysRedraw(): Boolean = true

    final override fun render(ctx: GuiGraphicsExtractor) {
        val delta = deltaSeconds()
        val renderer = BlazeRenderer(mc.window.guiScale.toFloat(), false)

        val operations = try {
            val size = paint(
                LbHudCanvas(
                    context = RenderContext(renderer),
                    width = mc.window.guiScaledWidth.toFloat(),
                    height = mc.window.guiScaledHeight.toFloat(),
                    deltaSeconds = delta,
                    inEditor = HudManager.isEditing,
                ),
            )
            measuredWidth = size.width.coerceAtLeast(0f)
            measuredHeight = size.height.coerceAtLeast(0f)
            renderer.finish()
        } catch (throwable: Throwable) {
            // A broken element must not take the whole HUD pass down.
            logger.error("Unable to paint the '$title' HUD element", throwable)
            return
        }

        GlyphAtlas.uploadDirty()
        replay(ctx, operations)
    }

    /** Hands the recorded operations to the GUI render state, so they are drawn after the current layer. */
    private fun replay(ctx: GuiGraphicsExtractor, operations: List<(GuiGraphicsExtractor) -> Unit>) {
        val state = ctx.guiRenderState
        val stacking = state as? StackingGuiRenderState

        stacking?.polycompose$setStacking(true)
        try {
            operations.forEach { operation -> operation(ctx) }
        } finally {
            stacking?.polycompose$setStacking(false)
        }
    }

    /**
     * Routes this element's option tree into LiquidBounce's own configuration folder instead of OneConfig's profile
     * directory, so HUD settings keep living next to all other LiquidBounce settings.
     */
    override fun addToSerialized(tree: Tree) {
        LbHudStore.attach(this, tree)
    }

    override fun shouldShow(): Boolean = super.shouldShow() && inGame

    /** Seconds since the previous frame, clamped so a stall cannot blow up the animation maths. */
    private fun deltaSeconds(): Float {
        val now = System.nanoTime()
        val previous = lastFrameNanos
        lastFrameNanos = now
        if (previous == 0L) return 0f
        return ((now - previous) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.25f)
    }

    private companion object {
        const val DEFAULT_WIDTH: Float = 60f
        const val DEFAULT_HEIGHT: Float = 12f
    }

}
