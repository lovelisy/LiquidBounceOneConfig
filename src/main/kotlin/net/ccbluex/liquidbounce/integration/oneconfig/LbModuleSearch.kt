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

/**
 * Narrows the searchable part of the LiquidBounce tree down to the modules a query names.
 *
 * OneConfig fuzzily matches a query against every field of a document, including the category a module belongs to and
 * the settings inside it, and a match anywhere inside a module hands the whole module back. With a distance of two a
 * short query could match nearly every name, so a search read as the entire tab.
 *
 * Only the module names are matched here, and only by substring: a query has to appear in the name of the module it is
 * looking for. That keeps the result list to the modules themselves and never surfaces a setting on its own.
 */
internal object LbModuleSearch {

    /** Separators that are dropped before comparing a query with a module name. */
    private val SEPARATORS = charArrayOf('.', '_', '-', '/')

    /**
     * Keys of the modules that stay searchable for [query], or `null` when the page is not searching at all.
     *
     * An empty set is meaningful here: the query named no module, so nothing should be left to search and the result
     * list has to come back empty instead of falling back to the whole tree.
     */
    fun matchingModules(query: String): Set<String>? {
        val normalizedQuery = compact(query)

        if (normalizedQuery.isEmpty()) {
            return null
        }

        return LbConfigTree.modules
            .filterValues { names -> names.any { compact(it).contains(normalizedQuery) } }
            .keys
            .toSet()
    }

    /** Lower case form of [text] without separators, so `Kill Aura` and `kill_aura` are the same name. */
    private fun compact(text: String): String = buildString(text.length) {
        for (character in text) {
            if (!character.isWhitespace() && character !in SEPARATORS) {
                append(character.lowercaseChar())
            }
        }
    }

}
