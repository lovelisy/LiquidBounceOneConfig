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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.polyfrost.compose.render.PolyColor
import java.lang.reflect.Type

/**
 * JSON codec for the option values of a HUD element.
 *
 * The values of an OneConfig option are plain Kotlin and Java types plus [PolyColor], so a small dedicated Gson
 * instance is enough and keeps LiquidBounce's HUD files free of any OneConfig internal representation.
 */
internal object LbHudJson {

    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(PolyColor::class.java, PolyColorAdapter)
        .create()

    /** Writes the visible state of [color], so chroma and gradients survive a reload. */
    private object PolyColorAdapter : JsonSerializer<PolyColor>, JsonDeserializer<PolyColor> {

        override fun serialize(color: PolyColor, type: Type, context: JsonSerializationContext): JsonElement =
            JsonObject().apply {
                addProperty("argb", color.argb)
                if (color.chroma) {
                    addProperty("chroma", true)
                    addProperty("chromaSpeed", color.chromaSpeed)
                }
                if (color.gradientEnabled) {
                    addProperty("gradient", true)
                    addProperty("gradientArgb", color.gradientArgb)
                }
            }

        override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): PolyColor {
            if (json.isJsonPrimitive) {
                return PolyColor(json.asInt)
            }

            val obj = json.asJsonObject
            val argb = obj.get("argb")?.asInt ?: 0xFFFFFFFF.toInt()
            val color = PolyColor(argb)
            if (obj.get("chroma")?.asBoolean == true) {
                color.chroma = true
                color.chromaSpeed = obj.get("chromaSpeed")?.asFloat ?: 1f
            }
            if (obj.get("gradient")?.asBoolean == true) {
                color.gradientEnabled = true
                color.gradientArgb = obj.get("gradientArgb")?.asInt ?: argb
            }
            return color
        }
    }

    /** Reads a stored value back into the type the option expects. */
    fun read(element: JsonElement, type: Class<*>): Any = when {
        type == Boolean::class.javaObjectType || type == Boolean::class.javaPrimitiveType ->
            element.asBoolean

        type == Int::class.javaObjectType || type == Int::class.javaPrimitiveType ->
            element.asInt

        type == Long::class.javaObjectType || type == Long::class.javaPrimitiveType ->
            element.asLong

        type == Float::class.javaObjectType || type == Float::class.javaPrimitiveType ->
            element.asFloat

        type == Double::class.javaObjectType || type == Double::class.javaPrimitiveType ->
            element.asDouble

        type == String::class.java ->
            element.asString

        else ->
            gson.fromJson(element, type)
    }

    /** Encodes [value] so [read] can decode it again. */
    fun write(value: Any): JsonElement = when (value) {
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Char -> JsonPrimitive(value)
        is Enum<*> -> JsonPrimitive(value.name)
        is Collection<*> -> JsonArray().apply { value.forEach { item -> add(item?.let(::write)) } }
        else -> gson.toJsonTree(value)
    }

}
