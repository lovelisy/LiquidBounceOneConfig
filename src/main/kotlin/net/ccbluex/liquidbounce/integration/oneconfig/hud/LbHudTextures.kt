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

import org.polyfrost.compose.render.PolyBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads the icon textures of the native HUD straight off the classpath.
 *
 * OneConfig's renderer draws bitmaps, not Minecraft texture handles, so every icon is decoded once into a [PolyBitmap]
 * and kept around. Icons never change at runtime, which makes the cache safe to keep for the whole session.
 */
object LbHudTextures {

    private val cache: ConcurrentHashMap<String, PolyBitmap> = ConcurrentHashMap()

    /** Decoded bitmap of the classpath [path] relative to `assets/liquidbounce/`, or `null` when it is missing. */
    fun get(path: String): PolyBitmap? {
        cache[path]?.let { return it }

        val bitmap = runCatching {
            LbHudTextures::class.java.classLoader
                .getResourceAsStream("assets/liquidbounce/$path")
                ?.use { PolyBitmap.decode(it.readBytes()) }
        }.getOrNull() ?: return null

        cache[path] = bitmap
        return bitmap
    }

    /**
     * Decoded bitmap of a Minecraft asset such as `minecraft:textures/mob_effect/speed.png`.
     *
     * Minecraft stores its textures in the resource manager, so the path is resolved as an [Identifier] and read through
     * the active resource packs. Like [get], the result is cached because effect sprites never change at runtime.
     */
    fun minecraft(path: String): PolyBitmap? {
        cache[path]?.let { return it }

        val bitmap = runCatching {
            val id = net.minecraft.resources.Identifier.parse(path)
            val resource = net.ccbluex.liquidbounce.utils.client.mc.resourceManager
                .getResource(id)
            if (!resource.isPresent) return@runCatching null
            resource.get().open().use { PolyBitmap.decode(it.readBytes()) }
        }.getOrNull() ?: return null

        cache[path] = bitmap
        return bitmap
    }

}
