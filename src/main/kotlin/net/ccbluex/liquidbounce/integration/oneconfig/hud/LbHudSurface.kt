package net.ccbluex.liquidbounce.integration.oneconfig.hud

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * Background handling shared by every LiquidBounce HUD element.
 *
 * The plate is drawn by the element itself, because the native look is built out of several pieces, such as the split
 * header and body of the key bind list or the per row cards of the array list. The colour, the visibility and the
 * radius all come from the standard background card of the OneConfig style panel, which edits the [Hud] properties
 * directly, so a change in the panel moves every piece at once instead of a separate unit option.
 *
 * @param defaultColor the background a fresh element starts with, matching the native surface alpha of that element.
 */
abstract class LbHudSurface(
    id: String,
    title: String,
    category: Hud.Category,
    defaultColor: Int,
) : LbHudBase(id, title, category) {

    /**
     * Colour of the accents the element paints itself, such as the bar of the array list, the fill of a held key or
     * the badge of the watermark.
     *
     * It is a regular OneConfig colour control, so the chroma cycle and the gradient work here exactly like they do on
     * a panel surface. The text colour is not repeated: it comes from [textColor], which the style panel already edits
     * for every HUD element, and the secondary labels are that colour at a lower alpha.
     */
    @Color(title = "Accent Color", alpha = true, gradient = true)
    @JvmField
    var accentColor: PolyColor = PolyColor(LbHudPalette.DEFAULT_ACCENT)

    init {
        showBackground = (defaultColor ushr 24) != 0
        bgPolyColor = PolyColor(defaultColor)
        bgRadius = SURFACE_RADIUS
    }

    /** The background colour in use, fully transparent while the switch of the style panel is off. */
    protected fun surfaceArgb(): Int = if (showBackground) bgPolyColor.rawArgb else 0

    /** The background colour at [factor] of its transparency, for surfaces made of several layers. */
    protected fun surfaceArgb(factor: Float): Int = LbHudPalette.withAlphaFactor(surfaceArgb(), factor)

    /** The radius the panel assigns to the background, applied to the pieces the element paints. */
    protected fun surfaceRadius(): Float = bgRadius

    /** The accent colour in use. */
    protected fun accentArgb(): Int = accentColor.rawArgb

    /** The accent colour at [factor] opacity. */
    protected fun accentArgb(factor: Float): Int = LbHudPalette.withAlphaFactor(accentArgb(), factor)

    private companion object {
        const val SURFACE_RADIUS: Float = 5f
    }

}
