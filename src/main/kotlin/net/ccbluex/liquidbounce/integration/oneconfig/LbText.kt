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
package net.ccbluex.liquidbounce.integration.oneconfig

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.lang.LanguageManager

/**
 * Resolves every text the OneConfig page shows through the LiquidBounce language files, so the page follows
 * the in-game language exactly like the rest of the client.
 *
 * Module and setting names use `liquidbounce.module.<name>.name`, `liquidbounce.setting.<name>.name` and
 * `liquidbounce.category.<name>.name`, which is what the language files provide.
 */
internal object LbText {

    private const val MODULE_PREFIX = "liquidbounce.module"
    private const val SETTING_PREFIX = "liquidbounce.setting"
    private const val CATEGORY_PREFIX = "liquidbounce.category"
    private const val TAG_PREFIX = "liquidbounce.tag"

    fun trOrNull(key: String?): String? {
        if (key.isNullOrBlank()) {
            return null
        }

        return LanguageManager.getLanguage()?.getOrDefault(key, "")?.takeIf { it.isNotBlank() }
    }

    fun moduleName(name: String): String =
        trOrNull("$MODULE_PREFIX.${lowerFirst(name)}.name") ?: name

    fun moduleDescription(descriptionKey: String?): String? = trOrNull(descriptionKey)

    fun valueName(value: Value<*>): String {
        val key = value.key
        if (key != null) {
            trOrNull("$key.name")?.let { return it }
        }

        return trOrNull("$SETTING_PREFIX.${lowerFirst(value.name)}.name") ?: value.name
    }

    fun categoryName(tag: String): String =
        trOrNull("$CATEGORY_PREFIX.${lowerFirst(tag)}.name") ?: tag

    fun tagName(tag: String): String =
        trOrNull("$TAG_PREFIX.${lowerFirst(tag)}.name")
            ?: trOrNull("$SETTING_PREFIX.${lowerFirst(tag)}.name")
            ?: tag

    fun description(value: Value<*>): String? {
        val key = value.descriptionKey
        val text = value.description.get()?.takeIf { it.isNotBlank() } ?: return null

        if (key != null && text == key) {
            return trOrNull(key)
        }

        return text
    }

    private fun lowerFirst(name: String): String =
        if (name.isEmpty()) name else name[0].lowercaseChar() + name.substring(1)

}
