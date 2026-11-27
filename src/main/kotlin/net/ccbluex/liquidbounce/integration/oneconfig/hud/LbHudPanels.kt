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

import net.minecraft.locale.Language
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Tells OneConfig's HUD style panel which of its cards a LiquidBounce element keeps.
 *
 * The style panel is hard-coded Compose UI: it reads the fields of [Hud] directly instead of walking the option tree,
 * so hiding a property never removed a control from it. The panel still asks for its cards by title, which gives one
 * hook the mod can use: an element declares the cards it cannot honour, and the panel patch drops them.
 *
 * The element being edited is published for the duration of one composition pass, because the card composable itself
 * does not receive it.
 */
object LbHudPanels {

    /** Title key of the card holding the font, the size, the modifiers, the colour and the shadow. */
    const val TEXT: String = "text"

    /** Title key of the card holding the background, the radius, the border and the merging. */
    const val BACKGROUND: String = "background"

    private val element = ThreadLocal<LbHud?>()

    /** Publishes the element the style panel is about to build, called from the panel patch. */
    @JvmStatic
    fun enter(hud: Hud?) {
        if (hud is LbHud) element.set(hud) else element.remove()
    }

    /** Clears the published element once the style panel is done, called from the panel patch. */
    @JvmStatic
    fun exit() {
        element.remove()
    }

    /**
     * Whether the card titled [title] has to be dropped for the element currently being edited.
     *
     * Called for every card the panel builds, including the ones of other mods, which simply never match.
     */
    @JvmStatic
    fun hides(title: String): Boolean {
        val current = element.get() ?: return false
        return current.hiddenPanelSections.any { titleOf(it) == title }
    }

    /**
     * Resolves the title the panel shows for [section].
     *
     * The panel builds the very same string through OneConfig's own translation helper, so comparing against this
     * value keeps working in every language instead of only in English.
     */
    fun titleOf(section: String): String {
        val fallback = when (section) {
            TEXT -> "Text"
            BACKGROUND -> "Background"
            else -> section
        }

        val key = "oneconfig.hud.panel.$section"
        return runCatching {
            val language = Language.getInstance()
            if (language.has(key)) language.getOrDefault(key, fallback) else fallback
        }.getOrDefault(fallback)
    }
}
