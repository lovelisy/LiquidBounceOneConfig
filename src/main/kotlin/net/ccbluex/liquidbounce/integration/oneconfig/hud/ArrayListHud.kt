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
import kotlin.math.max

/**
 * Native module list.
 *
 * Every enabled and visible module is listed with its tag, sorted by the width of its label. Rows fly in from the side
 * and slide into place when the order changes, and the accent bar on the left marks the element as a LiquidBounce HUD.
 *
 * Each row keeps the width of its own label, which is what produces the stepped silhouette of the native client: the
 * longest module reaches furthest out and the short ones stay tucked in behind it. The rows are never padded to a
 * common width, neither in game nor in the editor preview.
 */
object ArrayListHud : LbHudSurface("arraylist", "Array List", Hud.Category.INFO) {

    override val defaultShown: Boolean
        get() = true

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

    private fun accentWidth(): Float = ACCENT_WIDTH * textScale

    private fun rowInset(): Float = accentWidth() + horizontalPadding()

    private fun rowCorner(): Float = ROW_CORNER * textScale

    /** Width of one row, which is the width its own label needs plus the bar and the paddings. */
    private fun rowWidth(row: Row): Float = rowInset() + horizontalPadding() + row.textWidth

    /** Widths of the rows that are actually shown, so the measuring pass and the paint pass agree. */
    private fun shownRows(inEditor: Boolean): List<Row> {
        val all = rows()
        return if (inEditor) all.take(EDITOR_MAX_ROWS) else all
    }

    override fun measure(): HudSize {
        val rows = shownRows(HudManager.isEditing)
        if (rows.isEmpty()) {
            return HudSize(EMPTY_WIDTH * textScale, rowHeight())
        }

        var maxWidth = 0f
        for (row in rows) {
            maxWidth = max(maxWidth, rowWidth(row))
        }

        // The rows keep their own widths; only the reported extent gets a floor, so a short list still yields a card
        // wide enough for OneConfig to place it on a row of its own instead of shrinking it next to a neighbour.
        val width = if (HudManager.isEditing) {
            max(maxWidth, EDITOR_MIN_WIDTH * textScale)
        } else {
            maxWidth
        }

        return HudSize(width, rows.size * rowHeight())
    }

    /** Sample row shown in the HUD editor, in the in-game language. */
    private fun sampleName(): String = LbText.hudString("liquidbounce.hud.arraylist.sample.name", "Array List")

    override fun paint(canvas: LbHudCanvas) {
        val shown = shownRows(canvas.inEditor)
        if (shown.isEmpty()) {
            if (canvas.inEditor) {
                drawRow(canvas, sampleName(), null, 0f, 0f, EMPTY_WIDTH * textScale)
            }
            return
        }

        val activeNames = shown.mapTo(HashSet(shown.size * 2)) { it.module.name }
        animMap.keys.retainAll(activeNames)

        val rowHeight = rowHeight()
        val lineHeight = lineHeight(ROW_FONT_SIZE, LbHudFonts.Weight.MEDIUM)

        // Rows align to the extent the element reports, which in the editor has a floor, so the card keeps a workable size
        // while every row still ends on the same right edge. The left edge is what steps, which is the native silhouette.
        var maxRowWidth = 0f
        for (row in shown) {
            maxRowWidth = max(maxRowWidth, rowWidth(row))
        }
        val bounds = max(canvas.width, maxRowWidth)

        shown.forEachIndexed { index, row ->
            val targetY = index * rowHeight
            val anim = animMap.getOrPut(row.module.name) { AnimEntry(targetY, 0f) }
            val animated = !canvas.inEditor
            if (animated) {
                anim.y = LbHudPalette.expLerp(anim.y, targetY, 16f, canvas.deltaSeconds)
                anim.slideProgress = LbHudPalette.expLerp(anim.slideProgress, 1f, 15f, canvas.deltaSeconds)
            } else {
                // The editor preview shows the settled layout, so it matches the in game result exactly.
                anim.y = targetY
                anim.slideProgress = 1f
            }

            val width = rowWidth(row)
            val baseX = if (itemAlignment == "Left") 0f else max(0f, bounds - width)
            val flyOffset = (1f - anim.slideProgress) * if (itemAlignment == "Left") -FLY_DISTANCE else FLY_DISTANCE
            val x = baseX + flyOffset

            drawRow(canvas, row.name, row.tag, x, anim.y, width, lineHeight)
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
        val corner = rowCorner()

        canvas.fillCssBox(x, y, rowWidth, rowHeight, corner, 0f, 0f, corner, canvas.surfaceArgb())
        canvas.fillCssBox(x, y, accentWidth(), rowHeight, corner, 0f, 0f, corner, accentArgb())

        val textX = x + accentWidth() + horizontalPadding()
        val textY = y + (rowHeight - rowLineHeight) * 0.5f
        val nameWidth = drawText(canvas, name, textX, textY, ROW_FONT_SIZE, canvas.textColor, LbHudFonts.Weight.MEDIUM)
        if (!tag.isNullOrEmpty()) {
            drawText(
                canvas,
                " $tag",
                textX + nameWidth,
                textY,
                ROW_FONT_SIZE,
                canvas.faintTextArgb(),
                LbHudFonts.Weight.MEDIUM,
            )
        }
    }

    private const val ROW_FONT_SIZE: Float = 14f
    private const val ROW_VERTICAL_PADDING: Float = 10f
    private const val HORIZONTAL_PADDING: Float = 8f
    private const val ACCENT_WIDTH: Float = 4f
    private const val ROW_CORNER: Float = 4f
    private const val EMPTY_WIDTH: Float = 110f
    private const val FLY_DISTANCE: Float = 18f

    /** Rows the editor preview keeps before it stops growing, so the card never gets scaled down by OneConfig. */
    private const val EDITOR_MAX_ROWS: Int = 6

    /** Narrowest preview extent, so the library card stays wide enough to sit on a row of its own. */
    private const val EDITOR_MIN_WIDTH: Float = 170f

}
