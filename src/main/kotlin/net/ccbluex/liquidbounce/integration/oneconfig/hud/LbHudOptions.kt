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

/**
 * Option ids of [org.polyfrost.oneconfig.api.hud.v1.Hud] that a native LiquidBounce HUD element does not honour.
 *
 * Every element paints its own card, so the generic surface, border and text decorations of OneConfig are either
 * ignored or, in the case of the background switch, actively harmful: turning it on lays a second dark plate behind a
 * card that already has one. They stay in the tree so OneConfig keeps reading and writing them, but the settings
 * panel hides them through [LbHud.hiddenOptions].
 */
internal object LbHudOptions {

    /** The plate OneConfig would draw behind the element. */
    val BACKGROUND: Set<String> = setOf(
        "showBackground",
        "bgColor",
        "bgChroma",
        "bgChromaSpeed",
        "bgRadius",
        "mergeBackground",
        "mergeDiagonally",
        "mergeAcrossColours",
        "keepBgWhenHidden",
    )

    /** The frame OneConfig would draw around the element. */
    val BORDER: Set<String> = setOf(
        "borderEnabled",
        "borderColor",
        "borderChroma",
        "borderChromaSpeed",
        "borderSize",
    )

    /** Text options the native layout positions and styles on its own. */
    val TEXT_DECORATION: Set<String> = setOf(
        "textAlign",
        "caseType",
        "textUnderline",
        "textChroma",
        "textChromaSpeed",
    )

    /** The drop shadow OneConfig would draw behind every glyph. */
    val SHADOW: Set<String> = setOf(
        "showShadow",
        "fullShadow",
        "shadowColor",
        "shadowChroma",
        "shadowChromaSpeed",
        "shadowOffsetX",
        "shadowOffsetY",
    )

    /** The Poppins faces of the typography controls. */
    val TYPOGRAPHY: Set<String> = setOf(
        "font",
        "textScale",
        "textBold",
        "textItalic",
        "textWeight",
    )

    /** Everything a text element cannot use, because it paints its own surface and its own glyphs. */
    val UNUSED_BY_TEXT: Set<String> = BACKGROUND + BORDER + TEXT_DECORATION + SHADOW

    /** Everything an artwork element cannot use, except for the background and the tint colour of the artwork. */
    val UNUSED_BY_ARTWORK: Set<String> = BORDER + TEXT_DECORATION + SHADOW + TYPOGRAPHY

}
