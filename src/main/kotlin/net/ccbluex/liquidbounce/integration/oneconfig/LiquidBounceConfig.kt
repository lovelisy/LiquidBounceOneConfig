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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer

/**
 * The LiquidBounce card in the OneConfig screen.
 *
 * Every module is listed as a switch, grouped by its LiquidBounce category. The switch is backed by the
 * module's own `enabled` state through [Properties.functional], so toggling it in OneConfig is exactly the
 * same as toggling the module in-game, and modules toggled by binds or commands immediately show up here.
 */
class LiquidBounceConfig : Config(ID, TITLE, Config.Category.QOL) {

    companion object {
        const val ID = "liquidbounce"
        const val TITLE = "LiquidBounce"
    }

    override fun makeTree(): Tree {
        // The tree gets its id from the constructor. Calling `setID` again throws
        // "ID is already set" from `Node.setID`, which crashes the client during startup.
        val root = Tree(ID, TITLE, null, null)

        // Categories in their registered order, so the OneConfig list mirrors the old ClickGUI tabs.
        for (category in ModuleCategories.entries) {
            ModuleManager
                .filter { it.category === category }
                .sortedBy { it.name }
                .forEach { module -> root.put(moduleSwitch(category.tag, module)) }
        }

        // Add-on modules in categories that are not part of the built-in registry.
        ModuleManager
            .filter { module -> ModuleCategories.entries.none { it === module.category } }
            .sortedBy { it.name }
            .forEach { module -> root.put(moduleSwitch(module.category.tag, module)) }

        return root
    }

    private fun moduleSwitch(category: String, module: ClientModule): Property<Boolean> {
        val property = Properties.functional(
            { module.enabled },
            { value: Boolean -> module.enabled = value },
            module.name,
            module.name,
            "",
            Boolean::class.javaObjectType,
        )
        property.addMetadata("visualizer", Visualizer.SwitchVisualizer::class.java)
        property.addMetadata("category", category)
        return property
    }

}
