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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.oneconfig.LbText
import net.ccbluex.liquidbounce.utils.input.InputBind
import org.polyfrost.oneconfig.api.hud.v1.Hud
import kotlin.math.max
import kotlin.math.min

/**
 * Native key bind list.
 *
 * Shows every module that has a key bound, with the key on the right and the module name on the left. Enabled modules
 * use the accent colour, disabled ones stay dimmed, exactly like the card of the native client.
 */
object KeyBindsHud : LbHudSurface("keybinds", "KeyBinds", Hud.Category.INFO) {

    override val defaultShown: Boolean
        get() = true

    private class Row(val name: String, val keyText: String, val enabled: Boolean, val keyWidth: Float)

    private fun rows(): List<Row> = ModuleManager
        .filter { !it.bind.isUnbound }
        .map { module ->
            val name = displayName(module.name)
            val keyText = "[${formatBind(module.bind)}]"
            Row(
                name = name,
                keyText = keyText,
                enabled = module.enabled,
                keyWidth = textWidth(keyText, KEY_FONT_SIZE, LbHudFonts.Weight.MEDIUM),
            )
        }

    private fun headerHeight(): Float = titleLineHeight() + HEADER_PADDING * 2f

    private fun titleLineHeight(): Float = lineHeight(TITLE_SIZE, LbHudFonts.Weight.SEMIBOLD)

    private fun rowHeight(): Float = lineHeight(ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM) + ROW_PADDING * textScale

    private fun bodyHeight(rowCount: Int): Float {
        val count = max(1, rowCount)
        return BODY_PADDING * textScale + count * rowHeight()
    }

    private fun bodyWidth(list: List<Row>): Float {
        var width = MIN_WIDTH
        for (row in list) {
            val rowWidth = horizontalPadding() * 2f + textWidth(row.name, ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM) +
                ROW_GAP + row.keyWidth
            width = max(width, min(MAX_WIDTH, rowWidth))
        }
        return width
    }

    private fun horizontalPadding(): Float = BODY_PADDING * textScale

    override fun measure(): HudSize {
        val rows = rows()
        return HudSize(bodyWidth(rows), headerHeight() + bodyHeight(rows.size))
    }

    override fun paint(canvas: LbHudCanvas) {
        val rows = rows()
        val boxWidth = bodyWidth(rows)
        val header = headerHeight()
        val body = bodyHeight(rows.size)

        canvas.fillCssBox(0f, 0f, boxWidth, header, 5f, 5f, 0f, 0f, surfaceArgb())
        val titleY = (header - titleLineHeight()) * 0.5f
        drawText(
            canvas,
            LbText.hudString("liquidbounce.hud.keybinds.title", "Binds"),
            horizontalPadding(),
            titleY,
            TITLE_SIZE,
            textColor,
            LbHudFonts.Weight.SEMIBOLD,
        )
        canvas.icon(
            KEYBINDS_ICON,
            boxWidth - horizontalPadding() - ICON_SIZE,
            (header - ICON_SIZE) * 0.5f,
            ICON_SIZE,
            ICON_SIZE,
            textColor,
        )

        canvas.fillCssBox(0f, header, boxWidth, body, 0f, 0f, 5f, 5f, surfaceArgb(BODY_ALPHA))

        if (rows.isEmpty()) {
            drawText(
                canvas,
                LbText.hudString("liquidbounce.hud.keybinds.empty", "No key bindings"),
                horizontalPadding(),
                header + BODY_PADDING * textScale,
                EMPTY_SIZE,
                dimTextArgb(),
            )
            return
        }

        val rowHeight = rowHeight()
        rows.forEachIndexed { index, row ->
            val rowY = header + BODY_PADDING * textScale + index * rowHeight
            val nameWidth = max(
                MIN_NAME_WIDTH,
                boxWidth - horizontalPadding() * 2f - row.keyWidth - ROW_GAP,
            )
            val name = ellipsize(row.name, nameWidth, ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM)
            drawText(
                canvas,
                name,
                horizontalPadding(),
                rowY,
                ROW_FONT_SIZE,
                if (row.enabled) textColor else dimTextArgb(),
                if (row.enabled) LbHudFonts.Weight.MEDIUM else LbHudFonts.Weight.REGULAR,
            )
            drawText(
                canvas,
                row.keyText,
                boxWidth - horizontalPadding() - row.keyWidth,
                rowY,
                KEY_FONT_SIZE,
                if (row.enabled) accentArgb() else faintTextArgb(),
                LbHudFonts.Weight.MEDIUM,
            )
        }
    }

    /** Formats a bind the way the native client does, including the platform specific modifier names. */
    private fun formatBind(bind: InputBind): String {
        if (bind.isUnbound) {
            return "None"
        }

        val prefix = if (bind.modifiers.isEmpty()) {
            ""
        } else {
            bind.modifiers.joinToString("+") { it.platformRenderName } + "+"
        }
        return prefix + formatKeyName(bind.boundKey)
    }

    private fun formatKeyName(key: InputConstants.Key): String {
        if (key == InputConstants.UNKNOWN) {
            return "None"
        }

        if (key.type == InputConstants.Type.MOUSE) {
            return when (key.value) {
                InputConstants.MOUSE_BUTTON_LEFT -> "LMB"
                InputConstants.MOUSE_BUTTON_RIGHT -> "RMB"
                InputConstants.MOUSE_BUTTON_MIDDLE -> "MMB"
                else -> "Mouse ${key.value}"
            }
        }

        val shortKey = key.name
            .removePrefix("key.keyboard.")
            .removePrefix("key.mouse.")

        return when (shortKey) {
            "left.shift" -> "LShift"
            "right.shift" -> "RShift"
            "left.control" -> "LCtrl"
            "right.control" -> "RCtrl"
            "left.alt" -> "LAlt"
            "right.alt" -> "RAlt"
            "left.win" -> "Super"
            "right.win" -> "RSuper"
            "caps.lock" -> "CapsLock"
            "grave.accent" -> "`"
            "space" -> "Space"
            "escape" -> "Esc"
            "enter", "keypad.enter" -> "Enter"
            "backspace" -> "Backspace"
            "tab" -> "Tab"
            "delete" -> "Del"
            "insert" -> "Ins"
            "page.up" -> "PgUp"
            "page.down" -> "PgDn"
            "home" -> "Home"
            "end" -> "End"
            "up" -> "Up"
            "down" -> "Down"
            "left" -> "Left"
            "right" -> "Right"
            else -> {
                val localized = runCatching { key.displayName.string }.getOrNull()
                if (!localized.isNullOrBlank() && !localized.startsWith("key.")) {
                    localized
                } else {
                    shortKey.split('.').joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                }
            }
        }
    }

    private const val KEYBINDS_ICON: String = "textures/ui/hud/keybinds/icon-keybinds.png"

    private const val TITLE_SIZE: Float = 14f
    private const val ROW_FONT_SIZE: Float = 14f
    private const val KEY_FONT_SIZE: Float = 12f
    private const val EMPTY_SIZE: Float = 13f

    private const val HEADER_PADDING: Float = 8f
    private const val BODY_PADDING: Float = 6f
    private const val ROW_PADDING: Float = 4f
    private const val ROW_GAP: Float = 12f
    private const val ICON_SIZE: Float = 16f
    private const val MIN_NAME_WIDTH: Float = 30f
    private const val MIN_WIDTH: Float = 150f
    private const val MAX_WIDTH: Float = 210f

    /** The native body sits on a lighter plate than the header, so the same colour is used at this alpha. */
    private const val BODY_ALPHA: Float = 0.74f

}
