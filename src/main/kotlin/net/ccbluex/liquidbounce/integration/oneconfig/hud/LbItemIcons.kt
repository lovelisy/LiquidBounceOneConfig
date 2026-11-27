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
import org.polyfrost.compose.minecraft.mc.McItemIconHandle
import org.polyfrost.compose.minecraft.mc.McItemIcons
import org.polyfrost.compose.render.RenderContext
import kotlin.math.min

/**
 * Item icons of the native HUD elements.
 *
 * OneConfig does not draw an item stack on the spot: it renders the icons into a texture atlas on a frame hook over
 * every handle that is currently open, and the draw call only samples that atlas. A handle therefore has to stay open
 * across frames, which `McItemIcons.open(stack).use { ... }` cannot do, because it closes right after the draw and the
 * atlas is empty by the time it renders.
 *
 * The cache keeps one handle per slot. The keys are positional (`inventory:3`, `armor_items:head`), so the number of
 * open handles equals the number of slots an element draws and never grows with the items flowing through them.
 */
internal object LbItemIcons {

    private class Entry(var handle: McItemIconHandle?, var width: Float = -1f, var height: Float = -1f)

    private val entries = HashMap<String, Entry>()

    /**
     * Draws [stack] into the given box and keeps its handle alive for the next frames.
     *
     * [key] has to be stable for a slot across frames, not derived from the stack, or every stack change would leak a
     * handle into the atlas.
     */
    fun draw(
        context: RenderContext,
        key: String,
        stack: ItemStack,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        alpha: Float = 1f,
        countLabel: String? = null,
    ) {
        if (w <= 0f || h <= 0f || alpha <= 0f) {
            return
        }

        val entry = entry(key, stack) ?: return
        val handle = entry.handle ?: return

        runCatching {
            // The atlas reserves a page per handle, so the base size is only reported when the slot really changed its
            // extent; doing it on every frame would keep re-requesting the same page.
            if (entry.width != w || entry.height != h) {
                handle.setBaseSize(w, h)
                entry.width = w
                entry.height = h
            }

            handle.draw(context, x, y, w, h, alpha.coerceIn(0f, 1f))

            if (countLabel != null) {
                handle.drawDecorations(context, x, y, min(w, h), countLabel)
            } else {
                handle.drawDecorations(context, x, y, min(w, h))
            }
        }
    }

    /** Releases every handle of the given [prefix], used when an element stops drawing a slot. */
    fun release(prefix: String) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.key.startsWith(prefix)) {
                continue
            }

            runCatching { entry.value.handle?.close() }
            iterator.remove()
        }
    }

    private fun entry(key: String, stack: ItemStack): Entry? {
        val cached = entries[key]
        val handle = cached?.handle
        if (cached != null && handle != null && !handle.isClosed) {
            runCatching { handle.update(stack) }
            return cached
        }

        val fresh = runCatching { McItemIcons.open(stack) }.getOrNull() ?: return null
        return Entry(fresh).also { entries[key] = it }
    }

}
