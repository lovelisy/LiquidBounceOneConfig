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
import net.minecraft.client.gui.Hud as McHud
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import kotlin.math.floor
import kotlin.math.max

/**
 * Native status-effect display.
 *
 * Shows every active potion effect with its 16x16 sprite, the amplifier and a monospace duration. The sprite is read
 * from the active Minecraft resource packs through [LbHudTextures.minecraft], so custom resource packs are honoured.
 * When there is no effect the editor draws a fixed sample row; in game nothing is painted.
 */
object EffectsHud : LbHudSurface("effects", "Effects", Hud.Category.INFO) {

    override val defaultShown: Boolean
        get() = true

    private data class EffectRow(
        val sprite: String,
        val name: String,
        val amp: String,
        val duration: String,
        val width: Float,
    )

    private fun formatDuration(ticks: Int): String {
        if (ticks < 0) return "*:*"
        val totalSeconds = floor(ticks / 20.0).toInt().coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun rows(inEditor: Boolean): List<EffectRow> {
        val effects = mc.player?.activeEffects?.toList().orEmpty()
        if (effects.isEmpty()) {
            return if (inEditor) {
                listOf(EffectRow("minecraft:textures/mob_effect/speed.png", "Speed", "2", "1:30", 156f))
            } else {
                emptyList()
            }
        }

        return effects.map { instance ->
            val effect = instance.effect
            val sprite = McHud.getMobEffectSprite(effect)
            val path = "${sprite.namespace}:textures/${sprite.path}.png"
            val name = effect.value().displayName.string
            val amp = (instance.amplifier + 1).toString()
            val duration = formatDuration(instance.duration)
            val nameWidth = textWidth("$name  ", NAME_SIZE, LbHudFonts.Weight.MEDIUM)
            val ampWidth = textWidth(amp, NAME_SIZE, LbHudFonts.Weight.MEDIUM)
            val durationWidth = textWidth(duration, DURATION_SIZE, LbHudFonts.Weight.REGULAR)
            val width = ICON_X + ICON_SIZE.toFloat() + TEXT_GAP + nameWidth + ampWidth + HORIZONTAL_PADDING + durationWidth
            EffectRow(path, name, amp, duration, width)
        }
    }

    override fun measure(): HudSize {
        val shown = rows(HudManager.isEditing)
        if (shown.isEmpty()) return HudSize(EMPTY_WIDTH, ROW_HEIGHT)

        val boxWidth = shown.maxOf { it.width }
        val boxHeight = VERTICAL_PADDING + shown.size * ROW_HEIGHT + max(0, shown.size - 1) * ROW_GAP
        return HudSize(boxWidth, boxHeight)
    }

    override fun paint(canvas: LbHudCanvas) {
        val shown = rows(canvas.inEditor)
        if (shown.isEmpty()) return

        val boxWidth = shown.maxOf { it.width }
        val boxHeight = VERTICAL_PADDING + shown.size * ROW_HEIGHT + max(0, shown.size - 1) * ROW_GAP
        canvas.fill(0f, 0f, boxWidth, boxHeight, surfaceArgb(), surfaceRadius())

        shown.forEachIndexed { index, row ->
            val rowY = VERTICAL_PADDING / 2f + index * (ROW_HEIGHT + ROW_GAP)

            val bitmap = LbHudTextures.minecraft(row.sprite)
            if (bitmap != null) {
                canvas.roundedTexture(
                    bitmap,
                    ICON_X, rowY,
                    ICON_SIZE.toFloat(), ICON_SIZE.toFloat(), 0f,
                    0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
                    -1, FilterQuality.None,
                )
            }

            val textX = ICON_X + ICON_SIZE.toFloat() + TEXT_GAP
            val nameWidth = drawText(canvas, "${row.name}  ", textX, rowY, NAME_SIZE,
                textColor, LbHudFonts.Weight.MEDIUM)
            drawText(canvas, row.amp, textX + nameWidth, rowY, NAME_SIZE,
                LbHudPalette.ERROR_COLOR, LbHudFonts.Weight.MEDIUM)

            val durationWidth = textWidth(row.duration, DURATION_SIZE, LbHudFonts.Weight.REGULAR)
            drawText(canvas, row.duration, boxWidth - HORIZONTAL_PADDING - durationWidth,
                rowY + 1.5f, DURATION_SIZE, textColor, LbHudFonts.Weight.REGULAR)
        }
    }

    private const val NAME_SIZE: Float = 14f
    private const val DURATION_SIZE: Float = 12f
    private const val ICON_SIZE: Int = 16
    private const val ICON_X: Float = 6f
    private const val TEXT_GAP: Float = 8f
    private const val ROW_HEIGHT: Float = 20f
    private const val ROW_GAP: Float = 4f
    private const val VERTICAL_PADDING: Float = 8f
    private const val HORIZONTAL_PADDING: Float = 6f
    private const val EMPTY_WIDTH: Float = 150f
}
