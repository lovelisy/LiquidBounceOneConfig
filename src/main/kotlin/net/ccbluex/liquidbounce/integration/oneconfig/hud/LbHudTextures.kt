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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.resources.Identifier
import org.polyfrost.compose.render.PolyBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

/**
 * Loads the icon textures of the native HUD out of the client's resource packs.
 *
 * OneConfig's renderer draws bitmaps, not Minecraft texture handles, so every icon identifier is decoded once into a
 * [PolyBitmap] and kept around. Icons never change at runtime, which makes the cache safe to keep for the whole session.
 */
object LbHudTextures {

    private val cache: ConcurrentHashMap<Identifier, PolyBitmap> = ConcurrentHashMap()

    /** Decoded bitmap of [id], or `null` when the resource pack does not contain it. */
    fun get(id: Identifier): PolyBitmap? {
        cache[id]?.let { return it }

        val bitmap = runCatching {
            val resource = mc.resourceManager.getResource(id).getOrNull() ?: return null
            resource.open().use { PolyBitmap.decode(it.readBytes()) }
        }.getOrNull() ?: return null

        cache[id] = bitmap
        return bitmap
    }

}
