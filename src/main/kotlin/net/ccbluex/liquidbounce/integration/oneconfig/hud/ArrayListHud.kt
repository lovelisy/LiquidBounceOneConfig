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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.oneconfig.LbText
import net.ccbluex.liquidbounce.features.module.ModuleManager
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

/**
 * Native module list.
 *
 * Every enabled and visible module is listed with its tag, sorted by the width of its label. Rows fly in from the side
 * and slide into place when the order changes, and the accent bar on the left marks the element as a LiquidBounce HUD.
 */
object ArrayListHud : LbHudBase("arraylist", "Array List", Hud.Category.INFO) {

    @Switch(title = "Show Tags")
    @JvmField
    var showTags: Boolean = true

    @Dropdown(title = "Item Alignment", options = ["Left", "Right"])
    @JvmField
    var itemAlignment: String = "Right"

    @Dropdown(title = "Order", options = ["Ascending", "Descending"])
    @JvmField
    var order: String = "Descending"

    private class AnimEntry(var y: Float, var slideProgress: Float)

    private val animMap: HashMap<String, AnimEntry> = HashMap()

    private data class Row(
        val module: ClientModule,
        val name: String,
        val tag: String?,
        val textWidth: Float,
    )

    private fun rows(): List<Row> {
        val result = ArrayList<Row>()
        for (module in ModuleManager) {
            if (!module.enabled || module.hidden) continue

            val name = displayName(module.name)
            val tag = if (showTags) module.tag else null
            val nameWidth = textWidth(name, ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM)
            val tagWidth = if (!tag.isNullOrEmpty()) {
                textWidth(" $tag", ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM)
            } else {
                0f
            }
            result.add(Row(module, name, tag, nameWidth + tagWidth))
        }

        if (order == "Ascending") {
            result.sortBy { it.textWidth }
        } else {
            result.sortByDescending { it.textWidth }
        }
        return result
    }

    private fun rowHeight(): Float = lineHeight(ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM) + rowVerticalPadding()

    /** Padding follows the HUD text scale, so the rows stay balanced when the font grows. */
    private fun rowVerticalPadding(): Float = ROW_VERTICAL_PADDING * textScale

    private fun horizontalPadding(): Float = HORIZONTAL_PADDING * textScale

    private fun rowInset(): Float = ACCENT_WIDTH + horizontalPadding()

    override fun measure(): HudSize {
        val rows = rows()
        if (rows.isEmpty()) {
            return HudSize(EMPTY_WIDTH * textScale, rowHeight())
        }

        var maxWidth = 0f
        for (row in rows) {
            val width = rowInset() + horizontalPadding() + row.textWidth
            if (width > maxWidth) maxWidth = width
        }
        return HudSize(maxWidth, rows.size * rowHeight())
    }

    /** Sample row shown in the HUD editor, in the in-game language. */
    private fun sampleName(): String = LbText.hudString("liquidbounce.hud.arraylist.sample.name", "Array List")

    override fun paint(canvas: LbHudCanvas) {
        val rows = rows()
        if (rows.isEmpty()) {
            if (canvas.inEditor) {
                drawRow(canvas, sampleName(), null, 0f, 0f, EMPTY_WIDTH)
            }
            return
        }

        val activeNames = rows.mapTo(HashSet(rows.size * 2)) { it.module.name }
        animMap.keys.retainAll(activeNames)

        val rowHeight = rowHeight()
        val lineHeight = lineHeight(ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM)
        var maxRowWidth = 0f
        for (row in rows) {
            val width = rowInset() + horizontalPadding() + row.textWidth
            if (width > maxRowWidth) maxRowWidth = width
        }

        rows.forEachIndexed { index, row ->
            val targetY = index * rowHeight
            val anim = animMap.getOrPut(row.module.name) { AnimEntry(targetY, 0f) }
            anim.y = LbHudPalette.expLerp(anim.y, targetY, 16f, canvas.deltaSeconds)
            anim.slideProgress = LbHudPalette.expLerp(anim.slideProgress, 1f, 15f, canvas.deltaSeconds)

            val rowWidth = rowInset() + horizontalPadding() + row.textWidth
            val baseX = if (itemAlignment == "Left") 0f else maxRowWidth - rowWidth
            val flyOffset = (1f - anim.slideProgress) * if (itemAlignment == "Left") -36f else 36f
            val x = baseX + flyOffset
            val y = anim.y

            drawRow(canvas, row.name, row.tag, x, y, rowWidth, lineHeight)
        }
    }

    private fun drawRow(
        canvas: LbHudCanvas,
        name: String,
        tag: String?,
        x: Float,
        y: Float,
        rowWidth: Float,
        rowLineHeight: Float = lineHeight(ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM),
    ) {
        val rowHeight = rowLineHeight + rowVerticalPadding()

        canvas.fillCssBox(x, y, rowWidth, rowHeight, 4f, 0f, 0f, 4f, LbHudPalette.surface(0.68f))
        canvas.fillCssBox(x, y, ACCENT_WIDTH, rowHeight, 4f, 0f, 0f, 4f, LbHudPalette.accent(1f))

        val textX = x + ACCENT_WIDTH + horizontalPadding()
        val textY = y + (rowHeight - rowLineHeight) * 0.5f
        val nameWidth = drawText(canvas, name, textX, textY, ROW_FONT_SIZE, textColor, LbHudFonts.Weight.MEDIUM)
        if (!tag.isNullOrEmpty()) {
            drawText(
                canvas,
                " $tag",
                textX + nameWidth,
                textY,
                ROW_FONT_SIZE,
                LbHudPalette.ARRAYLIST_TAG_COLOR,
                LbHudFonts.Weight.MEDIUM,
            )
        }
    }

    private const val ROW_FONT_SIZE: Float = 14f
    private const val ROW_VERTICAL_PADDING: Float = 10f
    private const val HORIZONTAL_PADDING: Float = 8f
    private const val ACCENT_WIDTH: Float = 4f
    private const val EMPTY_WIDTH: Float = 110f

}
