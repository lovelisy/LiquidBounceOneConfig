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

import androidx.compose.runtime.Composable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import org.polyfrost.compose.composables.NodeCanvas
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * Base class of every LiquidBounce HUD element.
 *
 * OneConfig owns the placement, the scale, the toggle key and the per element settings panel; the element only has to
 * describe its size and its content. The shared look, meaning the background, the border, the padding, the font and the
 * text colour, lives on [Hud] itself and is edited in the very same panel.
 *
 * Content is drawn through a Compose canvas, so text and shapes go through OneConfig's font engine and PolyCompose's
 * renderer. Nothing here measures or rasterises a font on its own.
 */
abstract class LbHud(
    id: String,
    title: String,
    category: Hud.Category,
) : Hud(id, title, category), EventListener {

    private var lastFrameNanos: Long = 0L

    /**
     * Computes the size the content occupies, excluding the padding around it.
     *
     * Called from the update pass and from the Compose pass, so it has to be cheap and free of side effects: only
     * measure text, never draw it.
     */
    protected abstract fun measure(): HudSize

    /** Paints the content, with the origin on the top left corner of the padded area. */
    protected abstract fun paint(canvas: LbHudCanvas)

    init {
        staticW = DEFAULT_WIDTH
        staticH = DEFAULT_HEIGHT

        // The native elements paint their own card surface, so OneConfig must not draw a second background behind it.
        showBackground = false
    }

    /**
     * OneConfig asks the element how often it wants to be updated. The content is repainted every frame from the live
     * game state, so the update pass only exists to keep the reported size in sync with the content.
     *
     * The size is not measured during registration: measuring text needs the game's font pipeline, which is only
     * available once the client is running.
     */
    override fun updateFrequency(): Long = UPDATE_INTERVAL_NANOS

    override fun update(): Boolean {
        syncSize()
        return true
    }

    /** Recomputes the reported size, asking OneConfig for a new layout pass when it changed. */
    private fun syncSize() {
        val content = measure()
        val width = (content.width + padLeft + padRight).coerceAtLeast(MIN_EXTENT)
        val height = (content.height + padTop + padBottom).coerceAtLeast(MIN_EXTENT)

        if (width == staticW && height == staticH) {
            return
        }

        staticW = width
        staticH = height
        contentChanged()
    }

    /** The content is drawn every frame from the live game state, so a cached frame must never be reused. */
    override val alwaysRedraw: Boolean
        get() = true

    @Composable
    override fun Content() {
        NodeCanvas(
            hudBackground()
                .size(staticW, staticH)
                .padding(padLeft, padTop, padRight, padBottom),
            live = true,
        ) { x, y, width, height ->
            paintSafely(this, x, y, width, height)
        }
    }

    /** Runs the paint pass, logging instead of throwing so one broken element cannot take the HUD down. */
    private fun paintSafely(context: RenderContext, x: Float, y: Float, width: Float, height: Float) {
        context.save()
        context.translate(x, y)

        try {
            paint(
                LbHudCanvas(
                    context = context,
                    width = width,
                    height = height,
                    deltaSeconds = deltaSeconds(),
                    inEditor = HudManager.isEditing,
                ),
            )
        } catch (throwable: Throwable) {
            logger.error("Unable to paint the '$title' HUD element", throwable)
        } finally {
            context.restore()
        }
    }

    /** Asks OneConfig to re-run the Compose pass, used when an option of this element changed. */
    fun invalidate() {
        contentChanged()
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
        /** How often OneConfig polls the element, in nanoseconds. */
        const val UPDATE_INTERVAL_NANOS: Long = 50_000_000L

        /** Smallest extent an element may report, so an empty element stays snappable in the editor. */
        const val MIN_EXTENT: Float = 1f

        /** Size reported for the first layout pass, before the content has been measured once. */
        const val DEFAULT_WIDTH: Float = 60f
        const val DEFAULT_HEIGHT: Float = 12f
    }

}
